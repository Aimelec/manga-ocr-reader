"""Character-level helpers for Japanese text. Pure functions, no I/O."""
import re

# CJK ideographs (incl. extension A and compatibility forms) plus kanji-like marks: 々 〆 ヵ ヶ
KANJI_CHARS = "㐀-䶿一-鿿豈-﫿々〆ヵヶ"
KANJI_RE = re.compile(f"[{KANJI_CHARS}]")


def kata_to_hira(text):
    """Katakana -> hiragana, other characters unchanged ('タベル' -> 'たべる')."""
    return "".join(chr(ord(c) - 0x60) if "ァ" <= c <= "ヶ" else c for c in text)


def has_kanji(text):
    return bool(KANJI_RE.search(text))
