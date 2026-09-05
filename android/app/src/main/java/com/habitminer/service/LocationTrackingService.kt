package com.habitminer.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.habitminer.data.AppDatabase
import com.habitminer.data.RawGpsEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.IOException
import kotlin.math.log10

class LocationTrackingService : Service(), SensorEventListener {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private var isUserStill = false

    // Context Sensors
    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private var currentLightLux = 0f

    private var mediaRecorder: MediaRecorder? = null
    private var isRecordingAudio = false

    private var isScreenOn = true
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> isScreenOn = true
                Intent.ACTION_SCREEN_OFF -> isScreenOn = false
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "LocationTrackingChannel"
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "ACTION_START_TRACKING"
        const val ACTION_STOP = "ACTION_STOP_TRACKING"
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        // Register Screen receiver
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenReceiver, filter)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if (isUserStill) return
                
                val currentAudioDb = sampleAudioAmplitude()

                for (location in locationResult.locations) {
                    Log.d("HabitMiner", "New Location: ${location.latitude}, ${location.longitude}, Light: $currentLightLux, Audio: $currentAudioDb dB, Screen: $isScreenOn")
                    saveLocationToDb(location, currentLightLux, currentAudioDb, isScreenOn)
                }
            }
        }
        
        setupNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTracking()
            ACTION_STOP -> stopTracking()
        }
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startTracking() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("HabitMiner is active")
            .setContentText("Collecting multi-sensor contextual data...")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
            
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Start listening to Light Sensor
        lightSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        // Start Audio recording session if permitted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startAudioSampler()
        }
        
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
            .setMinUpdateIntervalMillis(1000L)
            .setMinUpdateDistanceMeters(0f)
            .setWaitForAccurateLocation(false)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: Exception) {
            Log.e("HabitMiner", "Error requesting location updates: ${e.message}", e)
        }
    }

    private fun startAudioSampler() {
        try {
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(externalCacheDir?.absolutePath + "/dev_null.3gp") // Discarded
                prepare()
                start()
                isRecordingAudio = true
            }
        } catch (e: IOException) {
            Log.e("HabitMiner", "Failed to start MediaRecorder", e)
        } catch (e: Exception) {
            Log.e("HabitMiner", "SecurityException for MediaRecorder (missing permissions?)", e)
        }
    }

    private fun sampleAudioAmplitude(): Float {
        if (!isRecordingAudio) return 0f
        val maxAmp = mediaRecorder?.maxAmplitude ?: 0
        if (maxAmp == 0) return 0f
        // Convert amplitude to dB (reference maxAmp=1 roughly)
        return (20 * log10(maxAmp.toDouble())).toFloat()
    }

    private fun stopTracking() {
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            sensorManager.unregisterListener(this)
            unregisterReceiver(screenReceiver)
            if (isRecordingAudio) {
                mediaRecorder?.stop()
                mediaRecorder?.release()
                mediaRecorder = null
                isRecordingAudio = false
            }
        } catch (e: Exception) {
            Log.e("HabitMiner", "Error stopping updates: ${e.message}")
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_LIGHT) {
            currentLightLux = event.values[0]
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun setupNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location & Context Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
    
    private fun saveLocationToDb(location: android.location.Location, light: Float, audio: Float, screen: Boolean) {
        serviceScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val entity = RawGpsEntity(
                latitude = location.latitude,
                longitude = location.longitude,
                timestamp = location.time / 1000,
                accuracy = location.accuracy,
                activityState = if (isUserStill) "STILL" else "IN_MOTION",
                audioLevel = audio,
                lightLevel = light,
                isScreenOn = screen
            )
            db.locationDao().insertLocation(entity)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
