"""Download and unpack the offline dictionary (JMdict + KANJIDIC2 + JMnedict, SQLite, ~310 MB unpacked).

    venv\\Scripts\\python.exe scripts\\setup_dictionary.py
    venv\\Scripts\\python.exe scripts\\setup_dictionary.py --from-archive path\\to\\jamdict_data-1.5.tar.gz

The database comes from the `jamdict-data` package on PyPI. Its installer fails on Windows, so this
script takes the archive directly, checks its SHA-256 against PyPI, and unpacks `jamdict.db.xz` to
dict/jamdict.db. The data is (c) the Electronic Dictionary Research and Development Group, CC BY-SA 4.0.
"""
import argparse
import hashlib
import json
import lzma
import shutil
import sys
import tarfile
import tempfile
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_OUT = ROOT / "dict" / "jamdict.db"
PACKAGE, VERSION = "jamdict-data", "1.5"
MEMBER_SUFFIX = "jamdict_data/jamdict.db.xz"


def pypi_sdist():
    """(url, sha256) of the package's source archive, from PyPI's JSON API."""
    with urllib.request.urlopen(f"https://pypi.org/pypi/{PACKAGE}/{VERSION}/json", timeout=30) as r:
        info = json.load(r)
    for f in info["urls"]:
        if f["packagetype"] == "sdist":
            return f["url"], f["digests"]["sha256"]
    raise RuntimeError(f"No source archive for {PACKAGE} {VERSION} on PyPI")


def sha256(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def download(url, dest):
    print(f"Downloading {url} ...")
    with urllib.request.urlopen(url, timeout=60) as r, open(dest, "wb") as f:
        shutil.copyfileobj(r, f, 1 << 20)


def unpack(archive, out):
    """Stream jamdict.db.xz out of the tar archive and decompress it to `out` (via a temp file)."""
    out.parent.mkdir(parents=True, exist_ok=True)
    tmp = out.with_suffix(".db.part")
    with tarfile.open(archive, "r:gz") as tar:
        member = next((m for m in tar.getmembers() if m.isfile() and m.name.endswith(MEMBER_SUFFIX)), None)
        if member is None:
            raise RuntimeError(f"{archive} does not contain {MEMBER_SUFFIX}")
        with tar.extractfile(member) as packed, lzma.open(packed) as db, open(tmp, "wb") as f:
            shutil.copyfileobj(db, f, 1 << 20)
    tmp.replace(out)


def main(argv=None):
    p = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    p.add_argument("--from-archive", type=Path, help="use an already downloaded jamdict_data-1.5.tar.gz")
    p.add_argument("--out", type=Path, default=DEFAULT_OUT, help=f"where to write the database ({DEFAULT_OUT})")
    p.add_argument("--force", action="store_true", help="replace an existing database")
    args = p.parse_args(argv)

    if args.out.exists() and not args.force:
        print(f"{args.out} already exists (use --force to replace it).")
        return 0
    url, expected = pypi_sdist() if args.from_archive is None else (None, None)
    with tempfile.TemporaryDirectory() as tmpdir:
        archive = args.from_archive
        if archive is None:
            archive = Path(tmpdir) / Path(url).name
            download(url, archive)
            if sha256(archive) != expected:
                raise SystemExit("Checksum mismatch: the download is not the file PyPI published. Nothing was written.")
        print(f"Unpacking to {args.out} ...")
        unpack(archive, args.out)
    print(f"Done: {args.out} ({args.out.stat().st_size // (1024 * 1024)} MB)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
