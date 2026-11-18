package com.mangareader.local.logic

import android.view.View
import androidx.compose.ui.geometry.Rect
import com.mangareader.local.model.ReadResult
import com.mangareader.local.model.SavedWord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakeServer(
    var reachable: Boolean = true,
    var results: ArrayDeque<ReadResult> = ArrayDeque(),
    var readError: Exception? = null,
    var explainPieces: List<String> = listOf("TRANSLATION: ok"),
    var explainError: Exception? = null,
) : ReadingServer {
    override var address = "192.168.1.100:8765"
    val reads = mutableListOf<ByteArray>()
    val explains = mutableListOf<Pair<String, List<String>>>()
    var certificateForgotten = false

    override suspend fun ping() = reachable

    override suspend fun read(jpeg: ByteArray): ReadResult {
        reads += jpeg
        readError?.let { throw it }
        return results.removeFirst()
    }

    override fun explain(line: String, context: List<String>): Flow<String> = flow {
        explains += line to context
        explainError?.let { throw it }
        explainPieces.forEach { emit(it) }
    }

    override fun forgetCertificate() {
        certificateForgotten = true
    }
}

class FakeCamera : CameraDevice {
    override val preview: View get() = error("no Android views in JVM tests")
    override val imageAspect = 0.75f
    override val hasFlash = true
    var onFrame: ((CameraFrame) -> Unit)? = null
    val focusedAt = mutableListOf<Pair<Float, Float>>()
    var torchOn = false
    var photos = 0

    override fun start(onFrame: (CameraFrame) -> Unit, onError: (String) -> Unit) {
        this.onFrame = onFrame
    }

    override fun focus(fx: Float, fy: Float) {
        focusedAt += fx to fy
    }

    override suspend fun focusAndWait(fx: Float, fy: Float) = focus(fx, fy)

    override suspend fun takePhoto(): Photo {
        photos++
        return Photo(byteArrayOf(1, 2, 3), 90)
    }

    override fun snapshot(): Snapshot? = null

    override fun setTorch(on: Boolean) {
        torchOn = on
    }

    /** Send a frame whose box content has the given uniform brightness. */
    fun frame(brightness: Float) = onFrame!!(CameraFrame { FloatArray(4) { brightness } })
}

class FakeCropper : PhotoCropper {
    val boxes = mutableListOf<Rect>()
    override fun cropPhoto(photo: Photo, box: Rect): ByteArray {
        boxes += box
        return photo.jpeg + byteArrayOf(photo.rotation.toByte())
    }

    override fun cropSnapshot(snapshot: Snapshot, box: Rect) = error("not used: the fake camera has no snapshots")
}

class MemoryFavourites(initial: List<SavedWord> = emptyList()) : FavouritesStorage {
    var saved: List<SavedWord> = initial
    var saves = 0
    override fun load() = saved
    override fun save(words: List<SavedWord>) {
        saved = words
        saves++
    }
}

class MemoryStats(initial: StoredStats = StoredStats(emptyList(), emptyList())) : StatsStorage {
    var saved = initial
    var saves = 0
    override fun load() = saved
    override fun save(stats: StoredStats) {
        saved = stats
        saves++
    }
}
