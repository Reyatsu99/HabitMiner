@file:Suppress("ktlint:standard:function-naming")

package com.habitminer.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.habitminer.engine.HabitUiState

data class SensorInfo(
    val name: String,
    val type: Int,
    val vendor: String,
    val version: Int,
    val power: Float,
    val maxRange: Float,
    val isAvailable: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticScreen(
    state: HabitUiState,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }

    val allSensors =
        remember {
            val requiredTypes =
                listOf(
                    Pair(Sensor.TYPE_ACCELEROMETER, "Accelerometer"),
                    Pair(Sensor.TYPE_GYROSCOPE, "Gyroscope"),
                    Pair(Sensor.TYPE_PROXIMITY, "Proximity"),
                    Pair(Sensor.TYPE_LIGHT, "Ambient Light"),
                    Pair(Sensor.TYPE_STEP_COUNTER, "Step Counter"),
                    Pair(Sensor.TYPE_STEP_DETECTOR, "Step Detector"),
                    Pair(Sensor.TYPE_MAGNETIC_FIELD, "Magnetometer"),
                    Pair(Sensor.TYPE_PRESSURE, "Barometer"),
                    Pair(Sensor.TYPE_RELATIVE_HUMIDITY, "Relative Humidity"),
                    Pair(Sensor.TYPE_AMBIENT_TEMPERATURE, "Ambient Temperature"),
                )

            requiredTypes.map { (type, typeName) ->
                val sensor = sensorManager.getDefaultSensor(type)
                if (sensor != null) {
                    SensorInfo(
                        name = typeName,
                        type = type,
                        vendor = sensor.vendor ?: "Unknown",
                        version = sensor.version,
                        power = sensor.power,
                        maxRange = sensor.maximumRange,
                        isAvailable = true,
                    )
                } else {
                    SensorInfo(
                        name = typeName,
                        type = type,
                        vendor = "N/A",
                        version = 0,
                        power = 0f,
                        maxRange = 0f,
                        isAvailable = false,
                    )
                }
            }.sortedBy { !it.isAvailable }
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sensor Diagnostics") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF0F172A),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                    ),
            )
        },
        containerColor = Color(0xFF0F172A),
    ) { paddingValues ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
        ) {
            item {
                Text(
                    text = "Data Quality",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 16.dp),
                )

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp),
                            )
                            .padding(16.dp),
                ) {
                    Column {
                        Text(
                            "App Usage Records: ${state.usageRecordCount}",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            "Context Snapshots: ${state.contextRecordCount}",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text("Total Days Monitored: ${state.daysOfData}", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            item {
                Text(
                    text = "Hardware Analysis",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }

            items(allSensors) { sensorInfo ->
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp),
                            )
                            .padding(16.dp),
                ) {
                    Column {
                        val icon = if (sensorInfo.isAvailable) "✓" else "✗"
                        val color = if (sensorInfo.isAvailable) Color(0xFF10B981) else Color(0xFFEF4444)

                        Text(
                            text = "$icon ${sensorInfo.name}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = color,
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (sensorInfo.isAvailable) {
                            Text("Vendor: ${sensorInfo.vendor}", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                            Text("Version: ${sensorInfo.version}", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                            Text("Power: ${sensorInfo.power} mA", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                            Text("Max Range: ${sensorInfo.maxRange}", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                        } else {
                            Text("Available: No", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
