package com.mangareader.local

import android.content.Context
import android.graphics.Bitmap
import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** CameraX: live preview, tap/box focus, full-resolution photo, and frames for Auto mode. */
class CameraController(private val context: Context, private val owner: LifecycleOwner) {

    val previewView = PreviewView(context).apply {
        scaleType = PreviewView.ScaleType.FIT_CENTER
        implementationMode = PreviewView.ImplementationMode.COMPATIBLE   // resizes smoothly with the sheet
    }

    /** Width / height of the camera image as shown (portrait). */
    var imageAspect by mutableFloatStateOf(3f / 4f)
        private set
    var hasFlash by mutableStateOf(false)
        private set

    private var camera: Camera? = null
    private var capture: ImageCapture? = null
    private val photoExecutor = Executors.newSingleThreadExecutor()
    private val analysisExecutor = Executors.newSingleThreadExecutor()

    fun start(analyzer: (ImageProxy) -> Unit, onError: (String) -> Unit) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                val provider = future.get()
                val ratio = AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY
                val preview = Preview.Builder()
                    .setResolutionSelector(ResolutionSelector.Builder().setAspectRatioStrategy(ratio).build())
                    .build()
                preview.setSurfaceProvider(previewView.surfaceProvider)
                // Zero shutter lag: the photo is the frame from the moment of the tap (Freeze relies on this).
                // Phones without support fall back to minimum latency automatically.
                val cap = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG)
                    .setResolutionSelector(ResolutionSelector.Builder()
                        .setAspectRatioStrategy(ratio)
                        .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                        .build())
                    .build()
                val analysis = ImageAnalysis.Builder()
                    .setResolutionSelector(ResolutionSelector.Builder()
                        .setAspectRatioStrategy(ratio)
                        .setResolutionStrategy(ResolutionStrategy(Size(640, 480),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER))
                        .build())
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(analysisExecutor, analyzer)

                provider.unbindAll()
                val cam = provider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, cap, analysis)
                camera = cam
                capture = cap
                hasFlash = cam.cameraInfo.hasFlashUnit()
                preview.resolutionInfo?.let {
                    val swap = it.rotationDegrees % 180 != 0
                    val w = if (swap) it.resolution.height else it.resolution.width
                    val h = if (swap) it.resolution.width else it.resolution.height
                    imageAspect = w.toFloat() / h
                }
            } catch (e: Exception) {
                onError("Camera error: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /** Box fractions -> a focus point on the preview (which is letterboxed by FIT_CENTER). */
    private fun action(fx: Float, fy: Float): FocusMeteringAction {
        val vw = previewView.width.toFloat()
        val vh = previewView.height.toFloat()
        var cw = vw
        var ch = vw / imageAspect
        if (ch > vh) { ch = vh; cw = vh * imageAspect }
        val point = previewView.meteringPointFactory.createPoint((vw - cw) / 2 + fx * cw, (vh - ch) / 2 + fy * ch)
        return FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
            .setAutoCancelDuration(4, TimeUnit.SECONDS)
            .build()
    }

    fun focus(fx: Float, fy: Float) {
        camera?.cameraControl?.startFocusAndMetering(action(fx, fy))
    }

    /** Focus on a point and wait until done (max 2.5 s; fixed-focus cameras just fail fast). */
    suspend fun focusAndWait(fx: Float, fy: Float) {
        val cam = camera ?: return
        val f = cam.cameraControl.startFocusAndMetering(action(fx, fy))
        withTimeoutOrNull(2500) {
            suspendCancellableCoroutine { cont ->
                f.addListener({ if (cont.isActive) cont.resume(Unit) }, ContextCompat.getMainExecutor(context))
            }
        }
    }

    /** Full-resolution JPEG and the clockwise rotation needed to make it upright. */
    suspend fun takePhoto(): Pair<ByteArray, Int> = suspendCancellableCoroutine { cont ->
        val cap = capture
        if (cap == null) {
            cont.resumeWithException(IllegalStateException("Camera not ready"))
            return@suspendCancellableCoroutine
        }
        cap.takePicture(photoExecutor, object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                try {
                    val rot = image.imageInfo.rotationDegrees
                    val buf = image.planes[0].buffer
                    val bytes = ByteArray(buf.remaining()).also { buf.get(it) }
                    cont.resume(bytes to rot)
                } catch (e: Exception) {
                    cont.resumeWithException(e)
                } finally {
                    image.close()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                cont.resumeWithException(exception)
            }
        })
    }

    /** Instant copy of what the preview shows right now, cut to the camera image (no black bars). */
    fun snapshot(): Bitmap? {
        val full = previewView.bitmap ?: return null
        var w = full.width.toFloat()
        var h = w / imageAspect
        if (h > full.height) { h = full.height.toFloat(); w = h * imageAspect }
        val left = ((full.width - w) / 2).toInt()
        val top = ((full.height - h) / 2).toInt()
        return Bitmap.createBitmap(full, left, top, w.toInt().coerceAtMost(full.width - left),
            h.toInt().coerceAtMost(full.height - top))
    }

    fun setTorch(on: Boolean) {
        camera?.cameraControl?.enableTorch(on)
    }
}
