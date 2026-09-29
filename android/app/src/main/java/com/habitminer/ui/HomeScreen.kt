@file:Suppress("ktlint:standard:function-naming")

package com.habitminer.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.habitminer.data.ContextSnapshotEntity
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

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
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

        // Hero Context Banner
        state.latestContext?.let { ctx ->
            ContextHeroBanner(
                context = ctx,
                hasNotificationPermission = state.hasNotificationPermission,
            )
        }

        // Actionable Insights Section
        val hasDeviations = state.overallDeviationScore > 0.4f && state.todayDeviations.isNotEmpty()
        val hasPredictions = state.predictions.isNotEmpty()

        if (hasDeviations || hasPredictions) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Actionable Insights",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                if (hasDeviations) {
                    val dev = state.todayDeviations.maxByOrNull { it.normalizedScore }
                    if (dev != null) {
                        ActionableInsightCard(
                            title = "Deviation Detected",
                            description = dev.description,
                            icon = Icons.Default.Warning,
                            isCritical = dev.normalizedScore > 0.7f,
                        )
                    }
                }

                if (hasPredictions) {
                    val top = state.predictions.first()
                    ActionableInsightCard(
                        title = "Next Likely Activity",
                        description = "Based on your routine → ${top.appName} (${(top.confidence * 100).toInt()}%)",
                        icon = Icons.Default.AutoAwesome,
                        isCritical = false,
                        accentColor = Color(0xFF38BDF8),
                    )
                }
            }
        }

        // Daily Summary Grid
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "Daily Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            DailySummaryGrid(
                screenTimeMs = state.todayScreenTimeMs,
                unlocks = state.todayUnlocks,
                topApp = state.todayTopApp,
                baselineStatus = state.baselineStatus,
            )
        }

        // Sync Status
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
    }
}

@Composable
fun ContextHeroBanner(
    context: ContextSnapshotEntity,
    hasNotificationPermission: Boolean,
) {
    val motion =
        when (context.motionState) {
            "STILL" -> "🧍 Still"
            "WALKING" -> "🚶 Walking"
            "UNKNOWN" -> "❔ Unknown"
            else -> "🏃 Active"
        }
    val light =
        if (context.lightLevel < 0) {
            "❔ Unavailable"
        } else if (context.lightLevel > 100) {
            "☀️ Bright"
        } else if (context.lightLevel > 10) {
            "🌙 Dim"
        } else {
            "🌑 Dark"
        }
    val notifications = if (hasNotificationPermission && context.notificationCount >= 0) " • 🔔 ${context.notificationCount}" else ""

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(24.dp),
                )
                .padding(20.dp),
    ) {
        Column {
            Text(
                text = "CURRENT CONTEXT",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$motion • 🔋 ${context.batteryLevel}% • $light$notifications",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
fun ActionableInsightCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isCritical: Boolean,
    accentColor: Color? = null,
) {
    val containerColor by animateColorAsState(
        targetValue = if (isCritical) MaterialTheme.colorScheme.errorContainer else (accentColor?.copy(alpha = 0.15f) ?: Color(0x33F59E0B)),
        label = "containerColor",
    )
    val contentColor by animateColorAsState(
        targetValue = if (isCritical) MaterialTheme.colorScheme.onErrorContainer else (accentColor ?: Color(0xFFF59E0B)),
        label = "contentColor",
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = contentColor,
                modifier = Modifier.size(32.dp),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
fun DailySummaryGrid(
    screenTimeMs: Long,
    unlocks: Int,
    topApp: String,
    baselineStatus: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Screen Time Ring
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Screen Time",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                Spacer(modifier = Modifier.height(16.dp))

                val hours = screenTimeMs / (1000 * 60 * 60)
                val minutes = (screenTimeMs / (1000 * 60)) % 60

                // Mock target of 4 hours for the progress ring
                val progress = (screenTimeMs.toFloat() / (4 * 60 * 60 * 1000)).coerceIn(0f, 1f)
                val progressColor = if (progress > 0.8f) Color(0xFFF59E0B) else Color(0xFF38BDF8)

                Box(contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(100.dp)) {
                        drawArc(
                            color = progressColor.copy(alpha = 0.2f),
                            startAngle = 270f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round),
                        )
                        drawArc(
                            color = progressColor,
                            startAngle = 270f,
                            sweepAngle = 360f * progress,
                            useCenter = false,
                            style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round),
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${hours}h",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "${minutes}m",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                }
            }
        }

        // Unlocks & Top App
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "Unlocks",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$unlocks",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "Top App",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = topApp.ifEmpty { "None" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                }
            }
        }
    }

    Text(
        text = baselineStatus,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}
