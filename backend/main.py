"""HTTP server for the Android app (same Wi-Fi as the phone). Start with start_server.bat.

/read: OCR + readings + dictionary for a bubble crop · /explain: AI explanation (streamed) ·
/app.apk: the app. HTTPS with a home-made certificate; the app trusts it on first use.
"""
import uvicorn
from fastapi import FastAPI

from backend.api import apk, explain, health, read
from backend.container import Container


def create_app(container=None):
    """App factory: tests pass a container with fake use cases."""
    app = FastAPI(title="Manga Reader")
    app.state.container = container or Container()
    for module in (health, apk, read, explain):
        app.include_router(module.router)
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
    print(f"Android app download:  https://{ip}:{s.port}/app.apk\n")
    uvicorn.run(create_app(container), host="0.0.0.0", port=s.port, log_level="warning",
                ssl_keyfile=key, ssl_certfile=cert)


if __name__ == "__main__":
    main()
