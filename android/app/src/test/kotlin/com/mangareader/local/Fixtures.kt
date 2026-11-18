package com.mangareader.local

import com.mangareader.local.model.Entry
import com.mangareader.local.model.ReadResult
import com.mangareader.local.model.Sense
import com.mangareader.local.model.Token

/** Small hand-made model objects for tests. */
object Fixtures {
    fun token(
        surface: String,
        pos: String = "名詞",
        reading: String = surface,
        dictionaryForm: String = surface,
        posEn: String = "noun",
        function: String = "",
        entries: List<Entry> = listOf(entry(surface, "meaning of $surface")),
    ) = Token(surface, reading, dictionaryForm, pos, posEn, "", listOf(surface to ""), function, entries,
        emptyList(), emptyList())

    fun entry(word: String, gloss: String, kana: String = "") =
        Entry(listOf(word), if (kana.isEmpty()) emptyList() else listOf(kana), listOf(Sense(gloss, "noun")))

    fun result(text: String, vararg tokens: Token) = ReadResult(text, text, tokens.toList(), emptyList())

    val punctuation = token("。", pos = "補助記号", posEn = "punctuation", entries = emptyList())
}
