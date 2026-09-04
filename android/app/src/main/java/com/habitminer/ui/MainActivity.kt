package com.habitminer.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import com.habitminer.service.LocationTrackingService
import com.habitminer.worker.ExportWorker

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
    var isTracking by remember { mutableStateOf(false) }
    
    // Live DB count from Room
    val db = remember { AppDatabase.getDatabase(context) }
    val pointCount by db.locationDao().getLocationCountFlow().collectAsState(initial = 0)

    // Permission launcher
    val permissionsToRequest = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            toggleTrackingService(context, true)
            isTracking = true
        } else {
            Toast.makeText(context, "Location permissions required for tracking", Toast.LENGTH_SHORT).show()
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
                            val hasPermissions = permissionsToRequest.all {
                                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                            }
                            if (hasPermissions) {
                                toggleTrackingService(context, true)
                                isTracking = true
                            } else {
                                permissionLauncher.launch(permissionsToRequest)
                            }
                        }
                    }
                ) {
                    Text(if (isTracking) "Stop Tracking" else "Start Tracking")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
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

