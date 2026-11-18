"""Tokenizer adapter: SudachiPy (core dictionary) mapped to domain Morphemes."""
import threading

from backend.domain.ports import Morpheme


def _to_morpheme(m):
    return Morpheme(surface=m.surface(), pos=tuple(m.part_of_speech()), dictionary_form=m.dictionary_form(),
                    normalized_form=m.normalized_form(), reading_form=m.reading_form(), is_oov=m.is_oov(),
                    handle=m)


class SudachiTokenizer:
    """Implements Tokenizer, safe to call from any thread.

    The Sudachi dictionary (the big part) is loaded once and shared. A Sudachi Tokenizer cannot be used
    by two threads at once ("Tokenizer is already in use"), and the server answers requests on a pool
    of worker threads, so each thread gets its own lightweight tokenizer.
    """

    def __init__(self, dictionary="core"):
        self.dictionary = dictionary
        self._dict = None
        self._load_lock = threading.Lock()
        self._local = threading.local()

    def warm_up(self):
        if self._dict is None:
            with self._load_lock:
                if self._dict is None:
                    from sudachipy import dictionary
                    self._dict = dictionary.Dictionary(dict=self.dictionary)
        return self

    def _tokenizer(self):
        tok = getattr(self._local, "tok", None)
        if tok is None:
            tok = self._local.tok = self.warm_up()._dict.tokenizer()
        return tok

    def tokenize(self, text):
        from sudachipy import tokenizer
        return [_to_morpheme(m) for m in self._tokenizer().tokenize(text, tokenizer.Tokenizer.SplitMode.C)]

    def split(self, morpheme):
        from sudachipy import tokenizer
        return [_to_morpheme(m) for m in morpheme.handle.split(tokenizer.Tokenizer.SplitMode.A)]
