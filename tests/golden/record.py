"""Record the golden outputs: what the analyzer returns today for every input in inputs.json.

    venv\\Scripts\\python.exe tests\\golden\\record.py            # text inputs only (fast)
    venv\\Scripts\\python.exe tests\\golden\\record.py --images   # also run OCR on the test images

Run it only to accept a deliberate change in behaviour; the golden tests compare against these files.
"""
import json
import sys
from pathlib import Path

HERE = Path(__file__).parent
ROOT = HERE.parents[1]
sys.path.insert(0, str(ROOT))

EXPECTED = HERE / "expected"


def load_inputs():
    return json.loads((HERE / "inputs.json").read_text(encoding="utf-8"))


def dump(path, data):
    path.write_text(json.dumps(data, ensure_ascii=False, indent=1, sort_keys=True) + "\n", encoding="utf-8")


def main():
    sys.stdout.reconfigure(encoding="utf-8")
    from backend.container import Container

    container = Container()

    EXPECTED.mkdir(exist_ok=True)
    inputs = load_inputs()
    for item in inputs["texts"]:
        dump(EXPECTED / f"text-{item['id']}.json", container.analyzer.analyze(item["text"]))
        print(f"recorded text-{item['id']}")
    if "--images" in sys.argv:
        from PIL import Image, ImageOps

        for item in inputs["images"]:
            img = ImageOps.exif_transpose(Image.open(ROOT / item["path"]))
            dump(EXPECTED / f"image-{item['id']}.json", container.bubble_reader.read(img))
            print(f"recorded image-{item['id']}")


if __name__ == "__main__":
    main()
