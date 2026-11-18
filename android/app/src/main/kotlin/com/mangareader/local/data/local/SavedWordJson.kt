package com.mangareader.local.data.local

import com.mangareader.local.model.Entry
import com.mangareader.local.model.KanjiInfo
import com.mangareader.local.model.SavedWord
import com.mangareader.local.model.Sense
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

// JSON format of the files the app keeps on the phone (favourites and statistics).

fun writeAtomically(file: File, text: String) {
    val tmp = File(file.parentFile, file.name + ".tmp")
    tmp.writeText(text)
    if (!tmp.renameTo(file)) {
        file.delete()
        tmp.renameTo(file)
    }
}

private fun strings(l: List<String>) = JSONArray().apply { l.forEach { put(it) } }
private fun JSONArray?.toStrings() = if (this == null) emptyList() else List(length()) { getString(it) }

fun savedWordToJson(w: SavedWord): JSONObject = JSONObject().apply {
    put("key", w.key); put("word", w.word); put("reading", w.reading); put("category", w.category)
    put("label", w.label); put("meaning", w.meaning); put("savedAt", w.savedAt)
    put("entries", JSONArray().apply {
        w.entries.forEach { e ->
            put(JSONObject().apply {
                put("kanji", strings(e.kanji)); put("kana", strings(e.kana))
                put("senses", JSONArray().apply {
                    e.senses.forEach { s -> put(JSONObject().put("gloss", s.gloss).put("pos", s.pos)) }
                })
            })
        }
    })
    put("kanjiInfo", JSONArray().apply {
        w.kanji.forEach { k ->
            put(JSONObject().put("char", k.char).put("meanings", strings(k.meanings))
                .put("on", strings(k.on)).put("kun", strings(k.kun)))
        }
    })
}

fun savedWordFromJson(o: JSONObject): SavedWord {
    val entries = o.optJSONArray("entries")
    val kanji = o.optJSONArray("kanjiInfo")
    return SavedWord(
        key = o.getString("key"), word = o.getString("word"), reading = o.optString("reading"),
        category = o.optString("category", "Other"), label = o.optString("label"),
        meaning = o.optString("meaning"), savedAt = o.optLong("savedAt"),
        entries = if (entries == null) emptyList() else List(entries.length()) { i ->
            val e = entries.getJSONObject(i)
            val senses = e.optJSONArray("senses")
            Entry(e.optJSONArray("kanji").toStrings(), e.optJSONArray("kana").toStrings(),
                if (senses == null) emptyList() else List(senses.length()) { j ->
                    senses.getJSONObject(j).let { Sense(it.optString("gloss"), it.optString("pos")) }
                })
        },
        kanji = if (kanji == null) emptyList() else List(kanji.length()) { i ->
            kanji.getJSONObject(i).let {
                KanjiInfo(it.getString("char"), it.optJSONArray("meanings").toStrings(),
                    it.optJSONArray("on").toStrings(), it.optJSONArray("kun").toStrings())
            }
        },
    )
}
