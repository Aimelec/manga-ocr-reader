"""Interfaces the use cases depend on (dependency inversion). Implementations live in backend.adapters
and backend.repositories; tests use fakes. Any class with these methods fits: no inheritance needed."""
from dataclasses import dataclass, field
from typing import ContextManager, Iterator, Optional, Protocol

from PIL import Image


@dataclass(frozen=True)
class Morpheme:
    """One word as the tokenizer sees it (Sudachi conventions: 6-level part of speech, katakana reading)."""
    surface: str
    pos: tuple[str, ...]
    dictionary_form: str
    normalized_form: str
    reading_form: str
    is_oov: bool = False
    handle: object = field(default=None, compare=False, repr=False)   # adapter-specific, for split()


class OcrEngine(Protocol):
    def read(self, image: Image.Image) -> str: ...


class Tokenizer(Protocol):
    def tokenize(self, text: str) -> list[Morpheme]:
        """Longest units (Sudachi mode C)."""

    def split(self, morpheme: Morpheme) -> list[Morpheme]:
        """Shortest units of one word (Sudachi mode A), to look up long compounds by parts."""


class WordDictionary(Protocol):
    def lookup_word(self, word: str, reading: str = "", names: bool = False, normalized: str = "",
                    pos: str = "", kana_surface: bool = False) -> list[dict]: ...

    def lookup_kanji(self, char: str) -> Optional[dict]: ...



class ChatModel(Protocol):
    def stream_chat(self, base_url: str, messages: list[dict], temperature: float, max_tokens: int) -> Iterator[str]:
        """Yields the answer piece by piece."""


class ModelRuntime(Protocol):
    def session(self) -> ContextManager[str]:
        """Keeps the model available inside the `with` block; yields its base URL."""
