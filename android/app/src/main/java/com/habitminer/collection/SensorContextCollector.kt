package com.habitminer.collection

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.habitminer.data.ContextSnapshotEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.math.sqrt

@Singleton
class SensorContextCollector
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        suspend fun collectSnapshot(
            unlockCount: Int,
            isScreenOn: Boolean,
            notificationCount: Int,
            collectSensors: Boolean,
        ): ContextSnapshotEntity {
            val timestamp = System.currentTimeMillis()

            // Keep unavailable sensor readings distinct from real darkness / stillness.
            val lightLevel = if (collectSensors) collectLightLevel() ?: -1f else -1f
            val motionState = if (collectSensors) collectMotionState() ?: "UNKNOWN" else "UNKNOWN"

            return ContextSnapshotEntity(
                timestamp = timestamp,
                motionState = motionState,
                lightLevel = lightLevel,
                // Battery handled in Worker
                batteryLevel = -1,
                isCharging = false,
                isScreenOn = isScreenOn,
                unlockCount = unlockCount,
                notificationCount = notificationCount,
            )
        }

        private suspend fun collectLightLevel(): Float? =
            withTimeoutOrNull(1000L) {
                suspendCancellableCoroutine { continuation ->
                    val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
                    if (lightSensor == null) {
                        continuation.resume(null)
                        return@suspendCancellableCoroutine
                    }

                    val listener =
                        object : SensorEventListener {
                            override fun onSensorChanged(event: SensorEvent?) {
                                if (event?.sensor?.type == Sensor.TYPE_LIGHT) {
                                    sensorManager.unregisterListener(this)
                                    if (continuation.isActive) {
                                        continuation.resume(event.values[0])
                                    }
                                }
                            }

                            override fun onAccuracyChanged(
                                sensor: Sensor?,
                                accuracy: Int,
                            ) {}
                        }

                    val registered = sensorManager.registerListener(listener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
                    if (!registered) {
                        continuation.resume(null)
                        return@suspendCancellableCoroutine
                    }
                    continuation.invokeOnCancellation {
                        sensorManager.unregisterListener(listener)
                    }
                }
            }

        private suspend fun collectMotionState(): String? =
            withTimeoutOrNull(1000L) {
                suspendCancellableCoroutine { continuation ->
                    val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                    if (accelSensor == null) {
                        continuation.resume(null)
                        return@suspendCancellableCoroutine
                    }

                    val samples = mutableListOf<Float>()

                    val listener =
                        object : SensorEventListener {
                            override fun onSensorChanged(event: SensorEvent?) {
                                if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
                                    val x = event.values[0]
                                    val y = event.values[1]
                                    val z = event.values[2]
                                    val magnitude = sqrt(x * x + y * y + z * z)
                                    samples.add(magnitude)

                                    if (samples.size >= 12) {
                                        sensorManager.unregisterListener(this)
                                        if (continuation.isActive) {
                                            val mean = samples.average().toFloat()
                                            var variance = 0f
                                            for (v in samples) {
                                                variance += (v - mean) * (v - mean)
                                            }
                                            variance /= samples.size

                                            val state =
                                                when {
                                                    variance < 0.5f -> "STILL"
                                                    variance < 2.0f -> "WALKING"
                                                    else -> "ACTIVE"
                                                }
                                            continuation.resume(state)
                                        }
                                    }
                                }
                            }

                            override fun onAccuracyChanged(
                                sensor: Sensor?,
                                accuracy: Int,
                            ) {}
                        }

                    val registered = sensorManager.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_NORMAL)
                    if (!registered) {
                        continuation.resume(null)
                        return@suspendCancellableCoroutine
                    }
                    continuation.invokeOnCancellation {
                        sensorManager.unregisterListener(listener)
                    }
                }
            }
    }
