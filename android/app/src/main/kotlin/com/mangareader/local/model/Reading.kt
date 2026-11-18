package com.mangareader.local.model

/** What the PC server's /read endpoint returns (see contract/openapi.json). */
data class ReadResult(
    val text: String,
    val hiragana: String,
    val tokens: List<Token>,
    val grammar: List<GrammarHit>,
)

data class Token(
    val surface: String,
    val reading: String,
    val dictionaryForm: String,
    val pos: String,
    val posEn: String,
    val formEn: String,
    val ruby: List<Pair<String, String>>,   // (text, furigana or "")
    val function: String,                   // explanation for particles / auxiliaries
    val entries: List<Entry>,
    val parts: List<Part>,
    val kanji: List<KanjiInfo>,
) {
    val isPunctuation get() = pos in setOf("補助記号", "記号", "空白")

    val label: String
        get() = posEn + (if (formEn.isNotEmpty()) ", $formEn" else "") +
            (if (dictionaryForm.isNotEmpty() && dictionaryForm != surface) " · $dictionaryForm" else "")

    val shortMeaning: String
        get() = function.ifEmpty {
            entries.firstOrNull()?.senses?.firstOrNull()?.gloss
                ?: parts.joinToString(" + ") { p -> "${p.surface}: ${p.entries.firstOrNull()?.senses?.firstOrNull()?.gloss ?: "?"}" }
                    .ifEmpty { "—" }
        }

    val hasDetails get() = entries.isNotEmpty() || parts.isNotEmpty() || kanji.isNotEmpty()
}

data class Entry(val kanji: List<String>, val kana: List<String>, val senses: List<Sense>)
data class Sense(val gloss: String, val pos: String)
data class Part(val surface: String, val reading: String, val entries: List<Entry>)
data class KanjiInfo(val char: String, val meanings: List<String>, val on: List<String>, val kun: List<String>)
data class GrammarHit(val name: String, val match: String, val explanation: String)
