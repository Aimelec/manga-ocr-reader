package com.mangareader.local.logic

import com.mangareader.local.logic.AutoReadDetector.Decision
import org.junit.Assert.assertEquals
import org.junit.Test

class AutoReadDetectorTest {

    private val detector = AutoReadDetector(intervalMs = 100, stillMs = 500, stillDiff = 4f, changedDiff = 10f)

    private fun grid(v: Float) = FloatArray(16) { v }

    private fun feed(from: Long, to: Long, step: Long = 100, value: (Long) -> Float) =
        (from..to step step).map { t -> detector.onFrame(t) { grid(value(t)) } }

    @Test
    fun `frames closer than the interval are skipped`() {
        detector.onFrame(1000) { grid(0f) }
        assertEquals(Decision.Skip, detector.onFrame(1050) { grid(0f) })
    }

    @Test
    fun `reads after the picture stays still long enough`() {
        val decisions = feed(1000, 1700) { 50f }
        assertEquals(Decision.Moving, decisions.first())           // nothing to compare with yet
        assertEquals(Decision.Read, decisions.last())
        assertEquals(1, decisions.count { it == Decision.Read })
    }

    @Test
    fun `movement restarts the wait`() {
        feed(1000, 1400) { 50f }
        assertEquals(Decision.Moving, detector.onFrame(1500) { grid(90f) })     // the hand moved
        val after = feed(1600, 2000) { 90f }
        assertEquals(listOf(Decision.Waiting), after.distinct())              // 500 ms not reached again yet
    }

    @Test
    fun `small noise still counts as still`() {
        val decisions = feed(1000, 1800) { t -> if (t % 200 == 0L) 50f else 52f }
        assertEquals(Decision.Read, decisions.last { it != Decision.Waiting })
    }

    @Test
    fun `the same bubble is not read twice, a different one is`() {
        feed(1000, 1700) { 50f }
        assertEquals(listOf(Decision.Waiting), feed(1800, 3000) { 50f }.distinct())
        assertEquals(Decision.Moving, detector.onFrame(3100) { grid(120f) })   // a new bubble comes into view
        assertEquals(1, feed(3200, 3900) { 120f }.count { it == Decision.Read })
    }

    @Test
    fun `after forgetLastRead the same bubble can be read again`() {
        feed(1000, 1700) { 50f }
        detector.forgetLastRead()
        assertEquals(1, feed(1800, 2500) { 50f }.count { it == Decision.Read })
    }

    @Test
    fun `diff is the mean absolute difference`() {
        assertEquals(2f, AutoReadDetector.diff(floatArrayOf(0f, 4f), floatArrayOf(2f, 2f)), 0f)
    }
}
