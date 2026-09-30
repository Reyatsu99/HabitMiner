@file:Suppress("ktlint:standard:function-naming")

package com.habitminer.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.habitminer.data.AppUsageEntity
import com.habitminer.data.ContextSnapshotEntity
import com.habitminer.engine.HabitUiState
import com.habitminer.ui.components.EmptyState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class TimelineItemData {
    abstract val timestamp: Long

    data class Usage(val entity: AppUsageEntity) : TimelineItemData() {
        override val timestamp: Long = entity.endTime
    }

    data class Snapshot(val entity: ContextSnapshotEntity) : TimelineItemData() {
        override val timestamp: Long = entity.timestamp
    }
}

@Composable
fun HistoryScreen(state: HabitUiState) {
    if (state.todayAppUsage.isEmpty()) {
        EmptyState(
            title = "No Activity Yet",
            message =
                "We haven't recorded any significant non-launcher app usage today. " +
                    "Your timeline will appear here once you start using your apps.",
        )
        return
    }

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val mergedTimeline =
        remember(state.todayAppUsage, state.todaySnapshots) {
            val list = mutableListOf<TimelineItemData>()
            state.todayAppUsage.forEach { list.add(TimelineItemData.Usage(it)) }
            state.todaySnapshots.forEach { list.add(TimelineItemData.Snapshot(it)) }
            list.sortedBy { it.timestamp }
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Today",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
        ) {
            itemsIndexed(mergedTimeline, key = { index, item ->
                when (item) {
                    is TimelineItemData.Usage -> "u_${item.entity.id}"
                    is TimelineItemData.Snapshot -> "s_${item.entity.id}"
                }
            }) { index, item ->
                val isLast = index == mergedTimeline.lastIndex

                when (item) {
                    is TimelineItemData.Usage -> TimelineNode(item.entity, timeFormat, isLast)
                    is TimelineItemData.Snapshot -> SnapshotNode(item.entity, isLast)
                }
            }
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun TimelineNode(
    usage: AppUsageEntity,
    timeFormat: SimpleDateFormat,
    isLast: Boolean,
) {
    val durationMins = (usage.durationMs / 60000).coerceAtLeast(1)
    val timeStr = timeFormat.format(Date(usage.endTime))

    Row(modifier = Modifier.fillMaxWidth()) {
        // Time column
        Text(
            text = timeStr,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.width(48.dp).padding(top = 4.dp),
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Node & Line column
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Divider(
                    modifier = Modifier.width(16.dp),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                )
                Box(
                    modifier =
                        Modifier
                            .size(12.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                )
            }

            if (!isLast) {
                Box(
                    modifier =
                        Modifier
                            .width(2.dp)
                            .height(56.dp)
                            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)),
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Content column
        Column(modifier = Modifier.padding(top = 2.dp)) {
            Text(
                text = usage.appName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$durationMins min",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
            if (!isLast) {
                Spacer(modifier = Modifier.height(36.dp))
            }
        }
    }
}

@Composable
fun SnapshotNode(
    snapshot: ContextSnapshotEntity,
    isLast: Boolean,
) {
    val motion =
        if (snapshot.accelVariance < 0.5f) {
            "Low motion"
        } else if (snapshot.accelVariance < 2.0f) {
            "Moderate motion"
        } else {
            "High motion"
        }
    val light =
        if (snapshot.lightLux > 100) {
            "Bright"
        } else if (snapshot.lightLux > 10) {
            "Dim"
        } else {
            "Dark"
        }
    val batteryText = if (snapshot.batteryLevel < 0) "" else " · ${snapshot.batteryLevel}% battery"

    Row(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.width(56.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier =
                    Modifier
                        .width(2.dp)
                        .height(32.dp)
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)),
            )
            if (!isLast) {
                Box(
                    modifier =
                        Modifier
                            .width(2.dp)
                            .height(32.dp)
                            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)),
                )
            }
        }

        Spacer(modifier = Modifier.width(24.dp))

        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Text(
                text = "──── Context ────",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$motion · ${snapshot.lightLux.toInt()} lux$batteryText",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
        }
    }
}
