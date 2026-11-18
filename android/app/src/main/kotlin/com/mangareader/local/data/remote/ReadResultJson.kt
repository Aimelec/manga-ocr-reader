package com.mangareader.local.data.remote

import com.mangareader.local.model.Entry
import com.mangareader.local.model.GrammarHit
import com.mangareader.local.model.KanjiInfo
import com.mangareader.local.model.Part
import com.mangareader.local.model.ReadResult
import com.mangareader.local.model.Sense
import com.mangareader.local.model.Token
import org.json.JSONArray
import org.json.JSONObject

// JSON of the server's /api/v1/read response -> model classes (contract: contract/openapi.json).

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

fun parseReadResult(o: JSONObject) = ReadResult(
    text = o.getString("text"),
    hiragana = o.optString("hiragana"),
    tokens = o.getJSONArray("tokens").objects(::parseToken),
    grammar = o.optJSONArray("grammar").objects {
        GrammarHit(it.getString("name"), it.optString("match"), it.optString("explanation"))
    },
)
