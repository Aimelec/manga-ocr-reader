package com.mangareader.local.logic

import androidx.compose.runtime.mutableStateListOf
import com.mangareader.local.model.SavedWord
import com.mangareader.local.model.Token

/** ★ Favourites, saved by hand. Newest first; saved through [storage] after every change. */
class Favourites(private val storage: FavouritesStorage, private val now: () -> Long = System::currentTimeMillis) {

    val words = mutableStateListOf<SavedWord>().apply { addAll(storage.load()) }

    fun isSaved(t: Token) = isSaved(wordKey(t))
    fun isSaved(key: String) = words.any { it.key == key }

    fun toggle(t: Token) = toggle(savedWordOf(t, now()))

    fun toggle(w: SavedWord) {
        if (!words.removeAll { it.key == w.key }) words.add(0, w.copy(savedAt = now()))
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
