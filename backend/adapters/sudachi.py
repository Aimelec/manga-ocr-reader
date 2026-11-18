"""Tokenizer adapter: SudachiPy (core dictionary) mapped to domain Morphemes."""
from backend.domain.ports import Morpheme


def _to_morpheme(m):
    return Morpheme(surface=m.surface(), pos=tuple(m.part_of_speech()), dictionary_form=m.dictionary_form(),
                    normalized_form=m.normalized_form(), reading_form=m.reading_form(), is_oov=m.is_oov(),
                    handle=m)


class SudachiTokenizer:
    """Implements Tokenizer. The dictionary loads on first use or on warm_up()."""

    def __init__(self, dictionary="core"):
        self.dictionary = dictionary
        self._tok = None

    def warm_up(self):
        if self._tok is None:
            from sudachipy import dictionary
            self._tok = dictionary.Dictionary(dict=self.dictionary).create()
        return self

    def tokenize(self, text):
        from sudachipy import tokenizer
        return [_to_morpheme(m) for m in self.warm_up()._tok.tokenize(text, tokenizer.Tokenizer.SplitMode.C)]

    def split(self, morpheme):
        from sudachipy import tokenizer
        return [_to_morpheme(m) for m in morpheme.handle.split(tokenizer.Tokenizer.SplitMode.A)]
