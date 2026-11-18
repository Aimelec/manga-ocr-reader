package com.mangareader.local.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangareader.local.logic.Favourites
import com.mangareader.local.logic.ReaderViewModel
import com.mangareader.local.logic.parseExplanation
import com.mangareader.local.logic.suggestedCorrection
import com.mangareader.local.model.*
import com.mangareader.local.ui.AppColors
import com.mangareader.local.ui.EntryView
import com.mangareader.local.ui.KanjiBox
import com.mangareader.local.ui.SectionTitle
import kotlinx.coroutines.launch

// The results sheet: sentence with furigana, AI explanation, grammar and word cards.

@Composable
internal fun Results(reader: ReaderViewModel, store: Favourites, modifier: Modifier) {
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
