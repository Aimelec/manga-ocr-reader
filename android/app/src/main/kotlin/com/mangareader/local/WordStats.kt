package com.mangareader.local

import com.mangareader.local.model.*
import android.content.Context
import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Statistics behind the automatic "Frequent" dictionary, kept on the phone. */
class WordStats(context: Context, private val scope: CoroutineScope) {

    private val file = File(context.filesDir, "word_stats.json")
    val stats = mutableStateMapOf<String, WordStat>()

    // sentences already counted, so rereading a bubble does not count its words again
    private val sentences = LinkedHashSet<String>()
    // (sentence, word) pairs already counted as looked up during this run
    private val lookedNow = HashSet<String>()
    private var saveJob: Job? = null

    init {
        runCatching {
            if (file.exists()) {
                val o = JSONObject(file.readText())
                val words = o.optJSONArray("words") ?: JSONArray()
                for (i in 0 until words.length()) {
                    val w = words.getJSONObject(i)
                    val saved = savedWordFromJson(w.getJSONObject("word"))
                    stats[saved.key] = WordStat(saved, w.optInt("seen"), w.optInt("looked"), w.optLong("lastSeen"),
                        w.optString("example"), w.optString("exampleSurface"), w.optBoolean("known"))
                }
                val s = o.optJSONArray("sentences") ?: JSONArray()
                for (i in 0 until s.length()) sentences.add(s.getString(i))
            }
        }
    }

    fun get(key: String) = stats[key]

    /** A bubble was read: every word in it counts as seen once (if this sentence is new). */
    fun recordSeen(r: ReadResult) {
        val sentence = r.text.trim()
        if (sentence.isEmpty() || !sentences.add(sentence)) return
        while (sentences.size > MAX_SENTENCES) sentences.remove(sentences.first())
        val now = System.currentTimeMillis()
        r.tokens.filter { !it.isPunctuation }.distinctBy { wordKey(it) }.forEach { t ->
            val k = wordKey(t)
            val old = stats[k]
            stats[k] = WordStat(
                word = old?.word ?: savedWordOf(t),
                seen = (old?.seen ?: 0) + 1,
                looked = old?.looked ?: 0,
                lastSeen = now,
                example = sentence,
                exampleSurface = t.surface,
                known = old?.known ?: false,
            )
        }
        scheduleSave()
    }

    /** The word was tapped in the sentence or its card was opened. Once per word per sentence. */
    fun recordLookup(r: ReadResult, t: Token) {
        if (t.isPunctuation) return
        val k = wordKey(t)
        if (!lookedNow.add(r.text.trim() + "|" + k)) return
        val old = stats[k] ?: WordStat(savedWordOf(t), 1, 0, System.currentTimeMillis(), r.text.trim(), t.surface, false)
        stats[k] = old.copy(looked = old.looked + 1)
        scheduleSave()
    }

    fun setKnown(key: String, known: Boolean) {
        stats[key]?.let { stats[key] = it.copy(known = known) }
        scheduleSave()
    }

    /** Forget all counts, example sentences, known marks and the list of sentences already counted. */
    fun clear() {
        stats.clear()
        sentences.clear()
        lookedNow.clear()
        scheduleSave()
    }

    /** Best first: score, then most recently seen. */
    fun inCategory(c: String, known: Boolean) = stats.values
        .filter { it.word.category == c && it.known == known }
        .sortedWith(compareByDescending<WordStat> { it.score }.thenByDescending { it.lastSeen })

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(1000)
            val words = stats.values.toList()
            val sent = sentences.toList()
            withContext(Dispatchers.IO) {
                val o = JSONObject()
                o.put("words", JSONArray().apply {
                    words.forEach { s ->
                        put(JSONObject().put("word", savedWordToJson(s.word)).put("seen", s.seen)
                            .put("looked", s.looked).put("lastSeen", s.lastSeen).put("example", s.example)
                            .put("exampleSurface", s.exampleSurface).put("known", s.known))
                    }
                })
                o.put("sentences", JSONArray().apply { sent.forEach { put(it) } })
                writeAtomically(file, o.toString())
            }
        }
    }

    companion object {
        private const val MAX_SENTENCES = 5000
    }
}
