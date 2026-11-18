package com.mangareader.local.ui.dictionary

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangareader.local.logic.Favourites
import com.mangareader.local.logic.WordStatistics
import com.mangareader.local.model.*
import com.mangareader.local.ui.AppColors
import com.mangareader.local.ui.EntryView
import com.mangareader.local.ui.KanjiBox

// The "My words" page: favourites and the automatic Frequent dictionary, by category.

/** One row in a dictionary list: the word, plus its statistics when there are any. */
private class DictItem(val word: SavedWord, val stat: WordStat?)

@Composable
internal fun DictionaryScreen(store: Favourites, stats: WordStatistics, onCamera: () -> Unit, onSettings: () -> Unit) {
    var frequent by rememberSaveable { mutableStateOf(false) }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmReset by remember { mutableStateOf(false) }
    BackHandler(enabled = category != null) { category = null }

    // Surface gives text and icons the theme's light color (without it they default to black)
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground) {
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            if (category != null) {
                IconButton(onClick = { category = null }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "All categories")
                }
            } else {
                Spacer(Modifier.width(12.dp))
            }
            Text(category ?: "My words", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            IconButton(onClick = { confirmReset = true }) {
                Icon(Icons.Filled.Refresh, contentDescription = "Reset dictionary", tint = MaterialTheme.colorScheme.error)
            }
            IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, contentDescription = "PC address") }
            IconButton(onClick = onCamera) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Back to camera")
            }
        }

        Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !frequent, onClick = { frequent = false; category = null },
                label = { Text("★ Favourites") })
            FilterChip(selected = frequent, onClick = { frequent = true; category = null },
                label = { Text("Frequent") })
        }

        AnimatedContent(
            Pair(frequent, category),
            transitionSpec = {
                if (targetState.second != null) slideInHorizontally { it / 3 } + fadeIn() togetherWith fadeOut()
                else fadeIn() togetherWith slideOutHorizontally { it / 3 } + fadeOut()
            },
            label = "dictionary",
        ) { (freq, cat) ->
            if (cat == null) CategoryGrid(store, stats, freq) { category = it }
            else WordList(store, stats, freq, cat)
        }
    }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            icon = { Icon(Icons.Filled.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Reset the dictionary?") },
            text = {
                Text("This deletes all favourites and all Frequent statistics (counts, example sentences, " +
                    "known words). It can't be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        store.clear()
                        stats.clear()
                        category = null
                        confirmReset = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Reset everything") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
        )
    }
}

private val MAIN_CATEGORIES = setOf("Nouns", "Verbs", "い-adjectives", "な-adjectives", "Adverbs", "Particles")

@Composable
private fun CategoryGrid(store: Favourites, stats: WordStatistics, frequent: Boolean, onOpen: (String) -> Unit) {
    // words per category, best first
    val byCategory: Map<String, List<String>> = CATEGORIES.associateWith { c ->
        if (frequent) stats.inCategory(c, known = false).map { it.word.word } else store.inCategory(c).map { it.word }
    }
    val shown = CATEGORIES.filter { byCategory.getValue(it).isNotEmpty() || (!frequent && it in MAIN_CATEGORIES) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        val empty = byCategory.values.all { it.isEmpty() }
        if (empty) {
            item(span = { GridItemSpan(2) }) {
                Text(
                    if (frequent) "Words from the bubbles you read show up here automatically, " +
                        "most looked-up first."
                    else "No favourites yet. In the results, tap the ★ next to a word to save it here.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(4.dp))
            }
        }
        items(shown, key = { it }) { c ->
            val list = byCategory.getValue(c)
            val n = list.size
            Card(
                onClick = { onOpen(c) },
                enabled = n > 0,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(c, style = MaterialTheme.typography.titleMedium,
                        color = if (n > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline)
                    Text(if (n == 1) "1 word" else "$n words", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (n > 0) {
                        Text(list.take(4).joinToString("、"), color = AppColors.furigana, maxLines = 1,
                            overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun WordList(store: Favourites, stats: WordStatistics, frequent: Boolean, category: String) {
    val items = if (frequent) stats.inCategory(category, known = false).map { DictItem(it.word, it) }
    else store.inCategory(category).map { DictItem(it, stats.get(it.key)) }
    val knownItems = if (frequent) stats.inCategory(category, known = true).map { DictItem(it.word, it) } else emptyList()
    var showKnown by remember(category, frequent) { mutableStateOf(false) }
    val open = remember { mutableStateMapOf<String, Boolean>() }

    LazyColumn(
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (items.isEmpty()) item(key = "empty") {
            Text("Nothing here right now.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(items, key = { it.word.key }) { item ->
            WordRow(item, store, stats, frequent, open[item.word.key] == true, Modifier.animateItem()) {
                open[item.word.key] = open[item.word.key] != true
            }
        }
        if (knownItems.isNotEmpty()) {
            item(key = "knownToggle") {
                TextButton(onClick = { showKnown = !showKnown }, modifier = Modifier.animateItem()) {
                    Text(if (showKnown) "Hide known words" else "Show known words (${knownItems.size})")
                }
            }
            if (showKnown) items(knownItems, key = { "known:" + it.word.key }) { item ->
                WordRow(item, store, stats, frequent, open[item.word.key] == true, Modifier.animateItem()) {
                    open[item.word.key] = open[item.word.key] != true
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordRow(
    item: DictItem, store: Favourites, stats: WordStatistics, frequent: Boolean, isOpen: Boolean,
    modifier: Modifier, onClick: () -> Unit,
) {
    val w = item.word
    val s = item.stat
    val favourite = store.isSaved(w.key)
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor =
            if (s?.known == true) MaterialTheme.colorScheme.surfaceContainerLowest
            else MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.fillMaxWidth().animateContentSize(),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(w.word, fontSize = 24.sp)
                if (w.reading.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Text(w.reading, color = AppColors.furigana, fontSize = 16.sp)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { store.toggle(w) }, modifier = Modifier.size(36.dp)) {
                    val tint by animateColorAsState(if (favourite) AppColors.furigana else MaterialTheme.colorScheme.outline,
                        label = "star")
                    Icon(Icons.Filled.Star, tint = tint,
                        contentDescription = if (favourite) "Remove from favourites" else "Add to favourites")
                }
            }
            Text(w.meaning, style = MaterialTheme.typography.bodyMedium,
                maxLines = if (isOpen) Int.MAX_VALUE else 2, overflow = TextOverflow.Ellipsis)
            if (s != null) {
                Text("looked up ${s.looked}× · seen ${s.seen}×", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
            }

            AnimatedVisibility(isOpen, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (s != null && s.example.isNotEmpty()) ExampleSentence(s.example, s.exampleSurface)
                    w.entries.forEach { EntryView(it) }
                    if (w.kanji.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            w.kanji.forEach { KanjiBox(it) }
                        }
                    }
                    Row(Modifier.align(Alignment.End)) {
                        if (frequent && s != null) {
                            TextButton(onClick = { stats.setKnown(w.key, !s.known) }) {
                                Icon(Icons.Filled.Check, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(if (s.known) "Not known yet" else "I know this")
                            }
                        }
                        if (!frequent) {
                            TextButton(onClick = { store.toggle(w) }) {
                                Icon(Icons.Filled.Delete, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Remove")
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The last sentence the word appeared in, with the word highlighted. */
@Composable
private fun ExampleSentence(sentence: String, surface: String) {
    val text = buildAnnotatedString {
        val at = if (surface.isNotEmpty()) sentence.indexOf(surface) else -1
        if (at < 0) append(sentence) else {
            append(sentence.substring(0, at))
            withStyle(SpanStyle(color = AppColors.furigana, fontWeight = FontWeight.Bold)) { append(surface) }
            append(sentence.substring(at + surface.length))
        }
    }
    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Text(text, fontSize = 17.sp, modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp))
    }
}
