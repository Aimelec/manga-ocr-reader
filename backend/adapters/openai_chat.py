"""Chat client for any OpenAI-compatible server (llama.cpp's llama-server, LM Studio, vLLM, Ollama's /v1...)."""
import json
import urllib.request


def parse_sse(lines):
    """Server-sent-event lines (bytes or str) -> the content pieces of a streamed chat completion."""
    for raw in lines:
        s = (raw.decode("utf-8") if isinstance(raw, bytes) else raw).strip()
        if not s.startswith("data:"):
            continue
        data = s[5:].strip()
        if data == "[DONE]":
            break
        delta = json.loads(data)["choices"][0].get("delta", {}).get("content")
        if delta:
            yield delta


class OpenAiCompatibleChat:
    """Implements ChatModel."""

    def __init__(self, extra_body=None, timeout=300):
        # llama.cpp: no "thinking" phase for reasoning models
        self.extra_body = extra_body if extra_body is not None else {"chat_template_kwargs": {"enable_thinking": False}}
        self.timeout = timeout

    def stream_chat(self, base_url, messages, temperature, max_tokens):
        body = {"messages": messages, "temperature": temperature, "max_tokens": max_tokens, "stream": True,
                **self.extra_body}
        req = urllib.request.Request(base_url + "/v1/chat/completions", data=json.dumps(body).encode(),
                                     headers={"Content-Type": "application/json"})
        with urllib.request.urlopen(req, timeout=self.timeout) as r:
            yield from parse_sse(r)
