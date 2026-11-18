"""Compatibility shim (removed at the end of the refactor): the AI explanation now lives in
backend.services.explain, with the model process in backend.adapters.llama_server."""
from reader_core import _container


class LlmService:
    def explain(self, line, context=()):
        return _container.line_explainer.explain(line, context)
