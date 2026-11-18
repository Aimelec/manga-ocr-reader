package com.mangareader.local.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.mangareader.local.logic.Favourites
import com.mangareader.local.logic.ReaderViewModel
import com.mangareader.local.logic.SettingsViewModel
import com.mangareader.local.logic.WordStatistics
import com.mangareader.local.model.*
import com.mangareader.local.ui.dictionary.DictionaryScreen
import com.mangareader.local.ui.reader.ReaderScreen
import kotlinx.coroutines.launch

private const val DICTIONARY_PAGE = 0
private const val READER_PAGE = 1

/** Two pages side by side: swipe right from the reader to reach the dictionary. */
@Composable
fun AppRoot(reader: ReaderViewModel, store: Favourites, stats: WordStatistics, settings: SettingsViewModel) {
    val pager = rememberPagerState(initialPage = READER_PAGE) { 2 }
    val scope = rememberCoroutineScope()
    var showSettings by remember { mutableStateOf(false) }

    // registered before the pages so the dictionary's own back handler (category -> list) wins
    BackHandler(enabled = pager.currentPage == DICTIONARY_PAGE) {
        scope.launch { pager.animateScrollToPage(READER_PAGE) }
    }

    HorizontalPager(pager, Modifier.fillMaxSize().background(Color.Black), beyondViewportPageCount = 1) { page ->
        if (page == DICTIONARY_PAGE) {
            DictionaryScreen(store, stats,
                onCamera = { scope.launch { pager.animateScrollToPage(READER_PAGE) } },
                onSettings = { showSettings = true })
        } else {
            ReaderScreen(reader, store, onDictionary = { scope.launch { pager.animateScrollToPage(DICTIONARY_PAGE) } })
        }
    }

    if (showSettings) SettingsDialog(settings) { showSettings = false }
}
