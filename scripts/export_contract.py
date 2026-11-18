"""Write the API contract files the Android app is checked against:

    contract/openapi.json               the full OpenAPI description of /api/v1
    contract/read_response.example.json a real /read response (the golden 'vertical' image)

    venv\\Scripts\\python.exe scripts\\export_contract.py

tests/api/test_contract_files.py fails when these files are out of date.
"""
import json
import shutil
import sys
from pathlib import Path
from types import SimpleNamespace

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

CONTRACT = ROOT / "contract"
EXAMPLE_SOURCE = ROOT / "tests" / "golden" / "expected" / "image-vertical.json"


def openapi_text():
    from backend.main import create_app
    spec = create_app(SimpleNamespace()).openapi()    # the schema does not need real use cases
    return json.dumps(spec, ensure_ascii=False, indent=2) + "\n"


def example_text():
    return json.dumps(json.loads(EXAMPLE_SOURCE.read_text(encoding="utf-8")), ensure_ascii=False, indent=2) + "\n"


def main():
    CONTRACT.mkdir(exist_ok=True)
    (CONTRACT / "openapi.json").write_text(openapi_text(), encoding="utf-8", newline="\n")
    (CONTRACT / "read_response.example.json").write_text(example_text(), encoding="utf-8", newline="\n")
    print("contract/ updated")


if __name__ == "__main__":
    main()
