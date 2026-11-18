"""Which dictionary entry fits the word in the sentence best. Pure scoring rules, no I/O.

Each rule is a function (entry, query) -> points; add a rule to RULES to extend the ranking.
"""
from dataclasses import dataclass

from backend.domain.japanese import kata_to_hira

# Sudachi part of speech -> words to look for in JMdict's part-of-speech labels
POS_HINT = {"動詞": "verb", "形容詞": "adjective (keiyoushi)", "形状詞": "adjectival noun",
            "名詞": "noun", "代名詞": "pronoun", "副詞": "adverb", "接続詞": "conjunction",
            "感動詞": "interjection", "連体詞": "pre-noun", "接頭辞": "prefix", "接尾辞": "suffix"}
COMMON = frozenset({"ichi1", "news1", "spec1", "spec2", "gai1"})


@dataclass(frozen=True)
class Query:
    """What we know about the word being looked up."""
    reading: str = ""          # hiragana reading from the tokenizer ('' = unknown)
    normalized: str = ""       # Sudachi's standard spelling
    pos: str = ""              # Sudachi part of speech (first level)
    kana_surface: bool = False  # the word was written in kana only


def standard_spelling(e, q):
    return 4 * (q.normalized in e.kanji or q.normalized in e.kana)


def same_reading(e, q):
    return 2 * (bool(q.reading) and q.reading in (kata_to_hira(k) for k in e.kana))


def same_part_of_speech(e, q):
    hint = POS_HINT.get(q.pos, "")
    return 2 * (bool(hint) and hint in " ".join(p for s in e.senses for p in s.pos))


def common_word(e, q):
    return 1 * bool(e.priorities & COMMON)


def usually_kana(e, q):
    return 1 * (q.kana_surface and any("usually written using kana" in m for s in e.senses for m in s.misc))


RULES = (standard_spelling, same_reading, same_part_of_speech, common_word, usually_kana)


def score_word(entry, query, rules=RULES):
    return sum(rule(entry, query) for rule in rules)


def score_name(entry, reading):
    return 2 * (bool(reading) and reading in (kata_to_hira(k) for k in entry.kana))
