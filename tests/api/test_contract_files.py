"""The committed contract files must match the code (run scripts/export_contract.py after API changes)."""
import json

from backend.api.schemas import ReadResponse
from scripts.export_contract import CONTRACT, example_text, openapi_text


def test_openapi_file_is_up_to_date():
    assert (CONTRACT / "openapi.json").read_text(encoding="utf-8") == openapi_text(), \
        "API changed: run scripts/export_contract.py and commit contract/"


def test_example_file_is_up_to_date_and_valid():
    text = (CONTRACT / "read_response.example.json").read_text(encoding="utf-8")
    assert text == example_text()
    ReadResponse.model_validate(json.loads(text))


def test_contract_documents_the_pairing_header_and_errors():
    spec = json.loads((CONTRACT / "openapi.json").read_text(encoding="utf-8"))
    read = spec["paths"]["/api/v1/read"]["post"]
    assert any(p["name"] == "X-Pairing-Code" and p["in"] == "header" for p in read["parameters"])
    assert {"200", "400", "401", "413"} <= set(read["responses"])
    assert {"200", "401", "503"} <= set(spec["paths"]["/api/v1/explain"]["post"]["responses"])
