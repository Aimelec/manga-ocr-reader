package com.mangareader.local.model

/** A word as kept on the phone (favourites and statistics; works without the PC). */
data class SavedWord(
    val key: String,
    val word: String,          // dictionary form for verbs / adjectives / endings
    val reading: String,
    val category: String,
    val label: String,
    val meaning: String,
    val entries: List<Entry>,
    val kanji: List<KanjiInfo>,
    val savedAt: Long,
)

/** Category names in display order. */
val CATEGORIES = listOf(
    "Nouns", "Verbs", "い-adjectives", "な-adjectives", "Adverbs", "Particles", "Endings",
    "Pronouns", "Names", "Conjunctions", "Interjections", "Pre-noun words", "Prefixes & suffixes", "Other",
)

/** How often a word came up: seen in a read bubble, and looked up (tapped / card opened). */
data class WordStat(
    val word: SavedWord,
    val seen: Int,
    val looked: Int,
    val lastSeen: Long,
    val example: String,          // last sentence the word appeared in
    val exampleSurface: String,   // the word as written in that sentence (may be conjugated)
    val known: Boolean,
) {
    /** Looked-up counts three times as much as seen. */
    val score get() = looked * 3 + seen
}
