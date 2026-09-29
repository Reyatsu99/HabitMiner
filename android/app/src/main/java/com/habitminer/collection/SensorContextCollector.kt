package com.habitminer.collection

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import com.habitminer.data.ContextSnapshotEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.math.sqrt

data class MotionStats(
    val mean: Float,
    val variance: Float,
    val std: Float,
    val min: Float,
    val max: Float,
    val energy: Float,
)

@Singleton
class SensorContextCollector
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        private val prefs = context.getSharedPreferences("sensor_prefs", Context.MODE_PRIVATE)

        suspend fun collectSnapshot(
            unlockCount: Int,
            isScreenOn: Boolean,
            notificationsLastHour: Int,
            collectSensors: Boolean,
            batteryLevel: Int,
            isCharging: Boolean,
        ): ContextSnapshotEntity {
            val timestamp = System.currentTimeMillis()

            // Keep unavailable sensor readings distinct from real darkness / stillness.
            val lightLux = if (collectSensors) collectLightLevel() ?: -1f else -1f
            val accelStats = if (collectSensors) collectMotionState(Sensor.TYPE_ACCELEROMETER) else null
            val gyroStats = if (collectSensors) collectMotionState(Sensor.TYPE_GYROSCOPE) else null
            val proximityNear = if (collectSensors) collectProximityState() else null
            val stepsDelta = if (collectSensors) collectStepDelta() else -1

            return ContextSnapshotEntity(
                timestamp = timestamp,
                accelMean = accelStats?.mean ?: -1f,
                accelVariance = accelStats?.variance ?: -1f,
                accelStd = accelStats?.std ?: -1f,
                accelMin = accelStats?.min ?: -1f,
                accelMax = accelStats?.max ?: -1f,
                accelEnergy = accelStats?.energy ?: -1f,
                gyroMean = gyroStats?.mean ?: -1f,
                gyroVariance = gyroStats?.variance ?: -1f,
                gyroStd = gyroStats?.std ?: -1f,
                gyroMin = gyroStats?.min ?: -1f,
                gyroMax = gyroStats?.max ?: -1f,
                gyroEnergy = gyroStats?.energy ?: -1f,
                lightLux = lightLux,
                proximityNear = proximityNear,
                stepsSinceLastSnapshot = stepsDelta,
                batteryLevel = batteryLevel,
                isCharging = isCharging,
                isScreenOn = isScreenOn,
                unlockCount = unlockCount,
                notificationsLastHour = notificationsLastHour,
            )
        }

        private suspend fun collectLightLevel(): Float? =
            withTimeoutOrNull(2000L) {
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

                    val handler = Handler(Looper.getMainLooper())
                    val registered = sensorManager.registerListener(listener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL, handler)
                    if (!registered) {
                        continuation.resume(null)
                        return@suspendCancellableCoroutine
                    }
                    continuation.invokeOnCancellation {
                        sensorManager.unregisterListener(listener)
                    }
                }
            }

        private suspend fun collectMotionState(sensorType: Int): MotionStats? =
            withTimeoutOrNull(2000L) {
                suspendCancellableCoroutine { continuation ->
                    val sensor = sensorManager.getDefaultSensor(sensorType)
                    if (sensor == null) {
                        continuation.resume(null)
                        return@suspendCancellableCoroutine
                    }

                    val samples = mutableListOf<Float>()

                    val listener =
                        object : SensorEventListener {
                            override fun onSensorChanged(event: SensorEvent?) {
                                if (event?.sensor?.type == sensorType) {
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
                                            val std = sqrt(variance)
                                            val min = samples.minOrNull() ?: 0f
                                            val max = samples.maxOrNull() ?: 0f
                                            val energy = samples.map { it * it }.average().toFloat()

                                            val stats =
                                                MotionStats(
                                                    mean = mean,
                                                    variance = variance,
                                                    std = std,
                                                    min = min,
                                                    max = max,
                                                    energy = energy,
                                                )
                                            continuation.resume(stats)
                                        }
                                    }
                                }
                            }

                            override fun onAccuracyChanged(
                                sensor: Sensor?,
                                accuracy: Int,
                            ) {}
                        }

                    val handler = Handler(Looper.getMainLooper())
                    val registered = sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME, handler)
                    if (!registered) {
                        continuation.resume(null)
                        return@suspendCancellableCoroutine
                    }
                    continuation.invokeOnCancellation {
                        sensorManager.unregisterListener(listener)
                    }
                }
            }

        private suspend fun collectProximityState(): Boolean? =
            withTimeoutOrNull(2000L) {
                suspendCancellableCoroutine { continuation ->
                    val proxSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
                    if (proxSensor == null) {
                        continuation.resume(null)
                        return@suspendCancellableCoroutine
                    }

                    val listener =
                        object : SensorEventListener {
                            override fun onSensorChanged(event: SensorEvent?) {
                                if (event?.sensor?.type == Sensor.TYPE_PROXIMITY) {
                                    sensorManager.unregisterListener(this)
                                    if (continuation.isActive) {
                                        val distance = event.values[0]
                                        val isNear = distance < proxSensor.maximumRange
                                        continuation.resume(isNear)
                                    }
                                }
                            }

                            override fun onAccuracyChanged(
                                sensor: Sensor?,
                                accuracy: Int,
                            ) {}
                        }

                    val handler = Handler(Looper.getMainLooper())
                    val registered = sensorManager.registerListener(listener, proxSensor, SensorManager.SENSOR_DELAY_NORMAL, handler)
                    if (!registered) {
                        continuation.resume(null)
                        return@suspendCancellableCoroutine
                    }
                    continuation.invokeOnCancellation {
                        sensorManager.unregisterListener(listener)
                    }
                }
            }

        private suspend fun collectStepDelta(): Int =
            withTimeoutOrNull(2000L) {
                suspendCancellableCoroutine { continuation ->
                    val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
                    if (stepSensor == null) {
                        continuation.resume(-1)
                        return@suspendCancellableCoroutine
                    }

                    val listener =
                        object : SensorEventListener {
                            override fun onSensorChanged(event: SensorEvent?) {
                                if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
                                    sensorManager.unregisterListener(this)
                                    if (continuation.isActive) {
                                        val currentSteps = event.values[0].toInt()
                                        val lastSteps = prefs.getInt("last_step_count", -1)

                                        prefs.edit().putInt("last_step_count", currentSteps).apply()

                                        if (lastSteps == -1 || currentSteps < lastSteps) {
                                            // Reboot or first time
                                            continuation.resume(currentSteps)
                                        } else {
                                            continuation.resume(currentSteps - lastSteps)
                                        }
                                    }
                                }
                            }

                            override fun onAccuracyChanged(
                                sensor: Sensor?,
                                accuracy: Int,
                            ) {}
                        }

                    val handler = Handler(Looper.getMainLooper())
                    val registered = sensorManager.registerListener(listener, stepSensor, SensorManager.SENSOR_DELAY_NORMAL, handler)
                    if (!registered) {
                        continuation.resume(-1)
                        return@suspendCancellableCoroutine
                    }
                    continuation.invokeOnCancellation {
                        sensorManager.unregisterListener(listener)
                    }
                }
            } ?: -1
    }
