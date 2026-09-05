package com.habitminer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.habitminer.engine.HabitEngine
import com.habitminer.engine.HabitUiState
import com.habitminer.engine.POISummary

val clusterColors = listOf(
    Color(0xFF38BDF8), Color(0xFF4ADE80), Color(0xFFF59E0B),
    Color(0xFFC084FC), Color(0xFFF87171), Color(0xFF67E8F9)
)

@Composable
fun InsightsScreen(state: HabitUiState) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 20.dp)
    ) {
        item {
            Text(
                "Routine Insights",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        // Predictability Score Card
        item {
            PredictabilityCard(score = state.predictabilityScore, pointCount = state.pointCount)
        }

        // Discovered Places
        if (state.poiSummaries.isNotEmpty()) {
            item {
                Text(
                    "Discovered Places",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.SemiBold
                )
            }
            items(state.poiSummaries) { poi ->
                POICard(poi)
            }
        } else {
            item {
                EmptyInsightsCard()
            }
        }

        // Behavioral Timeline
        if (state.habitEvents.isNotEmpty()) {
            item {
                Text(
                    "Behavioral Timeline",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.SemiBold
                )
            }
            items(state.habitEvents.takeLast(10).reversed()) { event ->
                val poiLabel = if (event.clusterId >= 0) {
                    state.poiSummaries.find { it.clusterId == event.clusterId }?.label ?: "Unknown Place"
                } else "In Transit"
                
                HabitEventRow(
                    emoji = HabitEngine.getHabitEmoji(event.habitType),
                    title = HabitEngine.getHabitName(event.habitType),
                    subtitle = poiLabel,
                    durationMin = event.durationSeconds / 60
                )
            }
        } else if (state.stayPoints.isNotEmpty()) {
            item {
                Text(
                    "Recent Stay Points (${state.stayPoints.size} detected)",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.SemiBold
                )
            }
            items(state.stayPoints.takeLast(10).reversed()) { sp ->
                StayPointRow(
                    label = state.poiSummaries.find { it.clusterId == sp.clusterId }?.label ?: "Unknown Place",
                    durationMin = sp.durationSeconds / 60,
                    clusterId = sp.clusterId
                )
            }
        }
    }
}

@Composable
fun PredictabilityCard(score: Int, pointCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Routine Predictability", color = Color(0xFF94A3B8), fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { score / 100f },
                        modifier = Modifier.size(72.dp),
                        color = when {
                            score >= 70 -> Color(0xFF4ADE80)
                            score >= 40 -> Color(0xFFF59E0B)
                            else        -> Color(0xFFF87171)
                        },
                        trackColor = Color(0xFF334155),
                        strokeWidth = 7.dp
                    )
                    Text(
                        "$score%",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Spacer(Modifier.width(20.dp))
                Column {
                    val description = when {
                        score >= 70 -> "Highly Regular\nYour routine is very predictable"
                        score >= 40 -> "Moderately Regular\nSome variation in patterns"
                        score > 0   -> "Highly Variable\nExploring diverse locations"
                        else        -> "Not enough data yet"
                    }
                    Text(description, color = Color(0xFFCBD5E1), fontSize = 13.sp, lineHeight = 18.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("$pointCount GPS points analysed", color = Color(0xFF64748B), fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun POICard(poi: POISummary) {
    val color = clusterColors.getOrElse(poi.clusterId % clusterColors.size) { Color.White }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text("📍", fontSize = 18.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(poi.label, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(
                    "${poi.visitCount} visit${if (poi.visitCount > 1) "s" else ""} · ${poi.totalDurationMin} min total",
                    color = Color(0xFF94A3B8), fontSize = 12.sp
                )
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("POI ${poi.clusterId}", color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun StayPointRow(label: String, durationMin: Long, clusterId: Int) {
    val color = clusterColors.getOrElse(clusterId % clusterColors.size) { Color.White }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(50))
                .background(color)
        )
        Spacer(Modifier.width(12.dp))
        Text(label, color = Color(0xFFCBD5E1), fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text("${durationMin}m", color = Color(0xFF64748B), fontSize = 12.sp)
    }
}

@Composable
fun HabitEventRow(emoji: String, title: String, subtitle: String, durationMin: Long) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF334155)),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 18.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(
                    "$subtitle · ${durationMin}m",
                    color = Color(0xFF94A3B8), fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun EmptyInsightsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            Modifier.padding(32.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🔍", fontSize = 36.sp)
            Spacer(Modifier.height(12.dp))
            Text("No places discovered yet", color = Color(0xFF94A3B8), fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Collect at least 15 minutes of GPS data\nat a location to detect a stay point",
                color = Color(0xFF64748B), fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
