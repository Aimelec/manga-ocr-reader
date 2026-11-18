"""Local server for the Android app (start_server.bat). Same Wi-Fi as the phone.
/read: OCR + readings + dictionary for a bubble crop · /explain: AI explanation (streamed) ·
/app.apk: the app. HTTPS with a home-made certificate; the app trusts it on first use."""
import io
import os
import shutil
import socket
import subprocess
from pathlib import Path

import uvicorn
from fastapi import FastAPI, File, UploadFile
from fastapi.responses import FileResponse, PlainTextResponse, StreamingResponse
from PIL import Image, ImageOps
from pydantic import BaseModel

from llm_service import LlmService
from reader_core import get_ocr, get_tokenizer, read_image

PORT = 8765
HERE = Path(__file__).parent
# openssl makes the self-signed certificate: set OPENSSL to its path or put it on PATH
# (Git for Windows ships one at the fallback location).
OPENSSL = (os.environ.get("OPENSSL") or shutil.which("openssl")
           or r"C:\Program Files\Git\mingw64\bin\openssl.exe")
app = FastAPI()
llm = LlmService()


@app.get("/", response_class=PlainTextResponse)
def index():
    """Connection check used by the app."""
    return "Manga Reader server is running. Install the app from /app.apk"


@app.get("/app.apk")
def apk():
    return FileResponse(HERE / "MangaReader.apk", media_type="application/vnd.android.package-archive",
                        filename="MangaReader.apk")


class ExplainRequest(BaseModel):
    line: str
    context: list[str] = []


@app.post("/explain")
def explain(req: ExplainRequest):
    """AI explanation of one bubble, streamed as plain text while the model writes it."""
    def stream():
        try:
            yield from llm.explain(req.line, req.context[-3:])
        except Exception as e:
            yield f"\n\nERROR: {e}"
    return StreamingResponse(stream(), media_type="text/plain; charset=utf-8")


@app.post("/read")
def read(image: UploadFile = File(...)):
    img = ImageOps.exif_transpose(Image.open(io.BytesIO(image.file.read())))
    return read_image(img)


def lan_ip():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.connect(("192.168.0.1", 80))
        return s.getsockname()[0]
    except OSError:
        return "127.0.0.1"
    finally:
        s.close()


def ensure_cert(ip):
    """Self-signed certificate for this PC's address, remade when the address changes."""
    d = HERE / "cert"
    d.mkdir(exist_ok=True)
    key, cert, ip_file = d / "key.pem", d / "cert.pem", d / "ip.txt"
    if cert.exists() and ip_file.exists() and ip_file.read_text() == ip:
        return str(key), str(cert)
    subprocess.run([OPENSSL, "req", "-x509", "-newkey", "rsa:2048", "-nodes", "-days", "3650",
                    "-keyout", str(key), "-out", str(cert), "-subj", "/CN=manga-reader",
                    "-addext", f"subjectAltName=IP:{ip},IP:127.0.0.1"],
                   check=True, capture_output=True)
    ip_file.write_text(ip)
    return str(key), str(cert)


if __name__ == "__main__":
    print("Loading OCR model...")
    get_ocr()
    get_tokenizer()
    ip = lan_ip()
    key, cert = ensure_cert(ip)
    print(f"\nReady. On your phone (same Wi-Fi) open:  https://{ip}:{PORT}")
    print("First time: Chrome warns the connection is not private -> Advanced -> Proceed.")
    print(f"Android app download:  https://{ip}:{PORT}/app.apk\n")
    uvicorn.run(app, host="0.0.0.0", port=PORT, log_level="warning",
                ssl_keyfile=key, ssl_certfile=cert)
