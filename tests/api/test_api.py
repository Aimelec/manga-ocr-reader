"""HTTP layer with fake use cases: routing, request parsing, streaming and errors."""
import io
from types import SimpleNamespace

import pytest
from fastapi.testclient import TestClient
from PIL import Image

from backend.main import API_PREFIX as V1
from backend.main import create_app


class FakeReader:
    def __init__(self):
        self.sizes = []

    def read(self, image):
        self.sizes.append(image.size)
        return {"text": "本", "hiragana": "ほん", "tokens": [], "grammar": []}


class FakeExplainer:
    def __init__(self, fail=False):
        self.fail = fail
        self.calls = []

    def explain(self, line, context=()):
        self.calls.append((line, list(context)))
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
                                    settings=SimpleNamespace(apk=apk))
        return TestClient(create_app(container)), container
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


def test_explain_error_is_appended_to_the_stream(make_client):
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
