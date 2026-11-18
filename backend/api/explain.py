"""Controller: AI explanation of one bubble, streamed as plain text while the model writes it."""
from fastapi import APIRouter, Depends
from fastapi.responses import StreamingResponse

from backend.api.deps import line_explainer
from backend.api.schemas import MAX_CONTEXT_BUBBLES, ExplainRequest

router = APIRouter()


@router.post("/explain")
def explain(req: ExplainRequest, explainer=Depends(line_explainer)):
    def stream():
        try:
            yield from explainer.explain(req.line, req.context[-MAX_CONTEXT_BUBBLES:])
        except Exception as e:
            yield f"\n\nERROR: {e}"
    return StreamingResponse(stream(), media_type="text/plain; charset=utf-8")
