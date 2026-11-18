"""Core of the manga reader: image crop -> Japanese text (manga-ocr) -> readings (Sudachi).

Everything runs locally, no paid services.
"""

import cv2
import numpy as np
from PIL import Image

_ocr = None
_tokenizer = None

from backend.domain.furigana import split_ruby  # noqa: E402,F401
from backend.domain.japanese import KANJI_RE, kata_to_hira  # noqa: E402


def get_ocr():
    global _ocr
    if _ocr is None:
        from manga_ocr import MangaOcr
        _ocr = MangaOcr()
    return _ocr


def get_tokenizer():
    global _tokenizer
    if _tokenizer is None:
        from sudachipy import dictionary
        _tokenizer = dictionary.Dictionary(dict="core").create()
    return _tokenizer


def preprocess(img):
    """Clean up a phone photo crop: grayscale, even out lighting, enlarge small crops."""
    gray = cv2.cvtColor(np.array(img.convert("RGB")), cv2.COLOR_RGB2GRAY)
    h, w = gray.shape
    if max(h, w) < 400:
        scale = 400 / max(h, w)
        gray = cv2.resize(gray, (int(w * scale), int(h * scale)), interpolation=cv2.INTER_CUBIC)
    gray = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8)).apply(gray)
    return Image.fromarray(gray)


CONTENT_POS = {"名詞", "代名詞", "動詞", "形容詞", "形状詞", "副詞", "連体詞", "接続詞", "感動詞",
               "接頭辞", "接尾辞"}


def explain(m, surface, reading):
    """Dictionary meanings for content words, table explanations for particles/auxiliaries."""
    from sudachipy import tokenizer as sudachi_tokenizer
    from dictionary import lookup_kanji, lookup_word
    from grammar import FUNCTION_WORDS

    pos = m.part_of_speech()
    base = m.dictionary_form()
    info = {"function": "", "entries": [], "parts": [], "kanji": []}
    if pos[0] in ("助詞", "助動詞"):
        info["function"] = FUNCTION_WORDS.get(surface) or FUNCTION_WORDS.get(base, "")
        return info
    if pos[0] not in CONTENT_POS:
        return info
    names = pos[1] == "固有名詞"
    normalized = m.normalized_form()
    kana_surface = not KANJI_RE.search(surface)
    for query in dict.fromkeys([normalized, base, surface]):
        info["entries"] = list(lookup_word(query, kata_to_hira(reading) if query == surface else "", names,
                                           normalized, pos[0], kana_surface))
        if info["entries"]:
            break
    if not info["entries"]:
        # Long compound not in the dictionary: look up its smaller parts instead.
        for sub in m.split(sudachi_tokenizer.Tokenizer.SplitMode.A):
            if sub.part_of_speech()[0] in CONTENT_POS:
                sub_reading = kata_to_hira(sub.reading_form())
                entries = lookup_word(sub.dictionary_form(), sub_reading)
                if entries:
                    info["parts"].append({"surface": sub.surface(), "reading": sub_reading,
                                          "entries": list(entries[:1])})
    info["kanji"] = [k for k in (lookup_kanji(c) for c in dict.fromkeys(KANJI_RE.findall(surface))) if k]
    return info


def readings(text):
    from sudachipy import tokenizer as sudachi_tokenizer
    from grammar import FORM_EN, POS_EN
    out = []
    for m in get_tokenizer().tokenize(text, sudachi_tokenizer.Tokenizer.SplitMode.C):
        surface = m.surface()
        if not surface:  # Sudachi splits '…' into extra empty tokens
            continue
        pos = m.part_of_speech()
        has_kanji = bool(KANJI_RE.search(surface))
        reading = kata_to_hira(m.reading_form()) if has_kanji and not m.is_oov() else surface
        form = pos[5].split("-")[0] if pos[5] != "*" else ""
        out.append({
            "surface": surface,
            "reading": reading,
            "has_kanji": has_kanji,
            "dictionary_form": m.dictionary_form(),
            "pos": pos[0],
            "pos_en": POS_EN.get(pos[0], pos[0]) + (" (name)" if pos[1] == "固有名詞" else ""),
            "form_en": FORM_EN.get(form, "") if pos[0] in ("動詞", "形容詞", "助動詞") else "",
            "ruby": split_ruby(surface, reading),
            **explain(m, surface, reading),
        })
    return out


def analyze(text):
    from grammar import find_patterns
    tokens = readings(text)
    hiragana = "".join(t["reading"] or t["surface"] for t in tokens)
    grammar = find_patterns(text, [(t["surface"], t["pos"]) for t in tokens])
    return {"text": text, "hiragana": hiragana, "tokens": tokens, "grammar": grammar}


def read_image(img):
    return analyze(get_ocr()(preprocess(img)))
