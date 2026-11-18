"""Paths and settings. Everything is relative to the project folder unless overridden."""
import json
import os
from dataclasses import dataclass, field
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


@dataclass(frozen=True)
class LlmSettings:
    model: str = "llm/models/gemma-4-12b/gemma-4-12b-it-Q6_K.gguf"
    device: str = "Vulkan0"          # llama.cpp device name (RX 9070 XT here)
    port: int = 8090
    context_size: int = 8192
    idle_seconds: int = 300
    temperature: float = 0.3
    max_tokens: int = 700


@dataclass(frozen=True)
class Settings:
    root: Path = ROOT
    port: int = 8765
    dict_db: Path = ROOT / "dict" / "jamdict.db"
    apk: Path = ROOT / "MangaReader.apk"
    cert_dir: Path = ROOT / "cert"
    llama_server: Path = ROOT / "tools" / "llama.cpp" / "llama-server.exe"
    llm_log: Path = ROOT / "llm" / "llama-server.log"
    llm: LlmSettings = field(default_factory=LlmSettings)


def load_settings(root=ROOT):
    """Defaults, overridden by llm/config.json (model choice) and MANGA_READER_PORT."""
    root = Path(root)
    llm = {}
    cfg = root / "llm" / "config.json"
    if cfg.exists():
        llm = json.loads(cfg.read_text(encoding="utf-8"))
    known = LlmSettings.__dataclass_fields__
    return Settings(
        root=root,
        port=int(os.environ.get("MANGA_READER_PORT", 8765)),
        dict_db=root / "dict" / "jamdict.db",
        apk=root / "MangaReader.apk",
        cert_dir=root / "cert",
        llama_server=root / "tools" / "llama.cpp" / "llama-server.exe",
        llm_log=root / "llm" / "llama-server.log",
        llm=LlmSettings(**{k: v for k, v in llm.items() if k in known}),
    )
