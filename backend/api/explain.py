"""Controller: AI explanation of one bubble, streamed as plain text while the model writes it."""
from fastapi import APIRouter, Depends, HTTPException
from fastapi.responses import StreamingResponse

from backend.api.deps import line_explainer
from backend.api.schemas import MAX_CONTEXT_BUBBLES, ErrorResponse, ExplainRequest

router = APIRouter()

# Written into the stream if the model fails after it started answering (the HTTP status is already sent).
STREAM_ERROR_MARKER = "\n\nERROR: "


@router.post("/explain", response_class=StreamingResponse,
             responses={200: {"content": {"text/plain": {}},
                              "description": f"The answer as it is written. If the model fails midway, the "
                                             f"stream ends with '{STREAM_ERROR_MARKER.strip()} <reason>'."},
                        503: {"model": ErrorResponse, "description": "The AI model could not start or answer"}})
def explain(req: ExplainRequest, explainer=Depends(line_explainer)):
    pieces = explainer.explain(req.line, req.context[-MAX_CONTEXT_BUBBLES:])
    try:
        first = next(pieces, "")          # loads the model if needed: failures here get a real HTTP error
    except Exception as e:
        raise HTTPException(503, f"The AI model is not available: {e}")

    def stream():
        yield first
        try:
            yield from pieces
        except Exception as e:
            yield f"{STREAM_ERROR_MARKER}{e}"
    return StreamingResponse(stream(), media_type="text/plain; charset=utf-8")
