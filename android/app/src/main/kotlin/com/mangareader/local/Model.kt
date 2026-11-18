package com.mangareader.local

import org.json.JSONArray
import org.json.JSONObject

/** What the PC server's /read endpoint returns (see reader_core.py). */
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

private fun <T> JSONArray?.objects(f: (JSONObject) -> T): List<T> =
    if (this == null) emptyList() else List(length()) { f(getJSONObject(it)) }

private fun JSONArray?.strings(): List<String> =
    if (this == null) emptyList() else List(length()) { getString(it) }

private fun parseEntry(o: JSONObject) = Entry(
    kanji = o.optJSONArray("kanji").strings(),
    kana = o.optJSONArray("kana").strings(),
    senses = o.optJSONArray("senses").objects { Sense(it.optString("gloss"), it.optString("pos")) },
)

private fun parseToken(o: JSONObject): Token {
    val surface = o.getString("surface")
    val ruby = o.optJSONArray("ruby")
    return Token(
        surface = surface,
        reading = o.optString("reading"),
        dictionaryForm = o.optString("dictionary_form"),
        pos = o.optString("pos"),
        posEn = o.optString("pos_en"),
        formEn = o.optString("form_en"),
        ruby = if (ruby == null) listOf(surface to "")
        else List(ruby.length()) { ruby.getJSONArray(it).let { p -> p.getString(0) to p.getString(1) } },
        function = o.optString("function"),
        entries = o.optJSONArray("entries").objects(::parseEntry),
        parts = o.optJSONArray("parts").objects {
            Part(it.getString("surface"), it.optString("reading"), it.optJSONArray("entries").objects(::parseEntry))
        },
        kanji = o.optJSONArray("kanji").objects {
            KanjiInfo(it.getString("char"), it.optJSONArray("meanings").strings(),
                it.optJSONArray("on").strings(), it.optJSONArray("kun").strings())
        },
    )
}

fun parseResult(o: JSONObject) = ReadResult(
    text = o.getString("text"),
    hiragana = o.optString("hiragana"),
    tokens = o.getJSONArray("tokens").objects(::parseToken),
    grammar = o.optJSONArray("grammar").objects {
        GrammarHit(it.getString("name"), it.optString("match"), it.optString("explanation"))
    },
)
