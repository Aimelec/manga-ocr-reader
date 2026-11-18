package com.mangareader.local.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.mangareader.local.logic.SettingsViewModel
import com.mangareader.local.model.*

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
