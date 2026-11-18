"""Compatibility shim (removed at the end of the refactor): JMdict lookups through the new layers."""
from reader_core import _container

lookup_word = _container.dictionary.lookup_word
lookup_kanji = _container.dictionary.lookup_kanji
