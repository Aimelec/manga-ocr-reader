package com.mangareader.local

import com.mangareader.local.model.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object ImageTools {

    const val SIG = 24   // Auto mode compares 24x24 brightness grids

    class Crop(val jpeg: ByteArray, val preview: ImageBitmap)

    /**
     * The box is in fractions (0..1) of the upright image on screen; the sensor image must be
     * rotated `rot` degrees clockwise to look like that. Returns the box in sensor fractions.
     */
    fun toSensor(f: Rect, rot: Int): Rect {
        var minX = 1f; var minY = 1f; var maxX = 0f; var maxY = 0f
        for (x in floatArrayOf(f.left, f.right)) for (y in floatArrayOf(f.top, f.bottom)) {
            val (sx, sy) = when (rot) {
                90 -> y to 1 - x
                180 -> 1 - x to 1 - y
                270 -> 1 - y to x
                else -> x to y
            }
            minX = min(minX, sx); maxX = max(maxX, sx)
            minY = min(minY, sy); maxY = max(maxY, sy)
        }
        return Rect(minX, minY, maxX, maxY)
    }

    /** Cut the box out of the full-resolution photo, upright, at most `maxSide` px. */
    fun crop(jpeg: ByteArray, rot: Int, box: Rect, maxSide: Int = 1600): Crop {
        @Suppress("DEPRECATION")
        val dec = BitmapRegionDecoder.newInstance(jpeg, 0, jpeg.size, false)!!
        val w = dec.width
        val h = dec.height
        val s = toSensor(box, rot)
        // small margin: preview and photo framing can differ by a few pixels
        val mx = s.width * 0.03f
        val my = s.height * 0.03f
        val r = android.graphics.Rect(
            max(0, ((s.left - mx) * w).toInt()), max(0, ((s.top - my) * h).toInt()),
            min(w, ((s.right + mx) * w).toInt()), min(h, ((s.bottom + my) * h).toInt()))
        var bmp = dec.decodeRegion(r, null)
        dec.recycle()
        val scale = min(1f, maxSide.toFloat() / max(bmp.width, bmp.height))
        if (rot != 0 || scale < 1f) {
            val m = Matrix().apply { postScale(scale, scale); postRotate(rot.toFloat()) }
            val t = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
            if (t !== bmp) bmp.recycle()
            bmp = t
        }
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 95, out)
        return Crop(out.toByteArray(), bmp.asImageBitmap())
    }

    /** Cut the box out of an upright bitmap (the frozen screen picture), as JPEG. */
    fun cropBitmap(bmp: Bitmap, box: Rect): ByteArray {
        val l = (box.left * bmp.width).toInt().coerceIn(0, bmp.width - 1)
        val t = (box.top * bmp.height).toInt().coerceIn(0, bmp.height - 1)
        val r = (box.right * bmp.width).toInt().coerceIn(l + 1, bmp.width)
        val b = (box.bottom * bmp.height).toInt().coerceIn(t + 1, bmp.height)
        val c = Bitmap.createBitmap(bmp, l, t, r - l, b - t)
        val out = ByteArrayOutputStream()
        c.compress(Bitmap.CompressFormat.JPEG, 95, out)
        return out.toByteArray()
    }

    /** Whole photo, upright, downsampled for showing on screen. */
    fun decodeForDisplay(jpeg: ByteArray, rot: Int, maxSide: Int = 2048): ImageBitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size, bounds)
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val b = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size, BitmapFactory.Options().apply { inSampleSize = sample })
        if (rot == 0) return b.asImageBitmap()
        val r = Bitmap.createBitmap(b, 0, 0, b.width, b.height, Matrix().apply { postRotate(rot.toFloat()) }, true)
        if (r !== b) b.recycle()
        return r.asImageBitmap()
    }

    /** 24x24 brightness grid of the box area of a camera frame (Y plane). */
    fun signature(img: ImageProxy, s: Rect): FloatArray {
        val plane = img.planes[0]
        val buf = plane.buffer
        val rs = plane.rowStride
        val ps = plane.pixelStride
        val w = img.width
        val h = img.height
        val g = FloatArray(SIG * SIG)
        for (j in 0 until SIG) {
            val yy = min(h - 1, ((s.top + s.height * (j + 0.5f) / SIG) * h).toInt())
            for (i in 0 until SIG) {
                val xx = min(w - 1, ((s.left + s.width * (i + 0.5f) / SIG) * w).toInt())
                g[j * SIG + i] = (buf.get(yy * rs + xx * ps).toInt() and 0xff).toFloat()
            }
        }
        return g
    }

    fun diff(a: FloatArray, b: FloatArray): Float {
        var d = 0f
        for (i in a.indices) d += abs(a[i] - b[i])
        return d / a.size
    }
}
