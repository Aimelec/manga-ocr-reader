"""HTTP server for the Android app (same Wi-Fi as the phone). Start with start_server.bat.

/api/v1/read: OCR + readings + dictionary for a bubble crop · /api/v1/explain: AI explanation (streamed) ·
/app.apk: the app. HTTPS with a home-made certificate; the app trusts it on first use.
"""
import uvicorn
from fastapi import Depends, FastAPI

from backend.api import apk, explain, health, read
from backend.api.deps import require_pairing_code
from backend.container import Container

API_VERSION = "1"
API_PREFIX = f"/api/v{API_VERSION}"
VERSIONED = (read, explain)


def create_app(container=None):
    """App factory: tests pass a container with fake use cases."""
    app = FastAPI(title="Manga Reader", version=API_VERSION)
    app.state.container = container or Container()
    for module in (health, apk):
        app.include_router(module.router)
    for module in VERSIONED:
        app.include_router(module.router, prefix=API_PREFIX, dependencies=[Depends(require_pairing_code)])
    # Unversioned paths for app builds older than the /api/v1 switch (they do not send the pairing code).
    for module in VERSIONED:
        app.include_router(module.router, deprecated=True, include_in_schema=False)
    return app


def main():
    from backend.adapters.tls import ensure_cert, lan_ip

    container = Container()
    print("Loading OCR model...")
    container.warm_up()
    s = container.settings
    ip = lan_ip()
    key, cert = ensure_cert(s.cert_dir, ip)
    print(f"\nReady. On your phone (same Wi-Fi) open:  https://{ip}:{s.port}")
    print("First time: Chrome warns the connection is not private -> Advanced -> Proceed.")
    print(f"Android app download:  https://{ip}:{s.port}/app.apk")
    print(f"Pairing code for the app:  {container.pairing_code}\n")
    uvicorn.run(create_app(container), host="0.0.0.0", port=s.port, log_level="warning",
                ssl_keyfile=key, ssl_certfile=cert)


if __name__ == "__main__":
    main()
