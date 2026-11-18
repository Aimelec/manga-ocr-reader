"""Controller: OCR + readings + dictionary for a speech-bubble crop."""
import io

from fastapi import APIRouter, Depends, File, HTTPException, UploadFile
from PIL import Image, ImageOps, UnidentifiedImageError

from backend.api.deps import bubble_reader
from backend.api.schemas import ErrorResponse, ReadResponse

router = APIRouter()

MAX_UPLOAD_BYTES = 10 * 1024 * 1024     # the app sends crops of at most 1600 px (well under 1 MB)
MAX_PIXELS = 40_000_000                 # refuse "decompression bombs" (tiny file, huge image)


def decode_image(data):
    try:
        img = Image.open(io.BytesIO(data))
        if img.width * img.height > MAX_PIXELS:
            raise HTTPException(413, f"Image too large ({img.width}x{img.height} pixels)")
        return ImageOps.exif_transpose(img)
    except (UnidentifiedImageError, OSError, Image.DecompressionBombError):
        raise HTTPException(400, "The upload is not a readable image")


@router.post("/read", response_model=ReadResponse,
             responses={400: {"model": ErrorResponse}, 413: {"model": ErrorResponse}})
def read(image: UploadFile = File(...), reader=Depends(bubble_reader)):
    data = image.file.read(MAX_UPLOAD_BYTES + 1)
    if len(data) > MAX_UPLOAD_BYTES:
        raise HTTPException(413, f"Image larger than {MAX_UPLOAD_BYTES // (1024 * 1024)} MB")
    return reader.read(decode_image(data))
