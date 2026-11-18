"""Prompt for the sentence explanation (shared by the model test and the server)."""

SYSTEM = """You help an intermediate Japanese learner read manga. They know basic grammar but struggle with kanji and nuance.
You get the current speech bubble (OCR text, it may contain recognition mistakes) and sometimes the previous bubbles for context.
Answer in English, exactly in this format:

TRANSLATION: <natural English translation of the current bubble only>
LITERAL: <a close, word-by-word style rendering that shows the Japanese structure>
NOTES:
- <tone and speech style: polite, rough, archaic, dialect, gendered speech, etc.>
- <idioms, set phrases, proverbs, slang and contractions, explained>
- <grammar points worth knowing>
- <who is probably doing what, if the subject or object is left out (use the context)>
OCR: ok

Rules:
- Write 2 to 5 short notes. Do not translate the context bubbles.
- Never write romaji or kana readings; the learner's app already shows readings. Quote Japanese words in Japanese script.
- The OCR line is exactly "ok" unless a character was clearly misrecognized; only then write the corrected Japanese text.
  Dialect, archaic forms, rough speech and contractions (買うて, じゃねえ, ならねえ, 始まんねえ...) are NOT mistakes: explain them in the notes, never correct them."""


# Version B of llm\test_context.py: readings + word types + dictionary forms, no meanings
# (adding dictionary meanings anchored the model on wrong senses).
SYSTEM_WITH_ANALYSIS = SYSTEM + """
- You also get a word analysis from a dictionary tool. Use it to see word boundaries and dictionary forms,
  but it can be wrong (especially readings of words like 明日, 今日, 一日 and names). If a reading in the
  analysis does not fit this context, say so in a note, giving the right reading in kana (the only time you write a reading)."""

SKIP_POS = ("補助記号", "記号", "空白")


def analysis_block(tokens):
    """tokens: the 'tokens' list from reader_core.analyze()."""
    rows = []
    for t in tokens:
        if t["pos"] in SKIP_POS:
            continue
        row = f"- {t['surface']}"
        if t["has_kanji"]:
            row += f" [{t['reading']}]"
        row += f" ({t['pos_en']}"
        if t["dictionary_form"] and t["dictionary_form"] != t["surface"]:
            row += f", dictionary form {t['dictionary_form']}"
        rows.append(row + ")")
    return "Word analysis (from a dictionary tool, may contain mistakes):\n" + "\n".join(rows)


def user_message(line, context=()):
    parts = []
    if context:
        parts.append("Previous bubbles (oldest first):")
        parts += [f"{i + 1}. {c}" for i, c in enumerate(context)]
        parts.append("")
    parts.append("Current bubble:")
    parts.append(line)
    return "\n".join(parts)
