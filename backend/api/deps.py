"""FastAPI dependencies: controllers get their use cases from the container stored on the app."""
from fastapi import Request


def container(request: Request):
    return request.app.state.container


def bubble_reader(request: Request):
    return container(request).bubble_reader


def line_explainer(request: Request):
    return container(request).line_explainer


def settings(request: Request):
    return container(request).settings
