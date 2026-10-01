@file:Suppress("ktlint:standard:function-naming")

package com.habitminer.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.habitminer.data.DeviationEntity
import com.habitminer.data.DiscoveredHabitEntity
import com.habitminer.engine.HabitUiState
import com.habitminer.ui.components.InfoCard
import com.habitminer.ui.components.LoadingState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InsightsScreen(state: HabitUiState) {
    if (!state.hasEnoughData && state.recentDeviations.isEmpty() && state.discoveredHabits.isEmpty()) {
        LoadingState(
            message = "Building your habit model...\nCurrently at ${state.daysOfData}/5 days of required data.",
        )
        return
    }

    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Insights",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            SectionHeader("Routine predictability")
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 16.dp)) {
                    PredictabilityGauge(score = state.predictabilityScore)
                    Spacer(modifier = Modifier.height(16.dp))
                    val label =
                        when {
                            state.predictabilityScore > 70f -> "Highly Regular"
                            state.predictabilityScore > 40f -> "Moderately Regular"
                            else -> "Variable"
                        }
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "[Experimental]",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    )
                }
            }
        }

        item {
            SectionHeader("DISCOVERED PATTERNS")
        }

        if (state.discoveredHabits.isEmpty()) {
            item {
                InfoCard(
                    title = "No strong routines yet",
                    message = "Your usage varies too much to form a highly confident routine. Keep using your phone normally.",
                )
            }
        } else {
            val visibleHabits = state.discoveredHabits.take(4)
            items(visibleHabits, key = { it.id }) { habit ->
                TypographicHabitItem(habit = habit)
                Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 12.dp))
            }
            if (state.discoveredHabits.size > 4) {
                item {
                    Text(
                        text = "${state.discoveredHabits.size} patterns discovered  ·  View all →",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }
        }

        item {
            SectionHeader("RECENT DEVIATIONS")
        }

        if (state.recentDeviations.isEmpty()) {
            item {
                InfoCard(
                    title = "All Good!",
                    message = "No recent deviations from your established baseline.",
                )
            }
        } else {
            val visibleDeviations = state.recentDeviations.take(4)
            items(visibleDeviations, key = { it.id + 100000L }) { dev ->
                TypographicDeviationItem(dev = dev)
                Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 12.dp))
            }
            if (state.recentDeviations.size > 4) {
                item {
                    Text(
                        text = "${state.recentDeviations.size} recent deviations  ·  View all →",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun SectionHeader(title: String) {
    Column {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        )
        Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))
    }
}

@Composable
fun TypographicHabitItem(habit: DiscoveredHabitEntity) {
    Column {
        Text(
            text = habit.habitName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${habit.timeSlot} · ${habit.dayType} · ${(habit.confidence * 100).toInt()}% match",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = habit.patternDescription,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
fun TypographicDeviationItem(dev: DeviationEntity) {
    val df = SimpleDateFormat("MMM d · HH:mm", Locale.getDefault())
    val dateStr = df.format(Date(dev.timestamp))
    val isHighImpact = dev.normalizedScore > 0.7f
    val impactColor = if (isHighImpact) MaterialTheme.colorScheme.error else Color(0xFFF59E0B)

    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = dateStr,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            )
            Text(
                text = dev.deviationType.replace('_', ' '),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = impactColor,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = dev.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
fun PredictabilityGauge(score: Float) {
    val color =
        when {
            score > 70f -> Color(0xFF10B981)
            score > 40f -> Color(0xFFF59E0B)
            else -> Color(0xFFEF4444)
        }

    val gaugeDescription = "Predictability gauge: ${score.toInt()}%"
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .size(160.dp)
                .semantics { contentDescription = gaugeDescription },
    ) {
        Canvas(modifier = Modifier.size(160.dp)) {
            drawArc(
                color = color.copy(alpha = 0.2f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round),
            )
            drawArc(
                color = color,
                startAngle = 135f,
                sweepAngle = 270f * (score / 100f).coerceIn(0f, 1f),
                useCenter = false,
                style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${score.toInt()}%",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Score",
                style = MaterialTheme.typography.labelMedium,
                color = color,
            )
        }
    }
}
