"""Use case: AI explanation of one speech bubble, streamed while the local model writes it."""
from backend.domain.prompts import SYSTEM_WITH_ANALYSIS, analysis_block, user_message


class LineExplainer:
    """Builds the prompt (line + previous bubbles + our word analysis) and streams the model's answer.

    runtime: ModelRuntime (keeps the model loaded, gives its base URL) · chat: ChatModel · analyzer: TextAnalyzer
    """

    def __init__(self, runtime, chat, analyzer, temperature, max_tokens, system=SYSTEM_WITH_ANALYSIS):
        self.runtime = runtime
        self.chat = chat
        self.analyzer = analyzer
        self.temperature = temperature
        self.max_tokens = max_tokens
        self.system = system

    def messages(self, line, context=()):
        # readings + dictionary forms from our own analysis help the model a little (see llm\test_context.py)
        user = user_message(line, list(context)) + "\n\n" + analysis_block(self.analyzer.tokens(line))
        return [{"role": "system", "content": self.system}, {"role": "user", "content": user}]

    def explain(self, line, context=()):
        """Yields the answer piece by piece as the model writes it."""
        messages = self.messages(line, context)
        with self.runtime.session() as base_url:
            yield from self.chat.stream_chat(base_url, messages, self.temperature, self.max_tokens)
