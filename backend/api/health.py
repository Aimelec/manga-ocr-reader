"""Controller: connection check used by the app."""
from fastapi import APIRouter
from fastapi.responses import PlainTextResponse

router = APIRouter()


@router.get("/", response_class=PlainTextResponse)
def index():
    return "Manga Reader server is running. Install the app from /app.apk"
