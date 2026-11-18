"""FastAPI dependencies: controllers get their use cases from the container stored on the app."""
import hmac

from fastapi import Header, HTTPException, Request


def container(request: Request):
    return request.app.state.container


def bubble_reader(request: Request):
    return container(request).bubble_reader


def line_explainer(request: Request):
    return container(request).line_explainer


def settings(request: Request):
    return container(request).settings


PAIRING_HEADER = "X-Pairing-Code"


def require_pairing_code(request: Request, x_pairing_code: str = Header(default="", alias=PAIRING_HEADER)):
    """Rejects requests without the code shown in the server window (constant-time comparison)."""
    expected = container(request).pairing_code
    if not hmac.compare_digest(x_pairing_code.strip().upper().encode(), expected.encode()):
        raise HTTPException(401, "Wrong or missing pairing code: type the code shown in the server window "
                                 "into the app's settings")
