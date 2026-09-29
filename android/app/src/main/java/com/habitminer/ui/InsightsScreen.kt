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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.habitminer.engine.HabitUiState

@Composable
fun InsightsScreen(state: HabitUiState) {
    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Wellness Insights",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
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
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Routine Predictability",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
            }
        }

        item {
            Text(
                text = "Recent Deviations",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        if (state.todayDeviations.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "✅  No deviations from your baseline today.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(20.dp),
                    )
                }
            }
        } else {
            items(state.todayDeviations, key = { it.id }) { dev ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = dev.deviationType.replace('_', ' '),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            val impactColor = if (dev.normalizedScore > 0.7f) Color(0xFFEF4444) else Color(0xFFF59E0B)
                            Text(
                                text = "${(dev.normalizedScore * 100).toInt()}% Impact",
                                style = MaterialTheme.typography.labelMedium,
                                color = impactColor,
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dev.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dev.timeBin.replace('_', ' '),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
fun PredictabilityGauge(score: Float) {
    val color =
        when {
            score > 70f -> Color(0xFF4ADE80)
            score > 40f -> Color(0xFFF59E0B)
            else -> Color(0xFFEF4444)
        }

    val gaugeDescription = "Predictability gauge: ${score.toInt()}%"
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .size(140.dp)
                .semantics { contentDescription = gaugeDescription },
    ) {
        Canvas(modifier = Modifier.size(140.dp)) {
            drawArc(
                color = color.copy(alpha = 0.2f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round),
            )
            drawArc(
                color = color,
                startAngle = 135f,
                sweepAngle = 270f * (score / 100f).coerceIn(0f, 1f),
                useCenter = false,
                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${score.toInt()}%",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Score",
                style = MaterialTheme.typography.labelSmall,
                color = color,
            )
        }
    }
}
