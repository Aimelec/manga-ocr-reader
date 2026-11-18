package com.mangareader.local.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.mangareader.local.logic.SettingsViewModel

@Composable
internal fun SettingsDialog(settings: SettingsViewModel, onClose: () -> Unit) {
    var address by remember { mutableStateOf(settings.address) }
    var code by remember { mutableStateOf(settings.pairingCode) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Connect to the PC") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Both are shown in the server window when it starts.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(value = address, onValueChange = { address = it }, singleLine = true,
                    label = { Text("PC address") }, placeholder = { Text("192.168.1.100:8765") })
                OutlinedTextField(value = code, onValueChange = { code = it }, singleLine = true,
                    label = { Text("Pairing code") }, placeholder = { Text("ABCD2345") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters))
                TextButton(onClick = {
                    settings.trustCertificateAgain()
                    onClose()
                }) { Text("Trust the PC's certificate again") }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                settings.save(address, code)
                onClose()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Cancel") } },
    )
}
