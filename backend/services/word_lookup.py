"""Dictionary lookups as the app shows them: ranked, trimmed and cached."""
from functools import lru_cache

from backend.domain.ranking import Query, score_name, score_word


class WordLookup:
    """Best dictionary entries for a word, and kanji details. Depends on any repository with
    `words(query, include_names)` and `kanji(char)` (see backend.repositories.jmdict)."""

    def __init__(self, repository, max_entries=3, max_senses=4, cache_size=4096):
        self.repo = repository
        self.max_entries = max_entries
        self.max_senses = max_senses
        self.lookup_word = lru_cache(maxsize=cache_size)(self._lookup_word)
        self.lookup_kanji = lru_cache(maxsize=cache_size)(self._lookup_kanji)

    def _lookup_word(self, word, reading="", names=False, normalized="", pos="", kana_surface=False):
        """Entries for a dictionary form, best guess first (names first when `names`)."""
        words, name_rows = self.repo.words(word, include_names=names)
        query = Query(reading=reading, normalized=normalized, pos=pos, kana_surface=kana_surface)
        entries = [{
            "kanji": list(e.kanji[:3]), "kana": list(e.kana[:3]),
            "senses": [{"gloss": "; ".join(s.glosses), "pos": ", ".join(s.pos)} for s in e.senses[:self.max_senses]],
            "score": score_word(e, query),
        } for e in words]
        entries.sort(key=lambda x: -x["score"])
        out = entries[:self.max_entries]
        if names:
            name_entries = [{
                "kanji": list(n.kanji[:1]), "kana": list(n.kana[:1]),
                "senses": [{"gloss": "; ".join(n.glosses), "pos": "name"}],
                "score": score_name(n, reading),
            } for n in name_rows]
            name_entries.sort(key=lambda x: -x["score"])
            out = name_entries[:3] + out
        return out

    def _lookup_kanji(self, char):
        k = self.repo.kanji(char)
        if k is None:
            return None
        return {"char": k.char, "meanings": list(k.meanings[:5]), "on": list(k.on[:4]), "kun": list(k.kun[:4]),
                "strokes": k.strokes}
