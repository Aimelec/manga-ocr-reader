"""Golden tests: the analyzer must keep returning exactly what tests/golden/expected/ recorded.

They pin today's behaviour so the architecture refactor provably changes nothing.
Needs the real Sudachi dictionary and dict/jamdict.db (marked 'golden'); image tests also load the OCR model ('slow').
"""
import json
from pathlib import Path

import pytest

from tests.golden.record import EXPECTED, ROOT, load_inputs

INPUTS = load_inputs()

pytestmark = pytest.mark.golden


def expected(name):
    return json.loads((EXPECTED / f"{name}.json").read_text(encoding="utf-8"))


def normalize(data):
    # Same serialization as record.py (tuples become lists, key order ignored).
    return json.loads(json.dumps(data, ensure_ascii=False, sort_keys=True))


@pytest.mark.parametrize("item", INPUTS["texts"], ids=[t["id"] for t in INPUTS["texts"]])
def test_text_analysis_matches_golden(item):
    from reader_core import analyze
    assert normalize(analyze(item["text"])) == expected(f"text-{item['id']}")


@pytest.mark.slow
@pytest.mark.parametrize("item", INPUTS["images"], ids=[i["id"] for i in INPUTS["images"]])
def test_image_read_matches_golden(item):
    from PIL import Image, ImageOps

    from reader_core import read_image
    path = Path(ROOT / item["path"])
    img = ImageOps.exif_transpose(Image.open(path))
    assert normalize(read_image(img)) == expected(f"image-{item['id']}")
