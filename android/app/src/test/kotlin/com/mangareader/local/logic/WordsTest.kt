package com.mangareader.local.logic

import com.mangareader.local.Fixtures.entry
import com.mangareader.local.Fixtures.punctuation
import com.mangareader.local.Fixtures.result
import com.mangareader.local.Fixtures.token
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordRulesTest {

    @Test
    fun `conjugated verbs are filed under their dictionary form`() {
        val matte = token("待っ", pos = "動詞", reading = "まっ", dictionaryForm = "待つ", posEn = "verb",
            entries = listOf(entry("待つ", "to wait", kana = "まつ")))
        val machimasu = token("待ち", pos = "動詞", reading = "まち", dictionaryForm = "待つ", posEn = "verb",
            entries = listOf(entry("待つ", "to wait", kana = "まつ")))
        assertEquals("Verbs|待つ|まつ", wordKey(matte))
        assertEquals(wordKey(matte), wordKey(machimasu))
        val saved = savedWordOf(matte, now = 42)
        assertEquals("待つ" to "まつ", saved.word to saved.reading)
        assertEquals(42L, saved.savedAt)
    }

    @Test
    fun `nouns keep their surface and drop a reading equal to the word`() {
        assertEquals("Nouns|ねこ|", wordKey(token("ねこ")))
        assertEquals("Nouns|猫|ねこ", wordKey(token("猫", reading = "ねこ")))
    }

    @Test
    fun `categories follow the part of speech, names first`() {
        assertEquals("Names", categoryOf(token("田中", posEn = "noun (name)")))
        assertEquals("Particles", categoryOf(token("は", pos = "助詞")))
        assertEquals("Endings", categoryOf(token("ます", pos = "助動詞")))
        assertEquals("Prefixes & suffixes", categoryOf(token("さん", pos = "接尾辞")))
        assertEquals("Other", categoryOf(token("…", pos = "補助記号")))
    }
}

class FavouritesTest {

    private val storage = MemoryFavourites()
    private var now = 1L
    private val favourites = Favourites(storage, now = { now })

    @Test
    fun `toggle saves and removes, newest first, persisted each time`() {
        favourites.toggle(token("本"))
        now = 2
        favourites.toggle(token("猫", reading = "ねこ"))
        assertEquals(listOf("猫", "本"), favourites.words.map { it.word })
        assertTrue(favourites.isSaved(token("本")))
        favourites.toggle(token("本"))
        assertFalse(favourites.isSaved(token("本")))
        assertEquals(listOf("猫"), storage.saved.map { it.word })
        assertEquals(3, storage.saves)
    }

    @Test
    fun `loads what was saved before and filters by category`() {
        storage.saved = listOf(savedWordOf(token("本"), 1), savedWordOf(token("は", pos = "助詞"), 1))
        val loaded = Favourites(storage)
        assertEquals(listOf("は"), loaded.inCategory("Particles").map { it.word })
        loaded.clear()
        assertTrue(storage.saved.isEmpty())
    }
}

class WordStatisticsTest {

    private val storage = MemoryStats()
    private var now = 100L
    private val stats = WordStatistics(storage, CoroutineScope(Dispatchers.Unconfined), now = { now },
        io = Dispatchers.Unconfined, saveDelayMs = 0)

    @Test
    fun `each new sentence counts its words once, punctuation ignored`() {
        stats.recordSeen(result("本と本。", token("本"), token("と", pos = "助詞"), token("本"), punctuation))
        assertEquals(setOf("本" to 1, "と" to 1), stats.stats.values.map { it.word.word to it.seen }.toSet())
    }

    @Test
    fun `rereading the same sentence does not count again`() {
        repeat(3) { stats.recordSeen(result("本だ", token("本"))) }
        assertEquals(1, stats.stats.values.single().seen)
        stats.recordSeen(result("本がある", token("本")))
        assertEquals(2, stats.stats.values.single().seen)
        assertEquals("本がある", stats.stats.values.single().example)
    }

    @Test
    fun `a lookup counts once per word and sentence`() {
        val r = result("本だ", token("本"))
        stats.recordSeen(r)
        repeat(3) { stats.recordLookup(r, token("本")) }
        assertEquals(1, stats.stats.values.single().looked)
    }

    @Test
    fun `best first by score, then most recent, and known words are separate`() {
        stats.recordSeen(result("a", token("本"), token("猫", reading = "ねこ")))
        now = 200
        stats.recordSeen(result("b", token("猫", reading = "ねこ")))
        stats.recordLookup(result("c", token("犬", reading = "いぬ")), token("犬", reading = "いぬ"))
        assertEquals(listOf("犬", "猫", "本"), stats.inCategory("Nouns", known = false).map { it.word.word })
        val dogKey = stats.inCategory("Nouns", known = false).first().word.key
        stats.setKnown(dogKey, true)
        assertEquals(listOf("犬"), stats.inCategory("Nouns", known = true).map { it.word.word })
    }

    @Test
    fun `changes are saved and loaded back`() {
        stats.recordSeen(result("本だ", token("本")))
        assertEquals(listOf("本だ"), storage.saved.sentences)
        val reloaded = WordStatistics(storage, CoroutineScope(Dispatchers.Unconfined))
        assertEquals(1, reloaded.stats.values.single().seen)
        reloaded.recordSeen(result("本だ", token("本")))      // the sentence list was restored too
        assertEquals(1, reloaded.stats.values.single().seen)
    }

    @Test
    fun `clear forgets everything`() {
        stats.recordSeen(result("本だ", token("本")))
        stats.clear()
        assertTrue(stats.stats.isEmpty())
        assertTrue(storage.saved.words.isEmpty() && storage.saved.sentences.isEmpty())
        assertNull(stats.get("Nouns|本|"))
    }
}

class SettingsViewModelTest {

    private val server = FakeServer()
    private var changes = 0
    private val settings = SettingsViewModel(server) { changes++ }

    @Test
    fun `address is cleaned before saving`() {
        settings.save("  https://192.168.1.7:8765/ ")
        assertEquals("192.168.1.7:8765", server.address)
        assertEquals(1, changes)
    }

    @Test
    fun `blank address keeps the old one`() {
        settings.save("   ")
        assertEquals("192.168.1.100:8765", server.address)
    }

    @Test
    fun `trusting again forgets the pinned certificate`() {
        settings.trustCertificateAgain()
        assertTrue(server.certificateForgotten)
        assertEquals(1, changes)
    }
}
