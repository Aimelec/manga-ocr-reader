"""Compatibility shim (removed at the end of the refactor): JMdict lookups through the new layers."""
from pathlib import Path

from backend.repositories.jmdict import JmdictRepository
from backend.services.word_lookup import WordLookup

DB = Path(__file__).parent / "dict" / "jamdict.db"
_lookup = WordLookup(JmdictRepository(DB))
lookup_word = _lookup.lookup_word
lookup_kanji = _lookup.lookup_kanji
