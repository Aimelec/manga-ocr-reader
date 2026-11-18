package com.mangareader.local.ui.reader

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.viewinterop.AndroidView
import com.mangareader.local.logic.Favourites
import com.mangareader.local.logic.ReaderViewModel
import com.mangareader.local.model.*
import com.mangareader.local.ui.AppColors
import com.mangareader.local.ui.BookIcon
import com.mangareader.local.ui.FlashlightIcon
import kotlin.math.max
import kotlin.math.min

// The reader page: camera with the reading box, buttons and status, results in a bottom sheet.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(reader: ReaderViewModel, store: Favourites, onDictionary: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black).systemBarsPadding()) {
        val h = maxHeight
        val sheetState = rememberStandardBottomSheetState(initialValue = SheetValue.Expanded, skipHiddenState = true)
        val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState)
        val density = LocalDensity.current

        // Results live in a sheet: drag it down for ~80% camera, up for 50/50.
        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetPeekHeight = h * 0.2f,
            sheetContainerColor = MaterialTheme.colorScheme.surface,
            sheetShadowElevation = 8.dp,
            sheetDragHandle = { DragHandle() },
            containerColor = Color.Black,
            sheetContent = {
                Column(Modifier.fillMaxWidth().height(h * 0.5f - 20.dp)) {
                    Controls(reader, onDictionary)
                    Results(reader, store, Modifier.weight(1f))
                }
            },
        ) {
            // The camera fills the space above the sheet and follows it while dragging.
            val offset = runCatching { sheetState.requireOffset() }.getOrNull()
            val cameraHeight = if (offset != null) with(density) { offset.toDp() } else h * 0.5f
            CameraArea(reader, Modifier.fillMaxWidth().height(cameraHeight))
        }
    }

}

@Composable
private fun DragHandle() {
    Box(Modifier.fillMaxWidth().height(20.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(44.dp, 5.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.outline))
    }
}

// ======================= camera + box =======================

