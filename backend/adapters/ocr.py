"""OCR adapter: phone-photo clean-up (OpenCV) + manga-ocr (Transformer OCR for Japanese manga text)."""
import cv2
import numpy as np
from PIL import Image


def clean_photo(img, min_side=400, clahe_clip=2.0, clahe_grid=(8, 8)):
    """Grayscale, even out lighting (CLAHE), enlarge small crops so the longest side is >= min_side."""
    gray = cv2.cvtColor(np.array(img.convert("RGB")), cv2.COLOR_RGB2GRAY)
    h, w = gray.shape
    if max(h, w) < min_side:
        scale = min_side / max(h, w)
        gray = cv2.resize(gray, (int(w * scale), int(h * scale)), interpolation=cv2.INTER_CUBIC)
    gray = cv2.createCLAHE(clipLimit=clahe_clip, tileGridSize=clahe_grid).apply(gray)
    return Image.fromarray(gray)


class MangaOcrEngine:
    """Implements OcrEngine. The model (~400 MB) loads on first use or on warm_up()."""

    def __init__(self, preprocess=clean_photo):
        self.preprocess = preprocess
        self._model = None

    def warm_up(self):
        if self._model is None:
            from manga_ocr import MangaOcr
            self._model = MangaOcr()
        return self

    def read(self, image):
        return self.warm_up()._model(self.preprocess(image))
