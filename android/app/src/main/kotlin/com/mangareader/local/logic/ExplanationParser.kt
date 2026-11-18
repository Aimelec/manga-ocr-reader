package com.mangareader.local.logic

/** The AI's answer, split into its sections. */
data class Explanation(val translation: String, val literal: String, val notes: List<String>, val ocr: String)

/**
 * Parses the TRANSLATION / LITERAL / NOTES / OCR format the prompt asks for. Works on a half-written
 * answer too (it is re-parsed while streaming). An answer without any section is shown as a translation.
 */
fun parseExplanation(text: String): Explanation {
    var translation = ""
    var literal = ""
    var ocr = ""
    val notes = mutableListOf<String>()
    var section = ""
    for (raw in text.lines()) {
        val l = raw.trim().replace("**", "")
        when {
            l.startsWith("TRANSLATION:", true) -> { section = "t"; translation = l.substringAfter(':').trim() }
            l.startsWith("LITERAL:", true) -> { section = "l"; literal = l.substringAfter(':').trim() }
            l.startsWith("NOTES:", true) -> { section = "n"; l.substringAfter(':').trim().takeIf { it.isNotEmpty() }?.let { notes += it } }
            l.startsWith("OCR:", true) -> { section = "o"; ocr = l.substringAfter(':').trim() }
            l.isEmpty() -> Unit
            section == "n" -> notes += l.removePrefix("- ").removePrefix("* ").removePrefix("• ")
            section == "t" -> translation += " $l"
            section == "l" -> literal += " $l"
        }
    }
    if (translation.isEmpty() && literal.isEmpty() && notes.isEmpty() && ocr.isEmpty()) translation = text.trim()
    return Explanation(translation, literal, notes, ocr)
}

/** The OCR correction the AI suggests, or null when it says the text was read correctly. */
fun suggestedCorrection(e: Explanation, readText: String?): String? {
    val ocr = e.ocr.trim('"', '<', '>', ' ', '「', '」')
    return ocr.takeIf { it.isNotEmpty() && !it.equals("ok", true) && it != readText }
}