@Composable
private fun CameraArea(reader: ReaderViewModel, modifier: Modifier) {
    // keep the last frozen image during the fade-out
    var shownFrozen by remember { mutableStateOf<ImageBitmap?>(null) }
    reader.frozen?.let { shownFrozen = it }

    Box(modifier.background(Color.Black).clipToBounds()) {
        AndroidView(factory = { reader.cameraPreview }, modifier = Modifier.fillMaxSize())
        AnimatedVisibility(visible = reader.frozen != null, enter = fadeIn(), exit = fadeOut()) {
            shownFrozen?.let {
                Image(it, contentDescription = "Frozen photo", contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().background(Color.Black))
            }
        }
        BoxOverlay(reader, Modifier.fillMaxSize())
        AnimatedVisibility(visible = reader.frozen != null, enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopStart).padding(10.dp)) {
            Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer) {
                Text("FROZEN", Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

/** Where the camera image is drawn inside the view (fit-center). */
private fun fitRect(size: Size, aspect: Float): Rect {
    if (size.width == 0f || size.height == 0f) return Rect.Zero
    var w = size.width
    var h = size.width / aspect
    if (h > size.height) { h = size.height; w = size.height * aspect }
    return Rect(Offset((size.width - w) / 2, (size.height - h) / 2), Size(w, h))
}

@Composable
private fun BoxOverlay(reader: ReaderViewModel, modifier: Modifier) {
    var size by remember { mutableStateOf(Size.Zero) }
    val content = fitRect(size, reader.imageAspect)
    val primary = MaterialTheme.colorScheme.primary
    val frameColor by animateColorAsState(
        when (reader.phase) {
            ReaderViewModel.Phase.Busy -> AppColors.furigana
            ReaderViewModel.Phase.Done -> AppColors.grammar
            ReaderViewModel.Phase.Idle -> primary
        }, label = "boxColor")
    val ring = remember { Animatable(0f) }
    LaunchedEffect(reader.focusTick) {
        if (reader.focusTick > 0) {
            ring.snapTo(1f)
            ring.animateTo(0f, tween(900))
        }
    }

    fun clamp(p: Offset) = Offset(p.x.coerceIn(content.left, content.right), p.y.coerceIn(content.top, content.bottom))
    fun toFraction(a: Offset, b: Offset) = Rect(
        (min(a.x, b.x) - content.left) / content.width, (min(a.y, b.y) - content.top) / content.height,
        (max(a.x, b.x) - content.left) / content.width, (max(a.y, b.y) - content.top) / content.height)

    Canvas(
        modifier
            .onSizeChanged { size = it.toSize() }
            .pointerInput(content) {
                var start = Offset.Zero
                detectDragGestures(
                    onDragStart = {
                        start = clamp(it)
                        reader.onDragStart()
                    },
                    onDragEnd = { reader.onBoxChanged() },
                ) { change, _ ->
                    change.consume()
                    if (content.width > 0f) reader.box = toFraction(start, clamp(change.position))
                }
            }
            .pointerInput(content) {
                detectTapGestures { p ->
                    if (content.contains(p)) {
                        reader.onTap(Offset((p.x - content.left) / content.width, (p.y - content.top) / content.height))
                    }
                }
            }
    ) {
        if (content.width == 0f) return@Canvas
        val f = reader.focusPoint
        if (f != null && ring.value > 0f) {
            drawCircle(Color.White.copy(alpha = ring.value), radius = (34 - 8 * ring.value).dp.toPx(),
                center = Offset(content.left + f.x * content.width, content.top + f.y * content.height),
                style = Stroke(2.dp.toPx()))
        }
        if (!reader.boxVisible) return@Canvas
        val b = reader.box
        val r = Rect(content.left + b.left * content.width, content.top + b.top * content.height,
            content.left + b.right * content.width, content.top + b.bottom * content.height)
        val dim = Color.Black.copy(alpha = 0.38f)
        drawRect(dim, Offset.Zero, Size(this.size.width, r.top))
        drawRect(dim, Offset(0f, r.bottom), Size(this.size.width, this.size.height - r.bottom))
        drawRect(dim, Offset(0f, r.top), Size(r.left, r.height))
        drawRect(dim, Offset(r.right, r.top), Size(this.size.width - r.right, r.height))
        drawRoundRect(frameColor, r.topLeft, r.size, CornerRadius(6.dp.toPx()), style = Stroke(3.dp.toPx()))
    }
}

// ======================= buttons + status =======================

@Composable
private fun Controls(reader: ReaderViewModel, onDictionary: () -> Unit) {
    val small = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
    Column(Modifier.padding(horizontal = 8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = reader::read, enabled = !reader.busy, contentPadding = small,
                modifier = Modifier.weight(1.2f)) {
                Text("Read", fontWeight = FontWeight.SemiBold)
            }
            FilledTonalButton(
                onClick = reader::toggleFreeze, contentPadding = small, modifier = Modifier.weight(1.3f),
                colors = if (reader.frozen != null) ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                else ButtonDefaults.filledTonalButtonColors(),
            ) {
                AnimatedContent(if (reader.frozen != null) "Unfreeze" else "Freeze", label = "freeze") {
                    Text(it, maxLines = 1)
                }
            }
            FilledTonalButton(
                onClick = reader::toggleAuto, contentPadding = small, modifier = Modifier.weight(1f),
                colors = if (reader.auto) ButtonDefaults.filledTonalButtonColors(
                    containerColor = AppColors.grammar, contentColor = Color.Black)
                else ButtonDefaults.filledTonalButtonColors(),
            ) { Text("Auto") }
            FilledIconToggleButton(checked = reader.torch, onCheckedChange = { reader.toggleTorch() },
                enabled = reader.hasFlash) {
                Icon(FlashlightIcon, contentDescription = "Flashlight")
            }
            FilledTonalIconButton(onClick = onDictionary) {
                Icon(BookIcon, contentDescription = "My words")
            }
        }
        AnimatedContent(reader.status, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "status") {
            Text(it, Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
