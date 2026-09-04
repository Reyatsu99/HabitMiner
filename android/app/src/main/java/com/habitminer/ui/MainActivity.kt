package com.habitminer.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.habitminer.data.AppDatabase
import com.habitminer.data.RawGpsEntity
import com.habitminer.service.LocationTrackingService
import com.habitminer.worker.ExportWorker
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HabitMinerApp()
        }
    }
}

@Composable
fun HabitMinerApp() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isTracking by remember { mutableStateOf(false) }
    
    // Live DB count from Room
    val db = remember { AppDatabase.getDatabase(context) }
    val pointCount by db.locationDao().getLocationCountFlow().collectAsState(initial = 0)

    // Location permissions launcher (Requests Fine + Coarse)
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        if (hasLocationPermission(context)) {
            checkGpsAndStart(context) {
                isTracking = true
            }
        } else {
            Toast.makeText(context, "Location permission is required to track GPS points", Toast.LENGTH_LONG).show()
        }
    }

    // Notification permission launcher (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Notification permission granted or denied - proceed with location tracking regardless
        if (hasLocationPermission(context)) {
            checkGpsAndStart(context) {
                isTracking = true
            }
        }
    }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "HabitMiner 🧠📍",
                    style = MaterialTheme.typography.headlineMedium
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = if (isTracking) "Status: ACTIVE 🟢" else "Status: INACTIVE 🔴",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isTracking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Total Points Collected: $pointCount",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (isTracking) {
                            toggleTrackingService(context, false)
                            isTracking = false
                        } else {
                            if (!hasLocationPermission(context)) {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission(context)) {
                                // Request notification permission for foreground service, but don't block tracking
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                checkGpsAndStart(context) {
                                    isTracking = true
                                }
                            }
                        }
                    }
                ) {
                    Text(if (isTracking) "Stop Tracking" else "Start Tracking")
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val replayManager = com.habitminer.data.TrajectoryReplayManager(context)
                        replayManager.injectSyntheticDemoData(50)
                        Toast.makeText(context, "Injected 50 demo GPS points into database!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Load Demo Trajectory (+50 Points)")
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val exportRequest = OneTimeWorkRequestBuilder<ExportWorker>().build()
                        WorkManager.getInstance(context).enqueue(exportRequest)
                        Toast.makeText(context, "Data export enqueued via WorkManager", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Export Data (JSON)")
                }
            }
        }
    }
}

private fun hasLocationPermission(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    return fine || coarse
}

private fun hasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

private fun checkGpsAndStart(context: Context, onStarted: () -> Unit) {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                       locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

    if (!isGpsEnabled) {
        Toast.makeText(context, "Please turn ON Location / GPS in your phone settings!", Toast.LENGTH_LONG).show()
    } else {
        toggleTrackingService(context, true)
        Toast.makeText(context, "Location tracking started!", Toast.LENGTH_SHORT).show()
        onStarted()
    }
}

private fun toggleTrackingService(context: Context, start: Boolean) {
    val intent = Intent(context, LocationTrackingService::class.java).apply {
        action = if (start) LocationTrackingService.ACTION_START else LocationTrackingService.ACTION_STOP
    }
    if (start) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    } else {
        context.stopService(intent)
    }
}
