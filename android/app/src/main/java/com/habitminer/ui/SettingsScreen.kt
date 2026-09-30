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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.habitminer.engine.HabitUiState
import com.habitminer.engine.HabitViewModel
import kotlinx.coroutines.delay

@Composable
fun SettingsScreen(
    state: HabitUiState,
    viewModel: HabitViewModel,
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

        // Privacy & Data Section
        SettingsSection(title = "Privacy & Local Data") {
            SettingsItem(
                icon = Icons.Default.Security,
                title = "On-Device Processing",
                description =
                    "Your usage, sensor, unlock, and notification summaries stay on this device. Data is never sent to a cloud server.",
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Data Retention",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                listOf(30, 90, 180).forEach { days ->
                    val isSelected = days == state.retentionDays
                    Button(
                        onClick = { viewModel.setRetentionDays(days) },
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                contentColor =
                                    if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                            ),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(if (isSelected) "✓ ${days}d" else "${days}d")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            SettingsAction(
                icon = Icons.Default.DeleteOutline,
                title = "Clear collected data",
                color = MaterialTheme.colorScheme.error,
                onClick = { confirmClearData = true },
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Export Section
        SettingsSection(title = "Data Portability") {
            SettingsItem(
                icon = Icons.Default.Download,
                title = "Export to CSV",
                description = "Export your raw context and usage data for external analysis.",
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.exportDataToCsv() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Export Data Now")
            }

            if (state.exportMessage != null) {
                LaunchedEffect(state.exportMessage) {
                    delay(4000)
                    viewModel.clearExportMessage()
                }
                Text(
                    text = state.exportMessage,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (confirmClearData) {
        AlertDialog(
            onDismissRequest = { confirmClearData = false },
            title = { Text("Clear local data?") },
            text = {
                Text("This will delete all collected app usage, context snapshots, baselines, and models. This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearDatabase()
                        confirmClearData = false
                    },
                ) {
                    Text("Clear Data", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearData = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    description: String,
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 2.dp),
        )
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
fun SettingsAction(
    icon: ImageVector,
    title: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color)
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = color,
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
        }
    }
}
