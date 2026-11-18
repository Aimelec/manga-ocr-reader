"""Adapter logic that can run without models or a GPU."""
from pathlib import Path

import pytest
from PIL import Image

from backend.adapters.llama_server import LlamaServer
from backend.adapters.ocr import MangaOcrEngine, clean_photo
from backend.adapters.openai_chat import parse_sse
from backend.adapters.pairing import ALPHABET, ENV_VAR, load_or_create_code, new_code
from backend.adapters.tls import ensure_cert


class TestParseSse:
    def test_yields_content_pieces_until_done(self):
        lines = [b'data: {"choices":[{"delta":{"role":"assistant"}}]}\n',
                 b'data: {"choices":[{"delta":{"content":"Hel"}}]}\n', b"\n",
                 b'data: {"choices":[{"delta":{"content":"lo"}}]}\n', b"data: [DONE]\n",
                 b'data: {"choices":[{"delta":{"content":"ignored"}}]}\n']
        assert list(parse_sse(lines)) == ["Hel", "lo"]

    def test_ignores_comments_and_accepts_str(self):
        assert list(parse_sse([": ping", 'data: {"choices":[{"delta":{"content":"x"}}]}'])) == ["x"]


class TestCleanPhoto:
    def test_small_crops_are_enlarged_and_grayscale(self):
        out = clean_photo(Image.new("RGB", (100, 50), "white"))
        assert out.size == (400, 200) and out.mode == "L"

    def test_large_crops_keep_their_size(self):
        assert clean_photo(Image.new("RGB", (800, 600), "white")).size == (800, 600)


def test_ocr_engine_cleans_the_photo_before_the_model():
    engine = MangaOcrEngine(preprocess=lambda img: f"clean({img})")
    engine._model = lambda img: f"text of {img}"     # stand-in for the real model
    assert engine.read("photo") == "text of clean(photo)"


class FakeProc:
    def poll(self):
        return None


class TestLlamaServerIdle:
    def make(self, now):
        s = LlamaServer("llama-server", Path("m.gguf"), "Vulkan0", 8090, 8192, idle_seconds=300, log_file="x.log",
                        clock=lambda: now[0])
        s.ensure_running = lambda: setattr(s, "last_used", now[0])   # no real process
        s.proc = FakeProc()
        return s

    def test_idle_after_timeout_without_use(self):
        now = [1000.0]
        s = self.make(now)
        with s.session():
            pass
        now[0] += 301
        assert s.is_idle()

    def test_not_idle_while_a_request_is_streaming(self):
        now = [1000.0]
        s = self.make(now)
        with s.session():
            now[0] += 10_000
            assert not s.is_idle()

    def test_counter_is_exact_under_concurrent_requests(self):
        import threading
        s = self.make([0.0])

        def many():
            for _ in range(2000):
                with s.session():
                    pass
        threads = [threading.Thread(target=many) for _ in range(8)]
        for t in threads:
            t.start()
        for t in threads:
            t.join()
        assert s.active == 0

    def test_session_yields_the_url(self):
        s = self.make([0.0])
        with s.session() as url:
            assert url == "http://127.0.0.1:8090"

    def test_missing_model_is_reported(self, tmp_path):
        s = LlamaServer("llama-server", tmp_path / "missing.gguf", "Vulkan0", 8090, 8192, 300, tmp_path / "x.log")
        with pytest.raises(FileNotFoundError):
            s.ensure_running()


class TestEnsureCert:
    def test_creates_once_per_address(self, tmp_path):
        calls = []

        def fake_openssl(cmd, **kw):
            calls.append(cmd)
            (tmp_path / "cert.pem").write_text("cert")

        key, cert = ensure_cert(tmp_path, "192.168.1.5", openssl="openssl", run=fake_openssl)
        assert Path(cert).name == "cert.pem" and Path(key).name == "key.pem"
        assert "subjectAltName=IP:192.168.1.5,IP:127.0.0.1" in calls[0]
        ensure_cert(tmp_path, "192.168.1.5", openssl="openssl", run=fake_openssl)
        assert len(calls) == 1
        ensure_cert(tmp_path, "192.168.1.9", openssl="openssl", run=fake_openssl)
        assert len(calls) == 2


class TestPairingCode:
    def test_new_code_uses_unambiguous_characters(self):
        code = new_code()
        assert len(code) == 8 and set(code) <= set(ALPHABET) and not set(code) & set("0O1IL")

    def test_created_once_then_reused(self, tmp_path):
        f = tmp_path / "cert" / "pairing_code.txt"
        first = load_or_create_code(f, env={})
        assert f.read_text(encoding="utf-8").strip() == first
        assert load_or_create_code(f, env={}) == first

    def test_environment_overrides_the_file(self, tmp_path):
        assert load_or_create_code(tmp_path / "x.txt", env={ENV_VAR: " my-code "}) == "MY-CODE"
        assert not (tmp_path / "x.txt").exists()
