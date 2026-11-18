package com.mangareader.local.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExplanationParserTest {

    @Test
    fun `splits the answer into its sections`() {
        val e = parseExplanation("""
            TRANSLATION: No need for thanks.
            LITERAL: Thanks does not reach.
            NOTES:
            - 「礼には及ばぬ」 is a set phrase.
            * 「〜ぬ」 is the archaic negative.
            OCR: ok
        """.trimIndent())
        assertEquals("No need for thanks.", e.translation)
        assertEquals("Thanks does not reach.", e.literal)
        assertEquals(listOf("「礼には及ばぬ」 is a set phrase.", "「〜ぬ」 is the archaic negative."), e.notes)
        assertEquals("ok", e.ocr)
    }

    @Test
    fun `markdown bold and multi-line sections are handled`() {
        val e = parseExplanation("**TRANSLATION:** Are you\na traveler?\nNOTES: first note\n• second note")
        assertEquals("Are you a traveler?", e.translation)
        assertEquals(listOf("first note", "second note"), e.notes)
    }

    @Test
    fun `a half-written answer gives what is there so far`() {
        val e = parseExplanation("TRANSLATION: Don't underestimate me.\nLITERAL: Me (obj")
        assertEquals("Don't underestimate me.", e.translation)
        assertEquals("Me (obj", e.literal)
    }

    @Test
    fun `an answer without sections is shown as the translation`() {
        assertEquals("Just some text", parseExplanation("  Just some text ").translation)
    }

    @Test
    fun `OCR correction only when the AI read something else`() {
        assertNull(suggestedCorrection(parseExplanation("OCR: ok"), "本だ"))
        assertNull(suggestedCorrection(parseExplanation("OCR: 「本だ」"), "本だ"))
        assertEquals("間違って", suggestedCorrection(parseExplanation("OCR: <間違って>"), "間遺って"))
    }
}
