"""Does giving the AI our word analysis (readings, dictionary forms, meanings) improve its answers?

    venv\\Scripts\\python.exe llm\\test_context.py

Variants, all with Gemma (llm\\config.json model):
  A  text only (what the app does now)
  B  text + word list: reading, word type, dictionary form
  C  B + first dictionary meaning of each word
Report: llm\\results\\report_context.md
"""
import json
import subprocess
import sys
import time
import urllib.request
from pathlib import Path

HERE = Path(__file__).parent
ROOT = HERE.parent
sys.path.insert(0, str(ROOT))

from llm.prompt import SYSTEM, user_message  # noqa: E402
from reader_core import analyze  # noqa: E402

PORT = 8091
cfg = json.loads((HERE / "config.json").read_text(encoding="utf-8"))

SYSTEM_WITH_ANALYSIS = SYSTEM + """
- You may also get a word analysis from a dictionary tool. Use it to see word boundaries and dictionary forms,
  but it can be wrong (especially readings of words like 明日, 今日, 一日 and names). If a reading in the
  analysis does not fit this context, say so in a note, giving the right reading in kana (the only time you write a reading)."""

EXTRA_LINES = [
    {"id": "kyou", "style": "reading: 今日 = きょう (not こんにち)", "context": [], "line": "今日は誰も来ないのか。"},
    {"id": "ichinichi", "style": "reading: 一日中 = いちにちじゅう (analysis splits it badly)", "context": [],
     "line": "一日中待っていたんだぞ。"},
    {"id": "nannin", "style": "reading: 何人 = なんにん", "context": [], "line": "何人いるんだ？"},
]


def analysis_block(line, meanings):
    rows = []
    for t in analyze(line)["tokens"]:
        if t["pos"] in ("補助記号", "記号", "空白"):
            continue
        row = f"- {t['surface']}"
        if t["has_kanji"]:
            row += f" [{t['reading']}]"
        row += f" ({t['pos_en']}"
        if t["dictionary_form"] and t["dictionary_form"] != t["surface"]:
            row += f", dictionary form {t['dictionary_form']}"
        row += ")"
        if meanings:
            gloss = t["function"] or (t["entries"][0]["senses"][0]["gloss"] if t["entries"] else "")
            if gloss:
                row += f": {gloss[:60]}"
        rows.append(row)
    return "Word analysis (from a dictionary tool, may contain mistakes):\n" + "\n".join(rows)


def ask(system, user):
    body = {"messages": [{"role": "system", "content": system}, {"role": "user", "content": user}],
            "temperature": 0.3, "max_tokens": 700, "chat_template_kwargs": {"enable_thinking": False}}
    req = urllib.request.Request(f"http://127.0.0.1:{PORT}/v1/chat/completions", data=json.dumps(body).encode(),
                                 headers={"Content-Type": "application/json"})
    t0 = time.time()
    with urllib.request.urlopen(req, timeout=300) as r:
        out = json.loads(r.read())
    return out["choices"][0]["message"]["content"], time.time() - t0


def main():
    sys.stdout.reconfigure(encoding="utf-8")
    lines = json.loads((HERE / "test_lines.json").read_text(encoding="utf-8")) + EXTRA_LINES
    log = open(HERE / "results" / "context.server.log", "w", encoding="utf-8")
    proc = subprocess.Popen([str(ROOT / "tools" / "llama.cpp" / "llama-server.exe"), "-m", str(ROOT / cfg["model"]),
                             "--device", cfg.get("device", "Vulkan0"), "-ngl", "99", "-c", "8192",
                             "--host", "127.0.0.1", "--port", str(PORT)], stdout=log, stderr=subprocess.STDOUT)
    md = ["# Does the word analysis help?", "",
          "A = text only · B = + readings / dictionary forms · C = B + first dictionary meanings", ""]
    try:
        for _ in range(300):
            try:
                urllib.request.urlopen(f"http://127.0.0.1:{PORT}/health", timeout=2)
                break
            except Exception:
                time.sleep(1)
        for t in lines:
            md += [f"## {t['id']} ({t['style']})", "", f"> {t['line']}", "",
                   "Analysis given in B:", "", "```", analysis_block(t["line"], False), "```", ""]
            for variant in "ABC":
                user = user_message(t["line"], t["context"])
                system = SYSTEM
                if variant != "A":
                    user += "\n\n" + analysis_block(t["line"], variant == "C")
                    system = SYSTEM_WITH_ANALYSIS
                answer, secs = ask(system, user)
                print(f"  {t['id']:<11} {variant} {secs:4.1f}s", flush=True)
                md += [f"### {variant} ({secs:.1f} s)", "", answer, ""]
    finally:
        proc.terminate()
        proc.wait(timeout=30)
        log.close()
    (HERE / "results" / "report_context.md").write_text("\n".join(md), encoding="utf-8")
    print("report: llm/results/report_context.md")


if __name__ == "__main__":
    main()
