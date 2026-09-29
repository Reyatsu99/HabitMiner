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

data class AccelerometerStats(
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
            val motionStats = if (collectSensors) collectMotionState() else null

            return ContextSnapshotEntity(
                timestamp = timestamp,
                accelMean = motionStats?.mean ?: -1f,
                accelVariance = motionStats?.variance ?: -1f,
                accelStd = motionStats?.std ?: -1f,
                accelMin = motionStats?.min ?: -1f,
                accelMax = motionStats?.max ?: -1f,
                accelEnergy = motionStats?.energy ?: -1f,
                lightLux = lightLux,
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

        private suspend fun collectMotionState(): AccelerometerStats? =
            withTimeoutOrNull(2000L) {
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
                                            val std = sqrt(variance)
                                            val min = samples.minOrNull() ?: 0f
                                            val max = samples.maxOrNull() ?: 0f
                                            val energy = samples.map { it * it }.average().toFloat()

                                            val stats =
                                                AccelerometerStats(
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
                    val registered = sensorManager.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_GAME, handler)
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
