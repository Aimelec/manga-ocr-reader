"""Use cases for Japanese text: explain one word, analyze a whole line."""
from dataclasses import dataclass

from backend.domain.furigana import split_ruby
from backend.domain.japanese import KANJI_RE, kata_to_hira
from backend.domain.pos import CONTENT_POS, FUNCTION_POS, INFLECTING_POS, PROPER_NOUN


@dataclass(frozen=True)
class Vocabulary:
    """English labels and hand-written explanations (see backend.repositories.grammar_tables)."""
    pos_en: dict
    form_en: dict
    function_words: dict


class WordExplainer:
    """What one word means: table explanation for particles/auxiliaries, dictionary entries for
    content words (by parts for long compounds), and details of each kanji."""

    def __init__(self, tokenizer, dictionary, vocabulary):
        self.tokenizer = tokenizer
        self.dictionary = dictionary
        self.vocab = vocabulary

    def explain(self, m, reading):
        surface, pos, base = m.surface, m.pos, m.dictionary_form
        info = {"function": "", "entries": [], "parts": [], "kanji": []}
        if pos[0] in FUNCTION_POS:
            info["function"] = self.vocab.function_words.get(surface) or self.vocab.function_words.get(base, "")
            return info
        if pos[0] not in CONTENT_POS:
            return info
        info["entries"] = self._entries(m, reading)
        if not info["entries"]:
            info["parts"] = self._parts(m)
        info["kanji"] = [k for k in (self.dictionary.lookup_kanji(c) for c in dict.fromkeys(KANJI_RE.findall(surface)))
                         if k]
        return info

    def _entries(self, m, reading):
        """Try Sudachi's standard spelling, then the dictionary form, then the word as written."""
        names = m.pos[1] == PROPER_NOUN
        kana_surface = not KANJI_RE.search(m.surface)
        for query in dict.fromkeys([m.normalized_form, m.dictionary_form, m.surface]):
            entries = list(self.dictionary.lookup_word(query, kata_to_hira(reading) if query == m.surface else "",
                                                       names, m.normalized_form, m.pos[0], kana_surface))
            if entries:
                return entries
        return []

    def _parts(self, m):
        """Long compound not in the dictionary: look up its smaller parts instead."""
        parts = []
        for sub in self.tokenizer.split(m):
            if sub.pos[0] in CONTENT_POS:
                sub_reading = kata_to_hira(sub.reading_form)
                entries = self.dictionary.lookup_word(sub.dictionary_form, sub_reading)
                if entries:
                    parts.append({"surface": sub.surface, "reading": sub_reading, "entries": list(entries[:1])})
        return parts


class TextAnalyzer:
    """A line of Japanese -> words with readings, furigana, meanings, plus the grammar patterns it uses."""

    def __init__(self, tokenizer, explainer, grammar, vocabulary):
        self.tokenizer = tokenizer
        self.explainer = explainer
        self.grammar = grammar
        self.vocab = vocabulary

    def tokens(self, text):
        out = []
        for m in self.tokenizer.tokenize(text):
            if not m.surface:  # Sudachi splits '…' into extra empty tokens
                continue
            out.append(self._token(m))
        return out

    def _token(self, m):
        surface, pos = m.surface, m.pos
        has_kanji = bool(KANJI_RE.search(surface))
        reading = kata_to_hira(m.reading_form) if has_kanji and not m.is_oov else surface
        form = pos[5].split("-")[0] if pos[5] != "*" else ""
        return {
            "surface": surface,
            "reading": reading,
            "has_kanji": has_kanji,
            "dictionary_form": m.dictionary_form,
            "pos": pos[0],
            "pos_en": self.vocab.pos_en.get(pos[0], pos[0]) + (" (name)" if pos[1] == PROPER_NOUN else ""),
            "form_en": self.vocab.form_en.get(form, "") if pos[0] in INFLECTING_POS else "",
            "ruby": split_ruby(surface, reading),
            **self.explainer.explain(m, reading),
        }

    def analyze(self, text):
        tokens = self.tokens(text)
        return {
            "text": text,
            "hiragana": "".join(t["reading"] or t["surface"] for t in tokens),
            "tokens": tokens,
            "grammar": self.grammar.find(text, [(t["surface"], t["pos"]) for t in tokens]),
        }


class BubbleReader:
    """Photo of a speech bubble -> analyzed text."""

    def __init__(self, ocr, analyzer):
        self.ocr = ocr
        self.analyzer = analyzer

    def read(self, image):
        return self.analyzer.analyze(self.ocr.read(image))
