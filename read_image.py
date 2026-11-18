"""Test the reader on image files from the PC: venv\\Scripts\\python.exe read_image.py photo.jpg [more.jpg ...]"""
import sys

from PIL import Image, ImageOps

from backend.container import Container

sys.stdout.reconfigure(encoding="utf-8")
reader = Container().bubble_reader
for path in sys.argv[1:]:
    result = reader.read(ImageOps.exif_transpose(Image.open(path)))
    print(f"== {path}")
    print("text:    ", result["text"])
    print("hiragana:", result["hiragana"])
    for t in result["tokens"]:
        if t["has_kanji"]:
            print(f"   {t['surface']} = {t['reading']}  ({t['pos']}, {t['dictionary_form']})")
