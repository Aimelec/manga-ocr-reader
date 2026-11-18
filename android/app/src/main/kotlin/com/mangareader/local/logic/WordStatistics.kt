package com.mangareader.local.logic

import androidx.compose.runtime.mutableStateMapOf
import com.mangareader.local.model.ReadResult
import com.mangareader.local.model.Token
import com.mangareader.local.model.WordStat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Statistics behind the automatic "Frequent" dictionary, kept on the phone. Saves are batched. */
class WordStatistics(
    private val storage: StatsStorage,
    private val scope: CoroutineScope,
    private val now: () -> Long = System::currentTimeMillis,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val saveDelayMs: Long = 1000,
) {
    val stats = mutableStateMapOf<String, WordStat>()

    // sentences already counted, so rereading a bubble does not count its words again
    private val sentences = LinkedHashSet<String>()
    // (sentence, word) pairs already counted as looked up during this run
    private val lookedNow = HashSet<String>()
    private var saveJob: Job? = null

    init {
        val stored = storage.load()
        stored.words.forEach { stats[it.word.key] = it }
        sentences.addAll(stored.sentences)
    }

    fun get(key: String) = stats[key]

    /** A bubble was read: every word in it counts as seen once (if this sentence is new). */
    fun recordSeen(r: ReadResult) {
        val sentence = r.text.trim()
        if (sentence.isEmpty() || !sentences.add(sentence)) return
        while (sentences.size > MAX_SENTENCES) sentences.remove(sentences.first())
        val t0 = now()
        r.tokens.filter { !it.isPunctuation }.distinctBy { wordKey(it) }.forEach { t ->
            val k = wordKey(t)
            val old = stats[k]
            stats[k] = WordStat(
                word = old?.word ?: savedWordOf(t, t0),
                seen = (old?.seen ?: 0) + 1,
                looked = old?.looked ?: 0,
                lastSeen = t0,
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
        val old = stats[k] ?: WordStat(savedWordOf(t, now()), 1, 0, now(), r.text.trim(), t.surface, false)
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
            delay(saveDelayMs)
            val snapshot = StoredStats(stats.values.toList(), sentences.toList())
            withContext(io) { storage.save(snapshot) }
        }
    }

    companion object {
        private const val MAX_SENTENCES = 5000
    }
}
