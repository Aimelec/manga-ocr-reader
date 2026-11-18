package com.mangareader.local

import com.mangareader.local.model.*
import androidx.compose.runtime.mutableStateListOf
import com.mangareader.local.logic.FavouritesStorage

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
class WordStore(private val storage: FavouritesStorage) {

    val words = mutableStateListOf<SavedWord>().apply { addAll(storage.load()) }

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

    private fun save() = storage.save(words.toList())
}
