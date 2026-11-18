package com.mangareader.local.logic

import com.mangareader.local.model.SavedWord
import com.mangareader.local.model.Token

// How a word from a read sentence is filed in "My words": category, key and saved form.

/** Dictionary category of a token (names first, then by part of speech). */
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

private val INFLECTING = setOf("動詞", "形容詞", "助動詞")

/** Word + reading to file a token under: dictionary form for verbs, adjectives and endings. */
private fun baseForm(t: Token): Pair<String, String> {
    val useBase = t.pos in INFLECTING && t.dictionaryForm.isNotEmpty()
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

fun savedWordOf(t: Token, now: Long): SavedWord {
    val (word, reading) = baseForm(t)
    return SavedWord(wordKey(t), word, reading, categoryOf(t), t.posEn, t.shortMeaning, t.entries, t.kanji, now)
}
