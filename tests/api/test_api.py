"""HTTP layer with fake use cases: routing, request parsing, streaming and errors."""
import io
from types import SimpleNamespace

import pytest
from fastapi.testclient import TestClient
from PIL import Image

from backend.api.deps import PAIRING_HEADER
from backend.main import API_PREFIX as V1
from backend.main import create_app

CODE = "ABCD2345"


class FakeReader:
    def __init__(self):
        self.sizes = []

    def read(self, image):
        self.sizes.append(image.size)
        return {"text": "本", "hiragana": "ほん", "tokens": [], "grammar": []}


class FakeExplainer:
    def __init__(self, fail=False, fail_to_start=False):
        self.fail = fail
        self.fail_to_start = fail_to_start
        self.calls = []

    def explain(self, line, context=()):
        self.calls.append((line, list(context)))
        if self.fail_to_start:
            raise FileNotFoundError("Model not found: gemma.gguf")
        yield "TRANSLATION: "
        if self.fail:
            raise RuntimeError("model crashed")
        yield "a book"


@pytest.fixture
def make_client(tmp_path):
    def make(explainer=None):
        apk = tmp_path / "MangaReader.apk"
        apk.write_bytes(b"APK")
        container = SimpleNamespace(bubble_reader=FakeReader(), line_explainer=explainer or FakeExplainer(),
                                    settings=SimpleNamespace(apk=apk), pairing_code=CODE)
        return TestClient(create_app(container), headers={PAIRING_HEADER: CODE}), container
    return make


def png(size=(30, 20)):
    buf = io.BytesIO()
    Image.new("RGB", size, "white").save(buf, "PNG")
    return buf.getvalue()


def test_health(make_client):
    client, _ = make_client()
    r = client.get("/")
    assert r.status_code == 200 and "running" in r.text


def test_read_decodes_the_upload_and_returns_the_analysis(make_client):
    client, c = make_client()
    r = client.post(f"{V1}/read", files={"image": ("bubble.png", png((30, 20)), "image/png")})
    assert r.status_code == 200 and r.json()["text"] == "本"
    assert c.bubble_reader.sizes == [(30, 20)]


def test_read_requires_an_image(make_client):
    client, _ = make_client()
    assert client.post(f"{V1}/read").status_code == 422


def test_explain_streams_and_keeps_last_three_bubbles(make_client):
    client, c = make_client()
    r = client.post(f"{V1}/explain", json={"line": "本だ", "context": ["1", "2", "3", "4"]})
    assert r.status_code == 200 and r.text == "TRANSLATION: a book"
    assert r.headers["content-type"].startswith("text/plain")
    assert c.line_explainer.calls == [("本だ", ["2", "3", "4"])]


def test_explain_context_is_optional(make_client):
    client, c = make_client()
    client.post(f"{V1}/explain", json={"line": "本だ"})
    assert c.line_explainer.calls == [("本だ", [])]


def test_explain_model_that_cannot_start_is_a_503(make_client):
    client, _ = make_client(FakeExplainer(fail_to_start=True))
    r = client.post(f"{V1}/explain", json={"line": "本だ"})
    assert r.status_code == 503 and "Model not found" in r.json()["detail"]


def test_explain_error_midway_is_appended_to_the_stream(make_client):
    client, _ = make_client(FakeExplainer(fail=True))
    r = client.post(f"{V1}/explain", json={"line": "本だ"})
    assert r.text == "TRANSLATION: \n\nERROR: model crashed"


def test_apk_download(make_client):
    client, _ = make_client()
    r = client.get("/app.apk")
    assert r.content == b"APK" and r.headers["content-type"] == "application/vnd.android.package-archive"


@pytest.mark.parametrize("path", ["/read", "/explain"])
def test_old_unversioned_paths_still_work_for_old_app_builds(make_client, path):
    client, _ = make_client()
    if path == "/read":
        r = client.post(path, files={"image": ("bubble.png", png(), "image/png")})
    else:
        r = client.post(path, json={"line": "本だ"})
    assert r.status_code == 200


def test_openapi_documents_only_the_versioned_paths(make_client):
    client, _ = make_client()
    paths = set(client.get("/openapi.json").json()["paths"])
    assert {f"{V1}/read", f"{V1}/explain"} <= paths
    assert "/read" not in paths and "/explain" not in paths


def test_read_rejects_files_that_are_not_images(make_client):
    client, c = make_client()
    r = client.post(f"{V1}/read", files={"image": ("notes.txt", b"hello", "text/plain")})
    assert r.status_code == 400 and c.bubble_reader.sizes == []


def test_read_rejects_oversized_uploads(make_client, monkeypatch):
    from backend.api import read
    monkeypatch.setattr(read, "MAX_UPLOAD_BYTES", 100)
    client, c = make_client()
    r = client.post(f"{V1}/read", files={"image": ("big.png", png((300, 300)), "image/png")})
    assert r.status_code == 413 and c.bubble_reader.sizes == []


def test_read_rejects_huge_pixel_counts(make_client, monkeypatch):
    from backend.api import read
    monkeypatch.setattr(read, "MAX_PIXELS", 500)
    client, _ = make_client()
    assert client.post(f"{V1}/read", files={"image": ("b.png", png((30, 20)), "image/png")}).status_code == 413


@pytest.mark.parametrize("body", [{"line": ""}, {"line": "あ" * 501}, {"line": "本", "context": ["x"] * 21},
                                  {"line": "本", "context": ["あ" * 501]}, {}])
def test_explain_validates_the_request(make_client, body):
    client, c = make_client()
    assert client.post(f"{V1}/explain", json=body).status_code == 422
    assert c.line_explainer.calls == []


@pytest.mark.parametrize("headers", [{}, {PAIRING_HEADER: "WRONG123"}])
def test_versioned_api_requires_the_pairing_code(make_client, headers):
    client, c = make_client()
    client.headers.pop(PAIRING_HEADER)
    r = client.post(f"{V1}/explain", json={"line": "本だ"}, headers=headers)
    assert r.status_code == 401 and c.line_explainer.calls == []
    assert client.post(f"{V1}/read", files={"image": ("b.png", png(), "image/png")}, headers=headers).status_code == 401


def test_pairing_code_is_case_insensitive(make_client):
    client, _ = make_client()
    r = client.post(f"{V1}/explain", json={"line": "本だ"}, headers={PAIRING_HEADER: CODE.lower()})
    assert r.status_code == 200


def test_health_and_apk_need_no_code(make_client):
    client, _ = make_client()
    client.headers.pop(PAIRING_HEADER)
    assert client.get("/").status_code == 200 and client.get("/app.apk").status_code == 200
