"""Controller: OCR + readings + dictionary for a speech-bubble crop."""
import io

from fastapi import APIRouter, Depends, File, UploadFile
from PIL import Image, ImageOps

from backend.api.deps import bubble_reader
from backend.api.schemas import ReadResponse

router = APIRouter()


@router.post("/read", response_model=ReadResponse)
def read(image: UploadFile = File(...), reader=Depends(bubble_reader)):
    img = ImageOps.exif_transpose(Image.open(io.BytesIO(image.file.read())))
    return reader.read(img)
