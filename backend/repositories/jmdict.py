"""Offline JMdict / KANJIDIC / JMnedict access (SQLite database built by jamdict).

Only fetches and maps rows to plain data; deciding which entry fits best is domain logic
(backend.domain.ranking).
"""
import threading
from dataclasses import dataclass


@dataclass(frozen=True)
class Sense:
    glosses: tuple[str, ...]
    pos: tuple[str, ...]
    misc: tuple[str, ...]


@dataclass(frozen=True)
class WordEntry:
    kanji: tuple[str, ...]
    kana: tuple[str, ...]
    priorities: frozenset[str]
    senses: tuple[Sense, ...]


@dataclass(frozen=True)
class NameEntry:
    kanji: tuple[str, ...]
    kana: tuple[str, ...]
    glosses: tuple[str, ...]


@dataclass(frozen=True)
class KanjiEntry:
    char: str
    meanings: tuple[str, ...]
    on: tuple[str, ...]
    kun: tuple[str, ...]
    strokes: int


class JmdictRepository:
    """Word, name and kanji lookups, safe to call from any thread.

    A SQLite connection may only be used by the thread that opened it, and the server answers
    requests on a pool of worker threads. So each thread gets its own jamdict instance (and
    connection), reused for all of its lookups and closed with the thread.
    """

    def __init__(self, db_file):
        self.db_file = str(db_file)
        self._local = threading.local()

    def _db(self):
        jam = getattr(self._local, "jam", None)
        if jam is None:
            from jamdict import Jamdict
            jam = self._local.jam = Jamdict(db_file=self.db_file, kd2_file=self.db_file,
                                            jmnedict_file=self.db_file)
        return jam

    def words(self, query, include_names=False):
        """All JMdict entries for `query` and, optionally, the first 5 JMnedict names."""
        result = self._db().lookup(query, lookup_chars=False, lookup_ne=include_names)
        words = [WordEntry(
            kanji=tuple(k.text for k in e.kanji_forms),
            kana=tuple(k.text for k in e.kana_forms),
            priorities=frozenset(p for f in list(e.kanji_forms) + list(e.kana_forms) for p in f.pri),
            senses=tuple(Sense(tuple(g.text for g in s.gloss), tuple(s.pos), tuple(s.misc)) for s in e.senses),
        ) for e in result.entries]
        names = [NameEntry(
            kanji=tuple(k.text for k in n.kanji_forms),
            kana=tuple(k.text for k in n.kana_forms),
            glosses=tuple(g.text for s in n.senses for g in s.gloss),
        ) for n in result.names[:5]] if include_names else []
        return words, names

    def kanji(self, char):
        result = self._db().lookup(char, lookup_chars=True, lookup_ne=False)
        for c in result.chars:
            if c.literal != char:
                continue
            on, kun = [], []
            for g in c.rm_groups:
                on += [r.value for r in g.on_readings]
                kun += [r.value for r in g.kun_readings]
            return KanjiEntry(char, tuple(c.meanings(english_only=True)), tuple(on), tuple(kun), c.stroke_count)
        return None
