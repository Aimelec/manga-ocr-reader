"""Compatibility shim (removed at the end of the refactor): the reader now lives in backend.services,
wired by backend.container."""
from backend.container import Container

_container = Container()


def get_ocr():
    return _container.ocr.warm_up()


def get_tokenizer():
    return _container.tokenizer.warm_up()


def readings(text):
    return _container.analyzer.tokens(text)


def analyze(text):
    return _container.analyzer.analyze(text)


def read_image(img):
    return _container.bubble_reader.read(img)
