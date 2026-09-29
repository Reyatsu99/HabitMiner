@file:Suppress("ktlint:standard:function-naming")

package com.habitminer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.habitminer.engine.HabitUiState
import com.habitminer.engine.HabitViewModel

@Composable
fun SettingsScreen(
    state: HabitUiState,
    viewModel: HabitViewModel,
    onNavigateToDiagnostics: () -> Unit = {},
) {
    var confirmClearData by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Data & Privacy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Your usage, sensor, unlock, and notification summaries stay on this device. Raw audio is never recorded.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                )
                Text(
                    "Keep data for ${state.retentionDays} days",
                    style = MaterialTheme.typography.labelLarge,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(30, 90, 180).forEach { days ->
                        TextButton(onClick = { viewModel.setRetentionDays(days) }) {
                            Text(if (days == state.retentionDays) "✓ ${days}d" else "${days}d")
                        }
                    }
                }
                TextButton(onClick = { confirmClearData = true }) {
                    Text("Clear collected data", color = MaterialTheme.colorScheme.error)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.exportDataToCsv() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Export Data to CSV")
                }

                if (state.exportMessage != null) {
                    Text(
                        text = state.exportMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Diagnostics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "View active hardware sensors on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                )
                TextButton(onClick = onNavigateToDiagnostics) {
                    Text("Hardware Sensor Diagnostics")
                }
            }
        }
    }

    if (confirmClearData) {
        AlertDialog(
            onDismissRequest = { confirmClearData = false },
            title = { Text("Clear local data?") },
            text = {
                Text(
                    "This removes the usage, context, unlock, notification, habit, " +
                        "baseline, and deviation data stored by HabitMiner on this device.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClearData = false
                        viewModel.clearCollectedData()
                    },
                ) { Text("Clear", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmClearData = false }) { Text("Cancel") } },
        )
    }
}
