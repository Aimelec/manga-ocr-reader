package com.mangareader.local.data

import androidx.compose.ui.geometry.Rect
import com.mangareader.local.Fixtures.entry
import com.mangareader.local.Fixtures.token
import com.mangareader.local.data.camera.ImageTools
import com.mangareader.local.data.local.FavouritesFile
import com.mangareader.local.data.local.StatsFile
import com.mangareader.local.data.remote.parseReadResult
import com.mangareader.local.logic.StoredStats
import com.mangareader.local.logic.savedWordOf
import com.mangareader.local.model.KanjiInfo
import com.mangareader.local.model.WordStat
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/** The app's parser against the server's real response (contract/read_response.example.json). */
class ReadResultContractTest {

    private val example = javaClass.classLoader!!.getResource("read_response.example.json")!!.readText()
    private val result = parseReadResult(JSONObject(example))

    @Test
    fun `parses the server's example response`() {
        assertEquals("明日の朝、駅の前で待っている。", result.text)
        assertEquals("あすのあさ、えきのまえでまっている。", result.hiragana)
        assertEquals(result.text, result.tokens.joinToString("") { it.surface })
    }

    @Test
    fun `ruby pairs put furigana only over kanji`() {
        val matte = result.tokens.first { it.surface == "待っ" }
        assertEquals(listOf("待" to "ま", "っ" to ""), matte.ruby)
        assertEquals("待つ", matte.dictionaryForm)
    }

    @Test
    fun `dictionary entries, kanji and grammar come through`() {
        val eki = result.tokens.first { it.surface == "駅" }
        assertTrue(eki.entries.first().senses.first().gloss.isNotEmpty())
        assertEquals("駅", eki.kanji.single().char)
        assertTrue(result.grammar.any { it.name.contains("ている") })
        assertTrue(result.tokens.first { it.surface == "の" }.function.isNotEmpty())
    }

    @Test
    fun `punctuation is recognised`() {
        assertTrue(result.tokens.first { it.surface == "。" }.isPunctuation)
    }
}

class WordFilesTest {

    private val dir: File = Files.createTempDirectory("mangareader").toFile()

    private val word = savedWordOf(
        token("駅", reading = "えき", entries = listOf(entry("駅", "station", kana = "えき"))), now = 7,
    ).copy(kanji = listOf(KanjiInfo("駅", listOf("station"), listOf("エキ"), emptyList())))

    @Test
    fun `favourites survive a save and load`() {
        FavouritesFile(dir).save(listOf(word))
        assertEquals(listOf(word), FavouritesFile(dir).load())
    }

    @Test
    fun `statistics survive a save and load`() {
        val stored = StoredStats(listOf(WordStat(word, 3, 1, 99, "駅の前", "駅", true)), listOf("駅の前"))
        StatsFile(dir).save(stored)
        assertEquals(stored, StatsFile(dir).load())
    }

    @Test
    fun `missing or broken files load as empty`() {
        assertTrue(FavouritesFile(dir).load().isEmpty())
        File(dir, "word_stats.json").writeText("{not json")
        assertTrue(StatsFile(dir).load().words.isEmpty())
    }
}

class ImageToolsTest {

    private val box = Rect(0.1f, 0.2f, 0.4f, 0.9f)

    @Test
    fun `upright photo keeps the box`() {
        assertEquals(box, ImageTools.toSensor(box, 0))
    }

    @Test
    fun `rotated sensor images map the box back`() {
        // the phone held upright: the sensor image is rotated 90 degrees clockwise to look right
        assertRect(Rect(0.2f, 0.6f, 0.9f, 0.9f), ImageTools.toSensor(box, 90))
        assertRect(Rect(0.6f, 0.1f, 0.9f, 0.8f), ImageTools.toSensor(box, 180))
        assertRect(Rect(0.1f, 0.1f, 0.8f, 0.4f), ImageTools.toSensor(box, 270))
    }

    private fun assertRect(expected: Rect, actual: Rect) {
        listOf(expected.left to actual.left, expected.top to actual.top, expected.right to actual.right,
            expected.bottom to actual.bottom).forEach { (e, a) -> assertEquals(e, a, 1e-6f) }
    }
}
