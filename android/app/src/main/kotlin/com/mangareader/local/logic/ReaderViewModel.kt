package com.mangareader.local.logic

import android.os.SystemClock
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import com.mangareader.local.model.ReadResult
import com.mangareader.local.model.Token
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * State and actions of the reader screen: the box, Freeze, Auto, reading a bubble through the PC,
 * and the on-demand AI explanation. The screen only shows this state and calls these functions.
 *
 * A plain state holder (Compose state + an injected scope), so unit tests run it with fakes.
 */
class ReaderViewModel(
    private val scope: CoroutineScope,
    private val camera: CameraDevice,
    private val server: ReadingServer,
    private val cropper: PhotoCropper,
    private val stats: WordStatistics,
    private val autoRead: AutoReadDetector = AutoReadDetector(),
    private val clock: () -> Long = SystemClock::uptimeMillis,
    private val work: CoroutineDispatcher = Dispatchers.Default,
    private val doneDisplayMs: Long = 900,
    private val boxDebounceMs: Long = 200,
) {
    enum class Phase { Idle, Busy, Done }
    enum class ExplainState { Idle, Waiting, Writing, Done, Failed }

    // ---------------- state shown on screen ----------------

    /** Reading box as fractions (0..1) of the camera image. */
    var box by mutableStateOf(Rect(0.3f, 0.15f, 0.7f, 0.85f))
    /** False right after freezing, until a box is dragged. */
    var boxVisible by mutableStateOf(true)
        private set
    var phase by mutableStateOf(Phase.Idle)
        private set
    var status by mutableStateOf("Drag on the camera to set the box, tap to focus")
        private set
    var auto by mutableStateOf(false)
        private set
    var torch by mutableStateOf(false)
        private set
    /** The frozen picture shown instead of the live camera, or null when live. */
    var frozen by mutableStateOf<ImageBitmap?>(null)
        private set
    var result by mutableStateOf<ReadResult?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    /** Last tap-to-focus point (fractions) and a counter that restarts its ring animation. */
    var focusPoint by mutableStateOf<Offset?>(null)
        private set
    var focusTick by mutableIntStateOf(0)
        private set
    var explanation by mutableStateOf("")
        private set
    var explainState by mutableStateOf(ExplainState.Idle)
        private set

    val busy get() = phase == Phase.Busy
    val cameraPreview: View get() = camera.preview
    val imageAspect get() = camera.imageAspect
    val hasFlash get() = camera.hasFlash

    // ---------------- internal ----------------

    private var explainJob: Job? = null
    /** Sentences read before the current one, oldest first: context for the AI. */
    private val history = ArrayDeque<String>()
    // Freeze: the screen picture (instant) and the full-resolution photo of the same moment (arrives shortly after)
    private var frozenSnapshot: Snapshot? = null
    private var frozenPhoto: Deferred<Photo?>? = null
    private var readJob: Job? = null
    private var pendingRead: Job? = null

    // ---------------- setup ----------------

    fun startCamera() {
        camera.start(::onFrame) { msg -> status = msg }
        checkServer()
    }

    fun checkServer() {
        scope.launch {
            status = "Connecting to ${server.address}…"
            status = if (server.ping()) "Connected. Drag on the camera to set the box, tap to focus"
            else "Can't reach the PC (${server.address}). Is start_server.bat running? Address: 📖 → ⚙"
        }
    }

    fun permissionDenied() {
        status = "Camera permission is needed"
    }

    // ---------------- box gestures ----------------

    fun onDragStart() {
        pendingRead?.cancel()   // correcting the box: forget the read that was about to start
        boxVisible = true
    }

    fun onBoxChanged() {
        autoRead.forgetLastRead()
        if (frozen != null) {
            // read automatically once the box has stayed put for a moment
            pendingRead?.cancel()
            pendingRead = scope.launch {
                delay(boxDebounceMs)
                read()
            }
        }
    }

    /** Tap on the camera area at fractions f. */
    fun onTap(f: Offset) {
        if (frozen != null) {
            if (boxVisible && box.contains(f)) read()   // tap the box = read it again
            return
        }
        camera.focus(f.x, f.y)
        focusPoint = f
        focusTick++
    }

    // ---------------- buttons ----------------

    fun toggleAuto() {
        auto = !auto
        autoRead.forgetLastRead()
        status = when {
            !auto -> "Auto off"
            frozen != null -> "Auto resumes after Unfreeze"
            else -> "Hold still over a bubble…"
        }
    }

    fun toggleTorch() {
        torch = !torch
        camera.setTorch(torch)
    }

    fun toggleFreeze() {
        if (frozen != null) {
            pendingRead?.cancel()
            readJob?.cancel()
            frozenPhoto?.cancel()
            frozen = null
            frozenSnapshot = null
            frozenPhoto = null
            boxVisible = true
            phase = Phase.Idle
            autoRead.forgetLastRead()
            status = if (auto) "Hold still over a bubble…" else "Live"
            return
        }
        val shot = camera.snapshot() ?: run {
            status = "Camera not ready yet"
            return
        }
        pendingRead?.cancel()
        readJob?.cancel()
        phase = Phase.Idle
        frozenSnapshot = shot
        frozen = shot.image
        boxVisible = false
        status = "Frozen: drag a box around a bubble"
        // full-resolution photo of the same moment, used for reading when it arrives
        frozenPhoto = scope.async {
            try {
                camera.takePhoto()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
        }
    }

    fun read() {
        if (frozen == null && busy) return
        if (frozen != null && !boxVisible) {
            status = "Drag a box around a bubble first"
            return
        }
        pendingRead?.cancel()
        readJob?.cancel()   // a newer box wins over a read still in progress
        val b = box
        phase = Phase.Busy
        readJob = scope.launch {
            try {
                if (frozen != null) {
                    status = "Reading…"
                    val photo = frozenPhoto?.await()
                    val jpeg = if (photo != null) {
                        withContext(work) { cropper.cropPhoto(photo, b) }
                    } else {
                        val snap = frozenSnapshot ?: return@launch
                        withContext(work) { cropper.cropSnapshot(snap, b) }
                    }
                    send(jpeg)
                } else {
                    val photo = shoot(b)
                    status = "Reading…"
                    send(withContext(work) { cropper.cropPhoto(photo, b) })
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                fail(e)
            }
        }
    }

    /** A word in the current result was tapped or its card opened (feeds the Frequent dictionary). */
    fun lookedUp(t: Token) {
        result?.let { stats.recordLookup(it, t) }
    }

    // ---------------- AI explanation (on demand) ----------------

    fun explain() {
        val r = result ?: return
        explainJob?.cancel()
        explanation = ""
        explainState = ExplainState.Waiting
        val context = history.filter { it != r.text }.takeLast(CONTEXT_BUBBLES)
        explainJob = scope.launch {
            try {
                server.explain(r.text, context).collect { chunk ->
                    explanation += chunk
                    if (explainState == ExplainState.Waiting) explainState = ExplainState.Writing
                }
                explainState = if (explanation.contains(STREAM_ERROR)) ExplainState.Failed else ExplainState.Done
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                explanation = e.message ?: e.toString()
                explainState = ExplainState.Failed
            }
        }
    }

    /** The error part of a failed explanation, for display. */
    val explainError get() = explanation.substringAfter(STREAM_ERROR).trim().ifEmpty { "The AI could not answer." }

    // ---------------- internals ----------------

    /** Live mode: focus on the box and take a full-resolution photo. */
    private suspend fun shoot(b: Rect): Photo {
        status = "Focusing…"
        camera.focusAndWait(b.center.x, b.center.y)
        status = "Taking photo…"
        return camera.takePhoto()
    }

    private suspend fun send(jpeg: ByteArray) {
        status = "Reading…"
        val r = server.read(jpeg)
        onNewResult(r)
        result = r
        stats.recordSeen(r)
        error = null
        phase = Phase.Done
        status = when {
            frozen != null -> "Done. Drag another box, or tap the box to read again"
            auto -> "Done. Move to the next bubble"
            else -> "Done"
        }
        delay(doneDisplayMs)
        if (phase == Phase.Done) phase = Phase.Idle
    }

    private fun onNewResult(r: ReadResult) {
        val previous = result?.text
        if (previous != null && previous != r.text) {
            history.addLast(previous)
            while (history.size > HISTORY_SIZE) history.removeFirst()
        }
        explainJob?.cancel()
        explanation = ""
        explainState = ExplainState.Idle
    }

    private fun fail(e: Exception) {
        phase = Phase.Idle
        val msg = e.message ?: e.toString()
        error = msg
        status = "Error: $msg"
    }

    /** Auto mode (called on the camera analysis thread). */
    private fun onFrame(frame: CameraFrame) {
        if (!auto || busy || frozen != null) {
            autoRead.pause()
            return
        }
        when (autoRead.onFrame(clock()) { frame.signature(box) }) {
            AutoReadDetector.Decision.Read -> scope.launch { read() }
            AutoReadDetector.Decision.Moving -> scope.launch { if (!busy && auto) status = "Hold still over a bubble…" }
            else -> Unit
        }
    }

    companion object {
        const val CONTEXT_BUBBLES = 3
        private const val HISTORY_SIZE = 5
        /** How the server marks a failure inside a streamed answer. */
        const val STREAM_ERROR = "ERROR:"
    }
}
