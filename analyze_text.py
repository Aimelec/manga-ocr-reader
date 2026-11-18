"""Analyze typed text without OCR: venv\\Scripts\\python.exe analyze_text.py "明日の朝、駅の前で待っている。" """
import sys

from backend.container import Container

sys.stdout.reconfigure(encoding="utf-8")
analyzer = Container().analyzer
for text in sys.argv[1:]:
    r = analyzer.analyze(text)
    print(f"\n== {r['text']}\n   {r['hiragana']}")
    for t in r["tokens"]:
        label = t["pos_en"] + (f", {t['form_en']}" if t["form_en"] else "")
        line = f"  {t['surface']} [{t['reading']}] ({label})"
        if t["function"]:
            line += f"  -> {t['function']}"
        elif t["entries"]:
            line += f"  -> {t['entries'][0]['senses'][0]['gloss']}"
        elif t["parts"]:
            line += "  -> " + " + ".join(f"{p['surface']}: {p['entries'][0]['senses'][0]['gloss']}" for p in t["parts"])
        print(line)
    for g in r["grammar"]:
        print(f"  * {g['name']} ({g['match']}): {g['explanation']}")
