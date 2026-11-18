"""Pairing code: a short secret the server shows once and the app sends with every request, so other
devices on the same Wi-Fi cannot use the server. (The app already pins the server's certificate; this
protects the other direction.)"""
import os
import secrets
from pathlib import Path

# No 0/O, 1/I/L: easy to read on the PC screen and type on the phone. 8 characters ~ 39 bits.
ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
LENGTH = 8
ENV_VAR = "MANGA_READER_PAIRING_CODE"


def new_code(length=LENGTH):
    return "".join(secrets.choice(ALPHABET) for _ in range(length))


def load_or_create_code(path, env=os.environ):
    """The code from MANGA_READER_PAIRING_CODE, else the saved one, else a new one (saved for next time)."""
    if env.get(ENV_VAR):
        return env[ENV_VAR].strip().upper()
    path = Path(path)
    if path.exists():
        code = path.read_text(encoding="utf-8").strip().upper()
        if code:
            return code
    code = new_code()
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(code + "\n", encoding="utf-8")
    return code
