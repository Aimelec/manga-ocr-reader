"""The setup and contract scripts, without network access."""
import io
import json
import lzma
import tarfile

import pytest

from backend.config import LlmSettings, load_settings
from scripts import setup_dictionary


def make_archive(path, db_bytes, member="jamdict_data-1.5/jamdict_data/jamdict.db.xz"):
    packed = lzma.compress(db_bytes)
    with tarfile.open(path, "w:gz") as tar:
        info = tarfile.TarInfo(member)
        info.size = len(packed)
        tar.addfile(info, io.BytesIO(packed))


class TestSetupDictionary:
    def test_unpacks_the_database_from_the_archive(self, tmp_path):
        make_archive(tmp_path / "data.tar.gz", b"SQLite format 3\x00 fake database")
        out = tmp_path / "dict" / "jamdict.db"
        assert setup_dictionary.main(["--from-archive", str(tmp_path / "data.tar.gz"), "--out", str(out)]) == 0
        assert out.read_bytes() == b"SQLite format 3\x00 fake database"
        assert not out.with_suffix(".db.part").exists()

    def test_keeps_an_existing_database_unless_forced(self, tmp_path):
        make_archive(tmp_path / "data.tar.gz", b"new")
        out = tmp_path / "jamdict.db"
        out.write_bytes(b"old")
        setup_dictionary.main(["--from-archive", str(tmp_path / "data.tar.gz"), "--out", str(out)])
        assert out.read_bytes() == b"old"
        setup_dictionary.main(["--from-archive", str(tmp_path / "data.tar.gz"), "--out", str(out), "--force"])
        assert out.read_bytes() == b"new"

    def test_archive_without_the_database_is_an_error(self, tmp_path):
        make_archive(tmp_path / "data.tar.gz", b"x", member="something/else.txt")
        with pytest.raises(RuntimeError):
            setup_dictionary.main(["--from-archive", str(tmp_path / "data.tar.gz"), "--out", str(tmp_path / "o.db")])


def test_example_llm_config_only_uses_known_settings(tmp_path):
    example = json.loads((setup_dictionary.ROOT / "llm" / "config.example.json").read_text(encoding="utf-8"))
    assert set(example) <= set(LlmSettings.__dataclass_fields__)
    (tmp_path / "llm").mkdir()
    (tmp_path / "llm" / "config.json").write_text(json.dumps(example), encoding="utf-8")
    assert load_settings(tmp_path).llm == LlmSettings(**example)
