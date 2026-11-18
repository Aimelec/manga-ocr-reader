package com.mangareader.local.logic

import android.view.View
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import com.mangareader.local.model.ReadResult
import com.mangareader.local.model.SavedWord
import com.mangareader.local.model.WordStat
import kotlinx.coroutines.flow.Flow

// What the logic layer needs from the outside world. The data layer implements these;
// unit tests use fakes. The logic never knows about HTTP, files, CameraX or Bitmaps.

/** The PC server. */
interface ReadingServer {
    /** "ip:port" of the PC. */
    var address: String
    /** Code shown in the server window at startup; sent with every request. */
    var pairingCode: String

    suspend fun ping(): Boolean
    suspend fun read(jpeg: ByteArray): ReadResult
    /** AI explanation of one bubble, piece by piece while the PC's model writes it. */
    fun explain(line: String, context: List<String>): Flow<String>
    /** Forget the remembered certificate (after the server made a new one). */
    fun forgetCertificate()
}

/** A full-resolution photo and the clockwise rotation that makes it upright. */
class Photo(val jpeg: ByteArray, val rotation: Int)

/** An instant picture of what the preview shows (used by Freeze). */
interface Snapshot {
    val image: ImageBitmap
}

/** One live camera frame, valid only during the callback. */
fun interface CameraFrame {
    /** Brightness grid of the box area (fractions of the upright image), for Auto mode. */
    fun signature(box: Rect): FloatArray
}

interface CameraDevice {
    /** The live preview, placed on screen by the UI. */
    val preview: View
    /** Width / height of the camera image as shown (portrait). */
    val imageAspect: Float
    val hasFlash: Boolean

    fun start(onFrame: (CameraFrame) -> Unit, onError: (String) -> Unit)
    fun focus(fx: Float, fy: Float)
    suspend fun focusAndWait(fx: Float, fy: Float)
    suspend fun takePhoto(): Photo
    fun snapshot(): Snapshot?
    fun setTorch(on: Boolean)
}

/** Cuts the reading box (fractions of the upright image) out of a picture, as JPEG. */
interface PhotoCropper {
    fun cropPhoto(photo: Photo, box: Rect): ByteArray
    fun cropSnapshot(snapshot: Snapshot, box: Rect): ByteArray
}

interface FavouritesStorage {
    fun load(): List<SavedWord>
    fun save(words: List<SavedWord>)
}

/** Everything the Frequent statistics keep between runs. */
data class StoredStats(val words: List<WordStat>, val sentences: List<String>)

interface StatsStorage {
    fun load(): StoredStats
    fun save(stats: StoredStats)
}
