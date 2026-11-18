package com.mangareader.local

import com.mangareader.local.logic.Favourites
import com.mangareader.local.logic.ReaderViewModel
import com.mangareader.local.logic.SettingsViewModel
import com.mangareader.local.logic.parseExplanation
import com.mangareader.local.logic.suggestedCorrection
import com.mangareader.local.model.*
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

// ======================= screen =======================

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

// ======================= results =======================

@Composable
private fun Results(reader: ReaderViewModel, store: Favourites, modifier: Modifier) {
    val r = reader.result
    val error = reader.error
    val listState = rememberLazyListState()
    val open = remember(r) { mutableStateMapOf<Int, Boolean>() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(r) { if (r != null) listState.scrollToItem(0) }

    Box(modifier.fillMaxWidth()) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (error != null) item(key = "error") {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth().animateItem()) {
                Text(error, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
        if (r == null) {
            if (error == null) item(key = "hint") {
                Text("Drag on the camera to put the box around a bubble, then tap Read. " +
                    "Or turn Auto on and hold still.",
                    Modifier.padding(8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@LazyColumn
        }

        val words = r.tokens.withIndex().filter { !it.value.isPunctuation }
        val firstWordItem = (if (error != null) 1 else 0) + 3 +   // sentence, hiragana, explain
            (if (r.grammar.isNotEmpty()) 1 + r.grammar.size else 0) + 1

        item(key = "line") {
            FuriganaLine(r.tokens, Modifier.animateItem()) { i ->
                val pos = words.indexOfFirst { it.index == i }
                if (pos >= 0) {
                    reader.lookedUp(r.tokens[i])
                    open[i] = true
                    scope.launch { listState.animateScrollToItem(firstWordItem + pos) }
                }
            }
        }
        item(key = "hira") {
            Text(r.hiragana, Modifier.padding(horizontal = 4.dp).animateItem(),
                fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item(key = "explain") { ExplainCard(reader, Modifier.animateItem()) }
        if (r.grammar.isNotEmpty()) {
            item(key = "grammarTitle") { SectionTitle("Grammar", Modifier.animateItem()) }
            items(r.grammar, key = { "g:" + it.name }) { GrammarCard(it, Modifier.animateItem()) }
        }
        item(key = "wordsTitle") { SectionTitle("Words", Modifier.animateItem(), "tap a card for more") }
        items(words, key = { "w:" + it.index }) { (i, t) ->
            WordCard(t, open[i] == true, store.isSaved(t), onSave = { store.toggle(t) },
                modifier = Modifier.animateItem()) {
                val opening = open[i] != true
                if (opening) reader.lookedUp(t)
                open[i] = opening
            }
        }
    }

    // Floating "back to sentence" button while the sentence line is scrolled out of view
    val lineItem = if (error != null) 1 else 0
    val showBack by remember(r, error) {
        derivedStateOf { r != null && listState.firstVisibleItemIndex > lineItem }
    }
    AnimatedVisibility(visible = showBack, enter = fadeIn() + slideInVertically { -it },
        exit = fadeOut() + slideOutVertically { -it },
        modifier = Modifier.align(Alignment.TopCenter).padding(top = 6.dp)) {
        ExtendedFloatingActionButton(
            onClick = { scope.launch { listState.animateScrollToItem(0) } },
            icon = { Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null) },
            text = { Text("Back to sentence") },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.height(44.dp),
        )
    }
    }
}

/** "✨ Explain" button, then the AI's translation, literal version and notes as they arrive. */
@Composable
private fun ExplainCard(reader: ReaderViewModel, modifier: Modifier) {
    val state = reader.explainState
    if (state == ReaderViewModel.ExplainState.Idle) {
        FilledTonalButton(onClick = reader::explain, modifier = modifier.fillMaxWidth()) {
            Text("✨ Explain this sentence")
        }
        return
    }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.fillMaxWidth().animateContentSize()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("✨ AI EXPLANATION", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
                if (state == ReaderViewModel.ExplainState.Done || state == ReaderViewModel.ExplainState.Failed) {
                    TextButton(onClick = reader::explain) { Text("Again") }
                }
            }
            when (state) {
                ReaderViewModel.ExplainState.Waiting -> {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("Thinking… (the first time, the PC loads the AI: about 10 s)",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                ReaderViewModel.ExplainState.Failed -> {
                    Text(reader.explainError,
                        color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
                else -> {
                    val e = parseExplanation(reader.explanation)
                    if (e.translation.isNotEmpty()) Text(e.translation, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    if (e.literal.isNotEmpty()) {
                        Text("Literal: ${e.literal}", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    e.notes.forEach { note ->
                        Row {
                            Text("•", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp))
                            Text(note, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    val ocr = suggestedCorrection(e, reader.result?.text)
                    if (ocr != null) {
                        Text("Possible misread, the AI thinks it says: $ocr", color = AppColors.furigana,
                            style = MaterialTheme.typography.bodySmall)
                    }
                    if (state == ReaderViewModel.ExplainState.Writing) LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier, hint: String? = null) {
    Row(modifier.padding(top = 8.dp, start = 4.dp), verticalAlignment = Alignment.Bottom) {
        Text(text.uppercase(), style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
        if (hint != null) {
            Spacer(Modifier.width(8.dp))
            Text(hint, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

/** The sentence with furigana over the kanji; tap a word to open its card. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FuriganaLine(tokens: List<Token>, modifier: Modifier, onWord: (Int) -> Unit) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth()) {
        FlowRow(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            tokens.forEachIndexed { i, t ->
                Row(Modifier.clip(RoundedCornerShape(6.dp)).clickable(enabled = !t.isPunctuation) { onWord(i) }) {
                    t.ruby.forEach { (text, kana) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(kana.ifEmpty { " " }, fontSize = 11.sp, color = AppColors.furigana,
                                maxLines = 1, softWrap = false)
                            Text(text, fontSize = 25.sp, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GrammarCard(g: GrammarHit, modifier: Modifier) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(g.name, color = AppColors.grammar, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (g.match.isNotEmpty()) {
                    Spacer(Modifier.width(6.dp))
                    Text("(${g.match})", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(g.explanation, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordCard(
    t: Token, open: Boolean, saved: Boolean, onSave: () -> Unit, modifier: Modifier, onClick: () -> Unit,
) {
    val isFunction = t.function.isNotEmpty()
    Card(onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.fillMaxWidth().animateContentSize()) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(t.surface, fontSize = 22.sp)
                if (t.reading.isNotEmpty() && t.reading != t.surface) {
                    Spacer(Modifier.width(8.dp))
                    Text(t.reading, color = AppColors.furigana, fontSize = 15.sp)
                }
                Spacer(Modifier.width(8.dp))
                Text(t.label, style = MaterialTheme.typography.labelSmall,
                    color = if (isFunction) AppColors.particle else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 3.dp).weight(1f))
                // save to / remove from "My words"
                IconButton(onClick = onSave, modifier = Modifier.size(36.dp)) {
                    val tint by animateColorAsState(if (saved) AppColors.furigana else MaterialTheme.colorScheme.outline,
                        label = "star")
                    Icon(Icons.Filled.Star, contentDescription = if (saved) "Remove from My words" else "Save to My words",
                        tint = tint)
                }
            }
            Text(t.shortMeaning, style = MaterialTheme.typography.bodyMedium,
                maxLines = if (open) Int.MAX_VALUE else 2, overflow = TextOverflow.Ellipsis)

            AnimatedVisibility(visible = open && t.hasDetails, enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()) {
                Column(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    t.entries.forEach { EntryView(it) }
                    t.parts.forEach { p ->
                        Text("${p.surface} [${p.reading}]", fontWeight = FontWeight.SemiBold)
                        p.entries.forEach { EntryView(it) }
                    }
                    if (t.kanji.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            t.kanji.forEach { KanjiBox(it) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun EntryView(e: Entry) {
    Column {
        Text((e.kanji + e.kana).joinToString(" · "), color = AppColors.furigana, fontSize = 15.sp)
        e.senses.forEachIndexed { n, s ->
            Row {
                Text("${n + 1}.", Modifier.width(22.dp), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Column {
                    Text(s.gloss, style = MaterialTheme.typography.bodyMedium)
                    if (s.pos.isNotEmpty()) Text(s.pos, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

@Composable
internal fun KanjiBox(k: KanjiInfo) {
    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(Modifier.padding(8.dp).width(150.dp)) {
            Text(k.char, fontSize = 30.sp, modifier = Modifier.padding(end = 8.dp))
            Column {
                Text(k.meanings.joinToString(", "), style = MaterialTheme.typography.bodySmall)
                if (k.on.isNotEmpty()) Text("on: " + k.on.joinToString("、"),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (k.kun.isNotEmpty()) Text("kun: " + k.kun.joinToString("、"),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ======================= settings =======================

@Composable
internal fun SettingsDialog(settings: SettingsViewModel, onClose: () -> Unit) {
    var address by remember { mutableStateOf(settings.address) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("PC address") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Shown in the server window, like 192.168.1.100:8765", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(value = address, onValueChange = { address = it }, singleLine = true)
                TextButton(onClick = {
                    settings.trustCertificateAgain()
                    onClose()
                }) { Text("Trust the PC's certificate again") }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                settings.save(address)
                onClose()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Cancel") } },
    )
}
