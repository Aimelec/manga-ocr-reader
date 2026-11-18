"""Compatibility shim: the tables live in backend.repositories.grammar_tables (removed at the end of the refactor)."""
from backend.domain.grammar import GrammarMatcher
from backend.repositories.grammar_tables import FORM_EN, FUNCTION_WORDS, PATTERNS, POS_EN  # noqa: F401

_matcher = GrammarMatcher(PATTERNS)


def find_patterns(text, tokens):
    return _matcher.find(text, tokens)
