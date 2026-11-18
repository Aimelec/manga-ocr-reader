"""Local AI explanations (Gemma via llama.cpp on the RX 9070 XT).

The model is loaded on the first request and unloaded after IDLE_SECONDS without use,
so the graphics card is free for ComfyUI the rest of the time. Settings: llm\\config.json.
"""
import json
import subprocess
import threading
import time
import urllib.request
from pathlib import Path

from llm.prompt import SYSTEM_WITH_ANALYSIS, analysis_block, user_message

HERE = Path(__file__).parent
CONFIG = HERE / "llm" / "config.json"
DEFAULTS = {
    "model": "llm/models/gemma-4-12b/gemma-4-12b-it-Q6_K.gguf",
    "device": "Vulkan0",          # RX 9070 XT in llama.cpp's numbering
    "port": 8090,
    "context_size": 8192,
    "idle_seconds": 300,
    "temperature": 0.3,
}


def _kill_with_parent(proc):
    """Put the child in a Windows job object that closes when this server process ends,
    so the model never keeps the video memory after the server window is closed."""
    import ctypes
    from ctypes import wintypes

    class IO_COUNTERS(ctypes.Structure):
        _fields_ = [(n, ctypes.c_ulonglong) for n in (
            "ReadOperationCount", "WriteOperationCount", "OtherOperationCount",
            "ReadTransferCount", "WriteTransferCount", "OtherTransferCount")]

    class BASIC_LIMITS(ctypes.Structure):
        _fields_ = [("PerProcessUserTimeLimit", ctypes.c_longlong), ("PerJobUserTimeLimit", ctypes.c_longlong),
                    ("LimitFlags", wintypes.DWORD), ("MinimumWorkingSetSize", ctypes.c_size_t),
                    ("MaximumWorkingSetSize", ctypes.c_size_t), ("ActiveProcessLimit", wintypes.DWORD),
                    ("Affinity", ctypes.c_size_t), ("PriorityClass", wintypes.DWORD),
                    ("SchedulingClass", wintypes.DWORD)]

    class EXTENDED_LIMITS(ctypes.Structure):
        _fields_ = [("BasicLimitInformation", BASIC_LIMITS), ("IoInfo", IO_COUNTERS),
                    ("ProcessMemoryLimit", ctypes.c_size_t), ("JobMemoryLimit", ctypes.c_size_t),
                    ("PeakProcessMemoryUsed", ctypes.c_size_t), ("PeakJobMemoryUsed", ctypes.c_size_t)]

    k32 = ctypes.WinDLL("kernel32", use_last_error=True)
    k32.CreateJobObjectW.restype = wintypes.HANDLE
    k32.OpenProcess.restype = wintypes.HANDLE
    job = k32.CreateJobObjectW(None, None)
    info = EXTENDED_LIMITS()
    info.BasicLimitInformation.LimitFlags = 0x2000   # JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE
    k32.SetInformationJobObject(wintypes.HANDLE(job), 9, ctypes.byref(info), ctypes.sizeof(info))  # 9 = extended limits
    handle = k32.OpenProcess(0x0001 | 0x0100, False, proc.pid)   # PROCESS_TERMINATE | PROCESS_SET_QUOTA
    k32.AssignProcessToJobObject(wintypes.HANDLE(job), wintypes.HANDLE(handle))
    k32.CloseHandle(wintypes.HANDLE(handle))
    return job   # keep the handle open for the server's lifetime; Windows closes it when the server exits


def config():
    cfg = dict(DEFAULTS)
    if CONFIG.exists():
        cfg.update(json.loads(CONFIG.read_text(encoding="utf-8")))
    return cfg


class LlmService:
    def __init__(self):
        self.cfg = config()
        self.proc = None
        self.lock = threading.Lock()
        self.last_used = 0.0
        self.active = 0
        threading.Thread(target=self._idle_watch, daemon=True).start()

    @property
    def url(self):
        return f"http://127.0.0.1:{self.cfg['port']}"

    def _healthy(self):
        try:
            with urllib.request.urlopen(self.url + "/health", timeout=2) as r:
                return r.status == 200
        except Exception:
            return False

    def ensure_running(self):
        with self.lock:
            self.last_used = time.time()
            if self.proc is not None and self.proc.poll() is None and self._healthy():
                return
            model = HERE / self.cfg["model"]
            if not model.exists():
                raise FileNotFoundError(f"Model not found: {model}")
            log = open(HERE / "llm" / "llama-server.log", "w", encoding="utf-8")
            self.proc = subprocess.Popen(
                [str(HERE / "tools" / "llama.cpp" / "llama-server.exe"), "-m", str(model),
                 "--device", self.cfg["device"], "-ngl", "99", "-c", str(self.cfg["context_size"]),
                 "--host", "127.0.0.1", "--port", str(self.cfg["port"])],
                stdout=log, stderr=subprocess.STDOUT, creationflags=subprocess.CREATE_NO_WINDOW)
            try:
                self.job = _kill_with_parent(self.proc)
            except Exception as e:
                print(f"warning: could not tie the AI model to the server process ({e})", flush=True)
            start = time.time()
            while time.time() - start < 180:
                if self.proc.poll() is not None:
                    raise RuntimeError("The AI model failed to start (see llm\\llama-server.log)")
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

    def _idle_watch(self):
        while True:
            time.sleep(15)
            if (self.proc is not None and self.active == 0
                    and time.time() - self.last_used > self.cfg["idle_seconds"]):
                self.stop()

    def explain(self, line, context=()):
        """Yields the answer piece by piece as the model writes it."""
        self.ensure_running()
        self.active += 1
        try:
            # readings + dictionary forms from our own analysis help the model a little (see llm\test_context.py)
            from reader_core import readings
            user = user_message(line, list(context)) + "\n\n" + analysis_block(readings(line))
            body = {
                "messages": [{"role": "system", "content": SYSTEM_WITH_ANALYSIS},
                             {"role": "user", "content": user}],
                "temperature": self.cfg["temperature"],
                "max_tokens": 700,
                "stream": True,
                "chat_template_kwargs": {"enable_thinking": False},
            }
            req = urllib.request.Request(self.url + "/v1/chat/completions", data=json.dumps(body).encode(),
                                         headers={"Content-Type": "application/json"})
            with urllib.request.urlopen(req, timeout=300) as r:
                for raw in r:
                    s = raw.decode("utf-8").strip()
                    if not s.startswith("data:"):
                        continue
                    data = s[5:].strip()
                    if data == "[DONE]":
                        break
                    delta = json.loads(data)["choices"][0].get("delta", {}).get("content")
                    if delta:
                        yield delta
        finally:
            self.active -= 1
            self.last_used = time.time()
