"""The API contract: request and response shapes. FastAPI validates against them and publishes them
as OpenAPI (contract/openapi.json), which the Android app's data layer mirrors."""
from pydantic import BaseModel, Field

MAX_CONTEXT_BUBBLES = 3


class Sense(BaseModel):
    gloss: str = Field(description="English meanings, '; '-separated")
    pos: str = Field(description="JMdict part-of-speech labels, ', '-separated ('name' for JMnedict)")


class Entry(BaseModel):
    kanji: list[str]
    kana: list[str]
    senses: list[Sense]
    score: int = Field(description="How well the entry fits the word in this sentence (higher = better)")


class Part(BaseModel):
    """Part of a long compound that is not in the dictionary as a whole."""
    surface: str
    reading: str
    entries: list[Entry]


class Kanji(BaseModel):
    char: str
    meanings: list[str]
    on: list[str]
    kun: list[str]
    strokes: int


class Token(BaseModel):
    surface: str
    reading: str = Field(description="Hiragana reading (the surface itself for kana and unknown words)")
    has_kanji: bool
    dictionary_form: str
    pos: str = Field(description="Sudachi part of speech (Japanese)")
    pos_en: str
    form_en: str = Field(description="Conjugation form, for verbs, adjectives and auxiliaries")
    ruby: list[tuple[str, str]] = Field(description="(text, furigana or '') pairs: furigana only over kanji")
    function: str = Field(description="Explanation for particles and auxiliaries")
    entries: list[Entry]
    parts: list[Part]
    kanji: list[Kanji]


class GrammarHit(BaseModel):
    name: str
    match: str
    explanation: str
    at: int = Field(description="Character offset of the match in the text")


class ReadResponse(BaseModel):
    text: str = Field(description="Japanese text recognized in the image")
    hiragana: str
    tokens: list[Token]
    grammar: list[GrammarHit]


class ExplainRequest(BaseModel):
    line: str = Field(description="The bubble to explain")
    context: list[str] = Field(default=[], description=f"Previous bubbles, oldest first; the last "
                                                       f"{MAX_CONTEXT_BUBBLES} are used")


class ErrorResponse(BaseModel):
    detail: str
