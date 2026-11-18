"""Compare local models on the test lines.

    venv\\Scripts\\python.exe llm\\test_models.py

For each model in MODELS: start llama-server on the RX 9070 XT, ask every line in test_lines.json,
record answer + speed, stop the server. Results: llm\\results\\<model>.json and llm\\results\\report.md
"""
import json
import subprocess
import sys
import time
import urllib.request
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from backend.domain.prompts import SYSTEM, user_message  # noqa: E402

HERE = Path(__file__).parent
ROOT = HERE.parent
SERVER = ROOT / "tools" / "llama.cpp" / "llama-server.exe"
PORT = 8090
DEVICE = sys.argv[1] if len(sys.argv) > 1 else "Vulkan0"   # the RX 9070 XT in llama.cpp's numbering

MODELS = {
    "gemma-4-12b-Q5_K_M": HERE / "models" / "gemma-4-12b" / "gemma-4-12b-it-Q5_K_M.gguf",
    "gemma-4-12b-Q6_K": HERE / "models" / "gemma-4-12b" / "gemma-4-12b-it-Q6_K.gguf",
}


def post(path, body, timeout=300):
    req = urllib.request.Request(f"http://127.0.0.1:{PORT}{path}", data=json.dumps(body).encode(),
                                 headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=timeout) as r:
        return json.loads(r.read())


def wait_ready(proc, timeout=300):
    start = time.time()
    while time.time() - start < timeout:
        if proc.poll() is not None:
            raise RuntimeError("llama-server exited")
        try:
            with urllib.request.urlopen(f"http://127.0.0.1:{PORT}/health", timeout=2) as r:
                if r.status == 200:
                    return time.time() - start
        except Exception:
            pass
        time.sleep(1)
    raise TimeoutError("llama-server did not start")


def run_model(name, path, lines):
    log = open(HERE / "results" / f"{name}.server.log", "w", encoding="utf-8")
    proc = subprocess.Popen([str(SERVER), "-m", str(path), "--device", DEVICE, "-ngl", "99", "-c", "8192",
                             "--port", str(PORT), "--host", "127.0.0.1"], stdout=log, stderr=subprocess.STDOUT)
    out = {"model": name, "file": path.name, "answers": []}
    try:
        out["load_seconds"] = round(wait_ready(proc), 1)
        for t in lines:
            body = {
                "messages": [{"role": "system", "content": SYSTEM},
                             {"role": "user", "content": user_message(t["line"], t["context"])}],
                "temperature": 0.3,
                "max_tokens": 700,
                "chat_template_kwargs": {"enable_thinking": False},
            }
            t0 = time.time()
            r = post("/v1/chat/completions", body)
            secs = time.time() - t0
            timings = r.get("timings", {})
            out["answers"].append({
                "id": t["id"], "line": t["line"], "seconds": round(secs, 2),
                "tokens_per_second": round(timings.get("predicted_per_second", 0), 1),
                "answer": r["choices"][0]["message"]["content"],
            })
            print(f"  {t['id']:<12} {secs:5.1f}s  {timings.get('predicted_per_second', 0):5.1f} tok/s", flush=True)
    finally:
        proc.terminate()
        proc.wait(timeout=30)
        log.close()
    return out


def main():
    sys.stdout.reconfigure(encoding="utf-8")
    (HERE / "results").mkdir(exist_ok=True)
    lines = json.loads((HERE / "test_lines.json").read_text(encoding="utf-8"))
    results = []
    for name, path in MODELS.items():
        if not path.exists():
            print(f"skip {name}: {path} missing")
            continue
        print(f"== {name}")
        r = run_model(name, path, lines)
        (HERE / "results" / f"{name}.json").write_text(json.dumps(r, ensure_ascii=False, indent=1), encoding="utf-8")
        results.append(r)

    md = ["# Model comparison", ""]
    for r in results:
        avg = sum(a["seconds"] for a in r["answers"]) / max(1, len(r["answers"]))
        tps = sum(a["tokens_per_second"] for a in r["answers"]) / max(1, len(r["answers"]))
        md.append(f"- **{r['model']}** ({r['file']}): load {r.get('load_seconds')} s, "
                  f"avg {avg:.1f} s per line, {tps:.0f} tokens/s")
    for t in lines:
        md += ["", f"## {t['id']} ({t['style']})", "", f"> {t['line']}", ""]
        for r in results:
            a = next(x for x in r["answers"] if x["id"] == t["id"])
            md += [f"### {r['model']} ({a['seconds']} s)", "", a["answer"], ""]
    out_name = sys.argv[2] if len(sys.argv) > 2 else "report.md"
    (HERE / "results" / out_name).write_text("\n".join(md), encoding="utf-8")
    print(f"report: llm/results/{out_name}")


if __name__ == "__main__":
    main()
