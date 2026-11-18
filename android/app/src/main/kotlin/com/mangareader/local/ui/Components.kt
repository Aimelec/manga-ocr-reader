package com.mangareader.local.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangareader.local.model.*
import com.mangareader.local.ui.AppColors
import com.mangareader.local.ui.EntryView
import com.mangareader.local.ui.KanjiBox
import com.mangareader.local.ui.SectionTitle

// Pieces shared by the reader and dictionary pages.

@Composable
internal fun SectionTitle(text: String, modifier: Modifier = Modifier, hint: String? = null) {
    Row(modifier.padding(top = 8.dp, start = 4.dp), verticalAlignment = Alignment.Bottom) {
        Text(text.uppercase(), style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
        if (hint != null) {
            Spacer(Modifier.width(8.dp))
            Text(hint, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
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
