package com.mangareader.local.data.local

import com.mangareader.local.logic.FavouritesStorage
import com.mangareader.local.logic.StatsStorage
import com.mangareader.local.logic.StoredStats
import com.mangareader.local.model.SavedWord
import com.mangareader.local.model.WordStat
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** ★ Favourites in `saved_words.json` (app-private storage). */
class FavouritesFile(dir: File) : FavouritesStorage {
    private val file = File(dir, "saved_words.json")

    override fun load(): List<SavedWord> = runCatching {
        if (!file.exists()) return emptyList()
        val arr = JSONArray(file.readText())
        List(arr.length()) { savedWordFromJson(arr.getJSONObject(it)) }
    }.getOrDefault(emptyList())

    override fun save(words: List<SavedWord>) {
        val arr = JSONArray()
        words.forEach { arr.put(savedWordToJson(it)) }
        writeAtomically(file, arr.toString())
    }
}

/** Frequent-words statistics in `word_stats.json` (app-private storage). */
class StatsFile(dir: File) : StatsStorage {
    private val file = File(dir, "word_stats.json")

    override fun load(): StoredStats = runCatching {
        if (!file.exists()) return StoredStats(emptyList(), emptyList())
        val o = JSONObject(file.readText())
        val words = o.optJSONArray("words") ?: JSONArray()
        val s = o.optJSONArray("sentences") ?: JSONArray()
        StoredStats(
            words = List(words.length()) { i ->
                val w = words.getJSONObject(i)
                WordStat(savedWordFromJson(w.getJSONObject("word")), w.optInt("seen"), w.optInt("looked"),
                    w.optLong("lastSeen"), w.optString("example"), w.optString("exampleSurface"), w.optBoolean("known"))
            },
            sentences = List(s.length()) { s.getString(it) },
        )
    }.getOrDefault(StoredStats(emptyList(), emptyList()))

    override fun save(stats: StoredStats) {
        val o = JSONObject()
        o.put("words", JSONArray().apply {
            stats.words.forEach { s ->
                put(JSONObject().put("word", savedWordToJson(s.word)).put("seen", s.seen)
                    .put("looked", s.looked).put("lastSeen", s.lastSeen).put("example", s.example)
                    .put("exampleSurface", s.exampleSurface).put("known", s.known))
            }
        })
        o.put("sentences", JSONArray().apply { stats.sentences.forEach { put(it) } })
        writeAtomically(file, o.toString())
    }
}
