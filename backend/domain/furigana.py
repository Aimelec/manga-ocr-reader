"""Furigana alignment: put each reading only over the kanji it belongs to."""
import re

from backend.domain.japanese import KANJI_CHARS, KANJI_RE, kata_to_hira

_RUNS = re.compile(f"(?:[{KANJI_CHARS}]+)|(?:[^{KANJI_CHARS}]+)")


def split_ruby(surface, reading):
    """Align a word with its reading so furigana only sits over the kanji.

    '食べる' + 'たべる' -> [('食', 'た'), ('べる', '')]
    Falls back to the whole reading over the whole word when the kana parts do not line up.
    """
    if not reading or not KANJI_RE.search(surface):
        return [(surface, "")]
    # Group the surface into kanji runs and kana runs; kana runs must appear literally in the reading.
    parts = _RUNS.findall(surface)
    pattern = "".join("(.+?)" if KANJI_RE.match(p) else "(" + re.escape(kata_to_hira(p)) + ")" for p in parts)
    m = re.fullmatch(pattern, reading)
    if not m:
        return [(surface, reading)]
    return [(p, g if KANJI_RE.match(p) else "") for p, g in zip(parts, m.groups())]
