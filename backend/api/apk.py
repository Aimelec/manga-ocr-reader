"""Controller: download the Android app from the phone's browser."""
from fastapi import APIRouter, Depends
from fastapi.responses import FileResponse

from backend.api.deps import settings

router = APIRouter()


@router.get("/app.apk")
def apk(s=Depends(settings)):
    return FileResponse(s.apk, media_type="application/vnd.android.package-archive", filename="MangaReader.apk")
