package com.mangareader.local.logic

import kotlin.math.abs

/**
 * Auto mode: decides from camera frames when to read. It reads once the box content has been
 * still for a moment and looks different from what was read last. Pure logic, fed with the
 * frame's brightness grid ([CameraFrame.signature]) and a timestamp.
 */
class AutoReadDetector(
    private val intervalMs: Long = 150,      // look at a frame at most this often
    private val stillMs: Long = 700,         // how long the content must stay still
    private val stillDiff: Float = 4f,       // below this mean difference two frames count as "still"
    private val changedDiff: Float = 10f,    // above this the content counts as a new bubble
) {
    enum class Decision { Skip, Moving, Waiting, Read }

    @Volatile private var prevSig: FloatArray? = null
    @Volatile private var lastReadSig: FloatArray? = null
    private var stillSince = 0L
    private var lastCheck = 0L

    /** Not watching right now (Auto off, busy or frozen): the next frame starts fresh. */
    fun pause() {
        prevSig = null
    }

    /** The box moved or Auto was toggled: the same content may be read again. */
    fun forgetLastRead() {
        lastReadSig = null
    }

    fun onFrame(now: Long, signature: () -> FloatArray): Decision {
        if (now - lastCheck < intervalMs) return Decision.Skip
        lastCheck = now
        val sig = signature()
        val prev = prevSig
        prevSig = sig
        if (prev == null || diff(sig, prev) >= stillDiff) {
            stillSince = 0L
            return Decision.Moving
        }
        if (stillSince == 0L) stillSince = now
        val last = lastReadSig
        if (now - stillSince > stillMs && (last == null || diff(sig, last) > changedDiff)) {
            lastReadSig = sig
            stillSince = 0L
            return Decision.Read
        }
        return Decision.Waiting
    }

    companion object {
        /** Mean absolute difference of two brightness grids. */
        fun diff(a: FloatArray, b: FloatArray): Float {
            var d = 0f
            for (i in a.indices) d += abs(a[i] - b[i])
            return d / a.size
        }
    }
}
