"""Composition root: the only place that picks concrete implementations and wires them together.
To swap an engine (another OCR, tokenizer or dictionary), change it here; the use cases stay untouched."""
from functools import cached_property

from backend.config import load_settings
from backend.domain.grammar import GrammarMatcher


class Container:
    def __init__(self, settings=None):
        self.settings = settings or load_settings()

    @cached_property
    def ocr(self):
        from backend.adapters.ocr import MangaOcrEngine
        return MangaOcrEngine()

    @cached_property
    def tokenizer(self):
        from backend.adapters.sudachi import SudachiTokenizer
        return SudachiTokenizer()

    @cached_property
    def dictionary(self):
        from backend.repositories.jmdict import JmdictRepository
        from backend.services.word_lookup import WordLookup
        return WordLookup(JmdictRepository(self.settings.dict_db))

    @cached_property
    def vocabulary(self):
        from backend.repositories import grammar_tables as t
        from backend.services.analysis import Vocabulary
        return Vocabulary(pos_en=t.POS_EN, form_en=t.FORM_EN, function_words=t.FUNCTION_WORDS)

    @cached_property
    def grammar(self):
        from backend.repositories.grammar_tables import PATTERNS
        return GrammarMatcher(PATTERNS)

    @cached_property
    def analyzer(self):
        from backend.services.analysis import TextAnalyzer, WordExplainer
        explainer = WordExplainer(self.tokenizer, self.dictionary, self.vocabulary)
        return TextAnalyzer(self.tokenizer, explainer, self.grammar, self.vocabulary)

    @cached_property
    def bubble_reader(self):
        from backend.services.analysis import BubbleReader
        return BubbleReader(self.ocr, self.analyzer)

    def warm_up(self):
        """Load the heavy models now instead of on the first request."""
        self.ocr.warm_up()
        self.tokenizer.warm_up()
