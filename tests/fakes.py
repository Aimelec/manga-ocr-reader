"""Hand-written fakes for the domain interfaces (backend/domain/ports.py). No models, files or network."""
from contextlib import contextmanager

from backend.domain.ports import Morpheme


def morpheme(surface, pos0="名詞", pos1="普通名詞", form="*", base=None, normalized=None, reading=None,
             oov=False, parts=()):
    """Build a Morpheme; `parts` are the morphemes returned by FakeTokenizer.split()."""
    return Morpheme(surface=surface, pos=(pos0, pos1, "*", "*", "*", form), dictionary_form=base or surface,
                    normalized_form=normalized or base or surface, reading_form=reading or surface,
                    is_oov=oov, handle=tuple(parts))


class FakeTokenizer:
    def __init__(self, sentences):
        self.sentences = sentences          # text -> list[Morpheme]

    def tokenize(self, text):
        return list(self.sentences[text])

    def split(self, m):
        return list(m.handle)


class FakeDictionary:
    def __init__(self, words=None, kanji=None):
        self.words = words or {}            # query -> list of entry dicts
        self.kanji = kanji or {}            # char -> dict
        self.calls = []

    def lookup_word(self, word, reading="", names=False, normalized="", pos="", kana_surface=False):
        self.calls.append((word, reading, names, normalized, pos, kana_surface))
        return self.words.get(word, [])

    def lookup_kanji(self, char):
        return self.kanji.get(char)


class FakeOcr:
    def __init__(self, text):
        self.text = text
        self.images = []

    def read(self, image):
        self.images.append(image)
        return self.text


class FakeRuntime:
    def __init__(self, url="http://model"):
        self.url = url
        self.open = 0
        self.sessions = 0

    @contextmanager
    def session(self):
        self.open += 1
        self.sessions += 1
        try:
            yield self.url
        finally:
            self.open -= 1


class FakeChat:
    def __init__(self, pieces=("ok",), error=None):
        self.pieces = pieces
        self.error = error
        self.requests = []

    def stream_chat(self, base_url, messages, temperature, max_tokens):
        self.requests.append({"url": base_url, "messages": messages, "temperature": temperature,
                              "max_tokens": max_tokens})
        yield from self.pieces
        if self.error:
            raise self.error


def entry(gloss, kanji=(), kana=(), pos="noun"):
    return {"kanji": list(kanji), "kana": list(kana), "senses": [{"gloss": gloss, "pos": pos}], "score": 0}
