"""The response schema must describe exactly what the analyzer returns (every golden output)."""
import json

import pytest

from backend.api.schemas import ReadResponse
from tests.golden.record import EXPECTED

GOLDEN = sorted(EXPECTED.glob("*.json"))


@pytest.mark.parametrize("path", GOLDEN, ids=[p.stem for p in GOLDEN])
def test_golden_output_matches_the_schema_exactly(path):
    data = json.loads(path.read_text(encoding="utf-8"))
    model = ReadResponse.model_validate(data)
    assert json.loads(model.model_dump_json()) == data      # nothing dropped, renamed or retyped
