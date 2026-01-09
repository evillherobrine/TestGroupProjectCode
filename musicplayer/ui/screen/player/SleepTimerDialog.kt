package com.example.musicplayer.ui.screen.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun SleepTimerDialog(onDismiss: () -> Unit, onTimerSet: (Long) -> Unit) {
    val timerOptions = listOf(15, 30, 45, 60)
    var isCustomMode by remember { mutableStateOf(false) }
    var customInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sleep Timer") },
        text = {
            Column {
                if (isCustomMode) {
                    Text("Enter duration (minutes):")
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = customInput,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() }) {
                                customInput = newValue
                            }
                        },
                        label = { Text("Minutes") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text("Stop audio after:")
                    Spacer(Modifier.height(16.dp))
                    timerOptions.forEach { minutes ->
                        Text(
                            text = "$minutes minutes",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onTimerSet(minutes * 60 * 1000L)
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp)
                        )
                    }
                    Text(
                        text = "Custom...",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isCustomMode = true }
                            .padding(vertical = 12.dp, horizontal = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            if (isCustomMode) {
                Row {
                    TextButton(onClick = { isCustomMode = false }) {
                        Text("Back")
                    }
                    TextButton(
                        onClick = {
                            val minutes = customInput.toLongOrNull()
                            if (minutes != null && minutes > 0) {
                                onTimerSet(minutes * 60 * 1000L)
                            }
                        },
                        enabled = customInput.isNotEmpty()
                    ) {
                        Text("Set")
                    }
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}