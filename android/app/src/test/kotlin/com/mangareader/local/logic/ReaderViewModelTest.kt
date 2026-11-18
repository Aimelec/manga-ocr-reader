package com.mangareader.local.logic

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.mangareader.local.Fixtures.result
import com.mangareader.local.Fixtures.token
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * The fakes never really suspend, so on an Unconfined scope every action runs to the end
 * before the next line of the test: no test dispatcher or sleeping needed.
 */
class ReaderViewModelTest {

    private val camera = FakeCamera()
    private val server = FakeServer()
    private val cropper = FakeCropper()
    private var now = 0L
    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val stats = WordStatistics(MemoryStats(), scope, now = { now }, io = Dispatchers.Unconfined, saveDelayMs = 0)
    private val vm = ReaderViewModel(scope, camera, server, cropper, stats, clock = { now },
        work = Dispatchers.Unconfined, doneDisplayMs = 0, boxDebounceMs = 0)

    private fun readReturning(text: String) {
        server.results += result(text, token(text))
        vm.read()
    }

    // ---------------- reading ----------------

    @Test
    fun `live read focuses on the box, crops the photo and shows the result`() {
        vm.box = Rect(0.25f, 0.25f, 0.75f, 0.5f)
        readReturning("本")
        assertEquals(listOf(0.5f to 0.375f), camera.focusedAt)     // the center of the box
        assertEquals(listOf(Rect(0.25f, 0.25f, 0.75f, 0.5f)), cropper.boxes)
        assertArrayEquals(byteArrayOf(1, 2, 3, 90), server.reads.single())
        assertEquals("本", vm.result?.text)
        assertNull(vm.error)
        assertEquals(ReaderViewModel.Phase.Idle, vm.phase)
        assertEquals("Done", vm.status)
    }

    @Test
    fun `read words are counted in the statistics`() {
        readReturning("本")
        assertEquals(1, stats.stats.values.single().seen)
    }

    @Test
    fun `a server error is shown and the reader is ready again`() {
        server.readError = IOException("The PC answered with error 500")
        vm.read()
        assertEquals("The PC answered with error 500", vm.error)
        assertEquals("Error: The PC answered with error 500", vm.status)
        assertEquals(ReaderViewModel.Phase.Idle, vm.phase)
        assertNull(vm.result)
    }

    @Test
    fun `a successful read clears the previous error`() {
        server.readError = IOException("offline")
        vm.read()
        server.readError = null
        readReturning("本")
        assertNull(vm.error)
    }

    // ---------------- AI explanation ----------------

    @Test
    fun `explanation streams in piece by piece`() {
        readReturning("本")
        server.explainPieces = listOf("TRANSLATION: ", "a book")
        vm.explain()
        assertEquals("TRANSLATION: a book", vm.explanation)
        assertEquals(ReaderViewModel.ExplainState.Done, vm.explainState)
    }

    @Test
    fun `the previous bubbles are sent as context, oldest first, at most three`() {
        listOf("一", "二", "三", "四", "五").forEach { readReturning(it) }
        vm.explain()
        assertEquals("五" to listOf("二", "三", "四"), server.explains.single())
    }

    @Test
    fun `rereading the same bubble does not repeat it in the context`() {
        readReturning("一")
        readReturning("二")
        readReturning("二")
        vm.explain()
        assertEquals(listOf("一"), server.explains.single().second)
    }

    @Test
    fun `explain does nothing before anything was read`() {
        vm.explain()
        assertTrue(server.explains.isEmpty())
        assertEquals(ReaderViewModel.ExplainState.Idle, vm.explainState)
    }

    @Test
    fun `an error before the answer starts is shown as a failure`() {
        readReturning("本")
        server.explainError = IOException("The AI model is not available")
        vm.explain()
        assertEquals(ReaderViewModel.ExplainState.Failed, vm.explainState)
        assertEquals("The AI model is not available", vm.explanation)
    }

    @Test
    fun `an error marker inside the stream is shown as a failure`() {
        readReturning("本")
        server.explainPieces = listOf("TRANSLATION: half", "\n\nERROR: model crashed")
        vm.explain()
        assertEquals(ReaderViewModel.ExplainState.Failed, vm.explainState)
        assertEquals("model crashed", vm.explainError)
    }

    @Test
    fun `a new result resets the explanation`() {
        readReturning("一")
        vm.explain()
        readReturning("二")
        assertEquals("", vm.explanation)
        assertEquals(ReaderViewModel.ExplainState.Idle, vm.explainState)
    }

    // ---------------- buttons, gestures, server check ----------------

    @Test
    fun `server check reports whether the PC answers`() {
        vm.checkServer()
        assertTrue(vm.status.startsWith("Connected"))
        server.reachable = false
        vm.checkServer()
        assertTrue(vm.status.startsWith("Can't reach the PC (192.168.1.100:8765)"))
    }

    @Test
    fun `tap focuses the live camera and moves the focus ring`() {
        vm.onTap(Offset(0.25f, 0.75f))
        assertEquals(listOf(0.25f to 0.75f), camera.focusedAt)
        assertEquals(Offset(0.25f, 0.75f), vm.focusPoint)
        assertEquals(1, vm.focusTick)
    }

    @Test
    fun `torch toggles the camera light`() {
        vm.toggleTorch()
        assertTrue(vm.torch && camera.torchOn)
        vm.toggleTorch()
        assertFalse(vm.torch || camera.torchOn)
    }

    @Test
    fun `freeze without a camera picture keeps the live view`() {
        vm.toggleFreeze()
        assertNull(vm.frozen)
        assertEquals("Camera not ready yet", vm.status)
    }

    @Test
    fun `a word lookup is recorded for the current sentence`() {
        val t = token("本")
        server.results += result("本", t)
        vm.read()
        vm.lookedUp(t)
        assertEquals(1, stats.stats.values.single().looked)
    }

    // ---------------- Auto mode ----------------

    @Test
    fun `auto reads once the picture has been still for a moment`() {
        vm.startCamera()
        vm.toggleAuto()
        server.results += result("本", token("本"))
        for (t in 0L..1600L step 200) {
            now = t
            camera.frame(100f)
        }
        assertEquals(1, server.reads.size)
        assertEquals("本", vm.result?.text)
    }

    @Test
    fun `auto does not read while auto is off`() {
        vm.startCamera()
        for (t in 0L..2000L step 200) {
            now = t
            camera.frame(100f)
        }
        assertTrue(server.reads.isEmpty())
    }

    @Test
    fun `auto does not read the same picture twice`() {
        vm.startCamera()
        vm.toggleAuto()
        server.results += result("本", token("本"))
        for (t in 0L..3000L step 200) {
            now = t
            camera.frame(100f)
        }
        assertEquals(1, server.reads.size)
    }
}
