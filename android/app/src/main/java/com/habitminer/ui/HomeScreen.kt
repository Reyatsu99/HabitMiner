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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.habitminer.engine.HabitUiState
import com.habitminer.engine.HabitViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    state: HabitUiState,
    viewModel: HabitViewModel,
) {
    val scrollState = rememberScrollState()
    var confirmClearData by remember { mutableStateOf(false) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header
        Column {
            Text(
                text = "HabitMiner",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date()),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )
        }

        // Context Strip
        state.latestContext?.let { ctx ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ContextChip(
                    icon =
                        when (ctx.motionState) {
                            "STILL" -> "🧍"
                            "WALKING" -> "🚶"
                            "UNKNOWN" -> "❔"
                            else -> "🏃"
                        },
                    text = ctx.motionState,
                )
                ContextChip(
                    icon = "🔋",
                    text = "${ctx.batteryLevel}%",
                )
                ContextChip(
                    icon =
                        if (ctx.lightLevel < 0) {
                            "❔"
                        } else if (ctx.lightLevel > 100) {
                            "☀️"
                        } else if (ctx.lightLevel > 10) {
                            "🌙"
                        } else {
                            "🌑"
                        },
                    text =
                        if (ctx.lightLevel < 0) {
                            "Unavailable"
                        } else if (ctx.lightLevel > 100) {
                            "Bright"
                        } else if (ctx.lightLevel > 10) {
                            "Dim"
                        } else {
                            "Dark"
                        },
                )
                if (state.hasNotificationPermission && ctx.notificationCount >= 0) {
                    ContextChip(
                        icon = "🔔",
                        text = "${ctx.notificationCount}",
                    )
                }
            }
        }

        // Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val hours = state.todayScreenTimeMs / (1000 * 60 * 60)
            val minutes = (state.todayScreenTimeMs / (1000 * 60)) % 60
            StatCard(
                title = "Screen Time",
                value = "${hours}h ${minutes}m",
                modifier = Modifier.weight(1f),
            )
            val devColor =
                when {
                    state.overallDeviationScore > 0.7f -> Color.Red
                    state.overallDeviationScore > 0.4f -> Color(0xFFF59E0B)
                    else -> Color.Green
                }
            StatCard(
                title = "Deviation",
                value = "${(state.overallDeviationScore * 100).toInt()}%",
                valueColor = devColor,
                modifier = Modifier.weight(1f),
            )
        }

        Text(
            text = "${state.todayUnlocks} unlocks today",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        // Top App
        if (state.todayTopApp.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Top App Today",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = state.todayTopApp,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        // Deviation Alert
        if (state.overallDeviationScore > 0.4f && state.todayDeviations.isNotEmpty()) {
            val dev = state.todayDeviations.maxByOrNull { it.normalizedScore }
            if (dev != null) {
                Card(
                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                if (dev.normalizedScore > 0.7f) {
                                    MaterialTheme.colorScheme.errorContainer
                                } else {
                                    Color(0x33F59E0B)
                                },
                        ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint =
                                if (dev.normalizedScore > 0.7f) {
                                    MaterialTheme.colorScheme.onErrorContainer
                                } else {
                                    Color(0xFFF59E0B)
                                },
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Deviation Detected",
                                fontWeight = FontWeight.Bold,
                                color =
                                    if (dev.normalizedScore > 0.7f) {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    } else {
                                        Color(0xFFF59E0B)
                                    },
                            )
                            Text(
                                text = dev.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }

        // Predictions
        if (state.predictions.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Next Likely Activity",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val top = state.predictions.first()
                    Text(
                        text = "Based on your routine → ${top.appName} (${(top.confidence * 100).toInt()}%)",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        // Data Status
        Text(
            text = state.baselineStatus,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        if (state.isSyncing) {
            Text(
                text = "Syncing your on-device data…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        } else {
            Button(
                onClick = { viewModel.loadHistoricalData() },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text("Sync Usage")
            }
        }

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
            }
        }
    }

    if (confirmClearData) {
        AlertDialog(
            onDismissRequest = { confirmClearData = false },
            title = { Text("Clear local data?") },
            text = { Text("This removes the usage, context, unlock, notification, habit, baseline, and deviation data stored by HabitMiner on this device.") },
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

@Composable
fun ContextChip(
    icon: String,
    text: String,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = icon, modifier = Modifier.padding(end = 4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = valueColor,
            )
        }
    }
}
