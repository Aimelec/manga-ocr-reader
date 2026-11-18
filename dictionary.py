"""Offline JMdict / KANJIDIC / JMnedict lookups (database in dict\\jamdict.db)."""
import threading
from functools import lru_cache
from pathlib import Path

DB = str(Path(__file__).parent / "dict" / "jamdict.db")
_lock = threading.Lock()
_jam = None


def jam():
    global _jam
    if _jam is None:
        from jamdict import Jamdict
        _jam = Jamdict(db_file=DB, kd2_file=DB, jmnedict_file=DB)
    return _jam


def _hira(text):
    return "".join(chr(ord(c) - 0x60) if "ァ" <= c <= "ヶ" else c for c in text)


# Sudachi part of speech -> words to look for in JMdict's part-of-speech labels
POS_HINT = {"動詞": "verb", "形容詞": "adjective (keiyoushi)", "形状詞": "adjectival noun",
            "名詞": "noun", "代名詞": "pronoun", "副詞": "adverb", "接続詞": "conjunction",
            "感動詞": "interjection", "連体詞": "pre-noun", "接頭辞": "prefix", "接尾辞": "suffix"}
COMMON = {"ichi1", "news1", "spec1", "spec2", "gai1"}


@lru_cache(maxsize=4096)
def lookup_word(word, reading="", names=False, normalized="", pos="", kana_surface=False,
                max_entries=3, max_senses=4):
    """Entries for a dictionary form, best guess first.

    Ranking: matches Sudachi's standard spelling (normalized) > reading > part of speech >
    common word; kana-only surfaces also favour entries marked 'usually written using kana'."""
    with _lock:
        result = jam().lookup(word, lookup_chars=False, lookup_ne=names)
    hint = POS_HINT.get(pos, "")
    entries = []
    for e in result.entries:
        kanji = [k.text for k in e.kanji_forms]
        kana = [k.text for k in e.kana_forms]
        all_pos = " ".join(p for s in e.senses for p in s.pos)
        pri = {p for f in list(e.kanji_forms) + list(e.kana_forms) for p in f.pri}
        score = (4 * (normalized in kanji or normalized in kana)
                 + 2 * (bool(reading) and reading in (_hira(k) for k in kana))
                 + 2 * (bool(hint) and hint in all_pos)
                 + 1 * bool(pri & COMMON)
                 + 1 * (kana_surface and any("usually written using kana" in m
                                             for s in e.senses for m in s.misc)))
        senses = [{"gloss": "; ".join(g.text for g in s.gloss), "pos": ", ".join(s.pos)}
                  for s in e.senses[:max_senses]]
        entries.append({"kanji": kanji[:3], "kana": kana[:3], "senses": senses, "score": score})
    entries.sort(key=lambda x: -x["score"])
    out = entries[:max_entries]
    if names:
        name_entries = [{
            "kanji": [k.text for k in n.kanji_forms][:1],
            "kana": [k.text for k in n.kana_forms][:1],
            "senses": [{"gloss": "; ".join(g.text for s in n.senses for g in s.gloss), "pos": "name"}],
            "score": 2 * (bool(reading) and reading in (_hira(k.text) for k in n.kana_forms)),
        } for n in result.names[:5]]
        name_entries.sort(key=lambda x: -x["score"])
        out = name_entries[:3] + out
    return out


@lru_cache(maxsize=4096)
def lookup_kanji(char):
    with _lock:
        result = jam().lookup(char, lookup_chars=True, lookup_ne=False)
    for c in result.chars:
        if c.literal != char:
            continue
        on, kun = [], []
        for g in c.rm_groups:
            on += [r.value for r in g.on_readings]
            kun += [r.value for r in g.kun_readings]
        return {"char": char, "meanings": c.meanings(english_only=True)[:5],
                "on": on[:4], "kun": kun[:4], "strokes": c.stroke_count}
    return None
