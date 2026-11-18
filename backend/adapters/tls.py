"""LAN address detection and the self-signed HTTPS certificate for it."""
import os
import shutil
import socket
import subprocess
from pathlib import Path

# Git for Windows ships an openssl at this location.
GIT_OPENSSL = r"C:\Program Files\Git\mingw64\bin\openssl.exe"


def find_openssl():
    """OPENSSL env variable, then PATH, then Git for Windows' copy."""
    return os.environ.get("OPENSSL") or shutil.which("openssl") or GIT_OPENSSL


def lan_ip():
    """This PC's address on the local network (no packet is sent: UDP connect only picks a route)."""
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.connect(("192.168.0.1", 80))
        return s.getsockname()[0]
    except OSError:
        return "127.0.0.1"
    finally:
        s.close()


def ensure_cert(cert_dir, ip, openssl=None, run=subprocess.run):
    """Self-signed certificate for `ip`, remade when the address changes. Returns (key, cert) paths."""
    d = Path(cert_dir)
    d.mkdir(exist_ok=True)
    key, cert, ip_file = d / "key.pem", d / "cert.pem", d / "ip.txt"
    if cert.exists() and ip_file.exists() and ip_file.read_text() == ip:
        return str(key), str(cert)
    run([openssl or find_openssl(), "req", "-x509", "-newkey", "rsa:2048", "-nodes", "-days", "3650",
         "-keyout", str(key), "-out", str(cert), "-subj", "/CN=manga-reader",
         "-addext", f"subjectAltName=IP:{ip},IP:127.0.0.1"],
        check=True, capture_output=True)
    ip_file.write_text(ip)
    return str(key), str(cert)
