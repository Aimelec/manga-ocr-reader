"""Runs llama.cpp's llama-server on demand and stops it after a while without use,
so the graphics card is free for other work the rest of the time."""
import subprocess
import threading
import time
import urllib.request
from contextlib import contextmanager


class LlamaServer:
    """Implements ModelRuntime: `with server.session(): ...` keeps the model loaded while in use."""

    def __init__(self, executable, model, device, port, context_size, idle_seconds, log_file,
                 tie_to_parent=None, clock=time.time, watch_interval=15):
        self.executable = executable
        self.model = model
        self.device = device
        self.port = port
        self.context_size = context_size
        self.idle_seconds = idle_seconds
        self.log_file = log_file
        self.tie_to_parent = tie_to_parent
        self.clock = clock
        self.watch_interval = watch_interval
        self.proc = None
        self.job = None
        self.lock = threading.Lock()
        self.last_used = 0.0
        self.active = 0
        self._watcher = None

    @property
    def url(self):
        return f"http://127.0.0.1:{self.port}"

    def start_idle_watch(self):
        if self._watcher is None:
            self._watcher = threading.Thread(target=self._idle_watch, daemon=True)
            self._watcher.start()

    @contextmanager
    def session(self):
        self.ensure_running()
        self.active += 1
        try:
            yield self.url
        finally:
            self.active -= 1
            self.last_used = self.clock()

    def _healthy(self):
        try:
            with urllib.request.urlopen(self.url + "/health", timeout=2) as r:
                return r.status == 200
        except Exception:
            return False

    def ensure_running(self):
        with self.lock:
            self.last_used = self.clock()
            if self.proc is not None and self.proc.poll() is None and self._healthy():
                return
            if not self.model.exists():
                raise FileNotFoundError(f"Model not found: {self.model}")
            log = open(self.log_file, "w", encoding="utf-8")
            self.proc = subprocess.Popen(
                [str(self.executable), "-m", str(self.model), "--device", self.device, "-ngl", "99",
                 "-c", str(self.context_size), "--host", "127.0.0.1", "--port", str(self.port)],
                stdout=log, stderr=subprocess.STDOUT, creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0))
            if self.tie_to_parent is not None:
                try:
                    self.job = self.tie_to_parent(self.proc)
                except Exception as e:
                    print(f"warning: could not tie the AI model to the server process ({e})", flush=True)
            start = time.time()
            while time.time() - start < 180:
                if self.proc.poll() is not None:
                    raise RuntimeError(f"The AI model failed to start (see {self.log_file})")
                if self._healthy():
                    print("AI model loaded", flush=True)
                    return
                time.sleep(0.5)
            raise TimeoutError("The AI model took too long to load")

    def stop(self):
        with self.lock:
            if self.proc is not None and self.proc.poll() is None:
                self.proc.terminate()
                try:
                    self.proc.wait(timeout=20)
                except subprocess.TimeoutExpired:
                    self.proc.kill()
                print("AI model unloaded (idle)", flush=True)
            self.proc = None

    def is_idle(self):
        return (self.proc is not None and self.active == 0
                and self.clock() - self.last_used > self.idle_seconds)

    def _idle_watch(self):
        while True:
            time.sleep(self.watch_interval)
            if self.is_idle():
                self.stop()
