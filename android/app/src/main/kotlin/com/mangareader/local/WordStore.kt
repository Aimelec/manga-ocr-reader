package com.mangareader.local

import com.mangareader.local.model.*
import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

fun categoryOf(t: Token): String = when {
    t.posEn.contains("(name)") -> "Names"
    else -> when (t.pos) {
        "名詞" -> "Nouns"
        "動詞" -> "Verbs"
        "形容詞" -> "い-adjectives"
        "形状詞" -> "な-adjectives"
        "副詞" -> "Adverbs"
        "助詞" -> "Particles"
        "助動詞" -> "Endings"
        "代名詞" -> "Pronouns"
        "接続詞" -> "Conjunctions"
        "感動詞" -> "Interjections"
        "連体詞" -> "Pre-noun words"
        "接頭辞", "接尾辞" -> "Prefixes & suffixes"
        else -> "Other"
    }
}

/** Word + reading to file a token under: dictionary form for verbs, adjectives and endings. */
private fun baseForm(t: Token): Pair<String, String> {
    val useBase = t.pos in setOf("動詞", "形容詞", "助動詞") && t.dictionaryForm.isNotEmpty()
    val word = if (useBase) t.dictionaryForm else t.surface
    val reading = if (word == t.surface) t.reading
    else t.entries.firstOrNull { word in it.kanji || word in it.kana }?.kana?.firstOrNull() ?: ""
    return word to (if (reading == word) "" else reading)
}

/** Same key for every conjugation of a word (待っ, 待って, 待ちます -> 待つ). */
fun wordKey(t: Token): String {
    val (word, reading) = baseForm(t)
    return "${categoryOf(t)}|$word|$reading"
}

fun savedWordOf(t: Token): SavedWord {
    val (word, reading) = baseForm(t)
    return SavedWord(wordKey(t), word, reading, categoryOf(t), t.posEn, t.shortMeaning,
        t.entries, t.kanji, System.currentTimeMillis())
}

/** ★ Favourites, saved by hand. */
class WordStore(context: Context) {

    private val file = File(context.filesDir, "saved_words.json")
    val words = mutableStateListOf<SavedWord>()

    init {
        runCatching {
            if (file.exists()) {
                val arr = JSONArray(file.readText())
                for (i in 0 until arr.length()) words.add(savedWordFromJson(arr.getJSONObject(i)))
            }
        }
    }

    fun isSaved(t: Token) = isSaved(wordKey(t))
    fun isSaved(key: String) = words.any { it.key == key }

    fun toggle(t: Token) = toggle(savedWordOf(t))

    fun toggle(w: SavedWord) {
        if (!words.removeAll { it.key == w.key }) words.add(0, w.copy(savedAt = System.currentTimeMillis()))
        save()
    }

    fun inCategory(c: String) = words.filter { it.category == c }

    /** Remove every favourite. */
    fun clear() {
        words.clear()
        save()
    }

    private fun save() {
        val arr = JSONArray()
        words.forEach { arr.put(savedWordToJson(it)) }
        writeAtomically(file, arr.toString())
    }
}

// ---------------- shared JSON helpers ----------------

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
