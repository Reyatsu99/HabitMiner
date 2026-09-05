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
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.habitminer.data.TrajectoryReplayManager
import com.habitminer.data.GeoLifeDataLoader
import com.habitminer.engine.HabitUiState
import com.habitminer.engine.HabitViewModel
import com.habitminer.service.LocationTrackingService
import com.habitminer.worker.ExportWorker

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { HabitMinerApp() }
    }
}

sealed class Screen(val label: String) {
    object Home     : Screen("Home")
    object Map      : Screen("Map")
    object Insights : Screen("Insights")
}

@Composable
fun HabitMinerApp(vm: HabitViewModel = viewModel()) {
    val state by vm.uiState.collectAsState()
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    val context = LocalContext.current

    val locationPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms.values.any { it }) {
            startTracking(context); vm.setTracking(true)
        } else {
            Toast.makeText(context, "Location permission required", Toast.LENGTH_SHORT).show()
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color(0xFF0F172A),
            surface    = Color(0xFF1E293B),
            primary    = Color(0xFF38BDF8),
            secondary  = Color(0xFF4ADE80)
        )
    ) {
        Scaffold(
            containerColor = Color(0xFF0F172A),
            bottomBar = {
                NavigationBar(containerColor = Color(0xFF1E293B), tonalElevation = 0.dp) {
                    listOf(
                        Triple(Screen.Home,     Icons.Default.Home,      "Home"),
                        Triple(Screen.Map,      Icons.Default.Map,       "Map"),
                        Triple(Screen.Insights, Icons.Default.Analytics, "Insights")
                    ).forEach { (screen, icon, label) ->
                        NavigationBarItem(
                            selected = currentScreen == screen,
                            onClick  = { currentScreen = screen },
                            icon     = { Icon(icon, contentDescription = label) },
                            label    = { Text(label) },
                            colors   = NavigationBarItemDefaults.colors(
                                selectedIconColor   = Color(0xFF38BDF8),
                                selectedTextColor   = Color(0xFF38BDF8),
                                unselectedIconColor = Color(0xFF64748B),
                                unselectedTextColor = Color(0xFF64748B),
                                indicatorColor      = Color(0xFF0F172A)
                            )
                        )
                    }
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding)) {
                when (currentScreen) {
                    Screen.Home     -> HomeScreen(
                        state = state,
                        onStartStop = {
                            if (state.isTracking) {
                                stopTracking(context); vm.setTracking(false)
                            } else {
                                val hasLoc = hasLocationPerm(context)
                                if (hasLoc) { startTracking(context); vm.setTracking(true) }
                                else locationPermLauncher.launch(
                                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION)
                                )
                            }
                        },
                        onLoadGeoLife = {
                            GeoLifeDataLoader(context).loadIntoDatabase { count ->
                                Toast.makeText(
                                    context,
                                    if (count > 0) "Loaded $count real GeoLife GPS points!"
                                    else "demo_trajectory.json not found in assets. Run export script first.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        onLoadDemo = {
                            TrajectoryReplayManager(context).injectSyntheticDemoData(50)
                            Toast.makeText(context, "Loaded 50 demo GPS points!", Toast.LENGTH_SHORT).show()
                        },
                        onExport = {
                            WorkManager.getInstance(context)
                                .enqueue(OneTimeWorkRequestBuilder<ExportWorker>().build())
                            Toast.makeText(context, "Export started in background", Toast.LENGTH_SHORT).show()
                        }
                    )
                    Screen.Map      -> MapScreen(state = state)
                    Screen.Insights -> InsightsScreen(state = state)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Home Screen
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun HomeScreen(
    state: HabitUiState,
    onStartStop:   () -> Unit,
    onLoadGeoLife: () -> Unit,
    onLoadDemo:    () -> Unit,
    onExport:      () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 20.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("HabitMiner", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 26.sp)
                    Text("Routine Intelligence Engine", color = Color(0xFF64748B), fontSize = 13.sp)
                }
                TrackingPulse(isTracking = state.isTracking)
            }
        }

        // Status card
        item { StatusCard(state = state, onStartStop = onStartStop) }

        // Prediction card
        item { PredictionCard(state = state) }

        // Stats row
        item { StatsRow(state = state) }

        // Actions
        item {
            Button(
                onClick = onLoadGeoLife,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("🗂️  Load Real GeoLife Dataset", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onLoadDemo,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4ADE80)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4ADE80))
                ) { Text("Demo Data", fontSize = 13.sp) }

                OutlinedButton(
                    onClick = onExport,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                ) { Text("Export JSON", fontSize = 13.sp) }
            }
        }
    }
}

@Composable
fun TrackingPulse(isTracking: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = "pulseScale"
    )
    Box(
        Modifier
            .size(16.dp)
            .scale(if (isTracking) scale else 1f)
            .clip(CircleShape)
            .background(if (isTracking) Color(0xFF4ADE80) else Color(0xFF475569))
    )
}

@Composable
fun StatusCard(state: HabitUiState, onStartStop: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (state.isTracking) "Tracking ACTIVE" else "Tracking INACTIVE",
                        color = if (state.isTracking) Color(0xFF4ADE80) else Color(0xFF94A3B8),
                        fontWeight = FontWeight.SemiBold, fontSize = 15.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${state.pointCount} GPS points collected",
                        color = Color(0xFF64748B), fontSize = 12.sp
                    )
                }
                Button(
                    onClick = onStartStop,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.isTracking) Color(0xFF7F1D1D) else Color(0xFF0369A1)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (state.isTracking) "Stop" else "Start", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun PredictionCard(state: HabitUiState) {
    val pred = state.prediction
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (pred != null) Color(0xFF0C2340) else Color(0xFF1E293B)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                "🔮  Next Predicted Destination",
                color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))

            if (pred != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(pred.label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("Based on ${pred.timeSlot} patterns", color = Color(0xFF64748B), fontSize = 12.sp)
                    }
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF38BDF8).copy(alpha = 0.15f))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            "${pred.confidence}%",
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Bold, fontSize = 18.sp
                        )
                    }
                }
            } else {
                Text(
                    if (state.pointCount == 0)
                        "Start tracking or load demo data to see predictions"
                    else
                        "Analysing movement patterns…\nNeed more data to make predictions",
                    color = Color(0xFF64748B), fontSize = 13.sp, lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun StatsRow(state: HabitUiState) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(
            Modifier.weight(1f),
            emoji = "📍", label = "Places Found",
            value = "${state.poiSummaries.size}"
        )
        StatCard(
            Modifier.weight(1f),
            emoji = "🧭", label = "Stay Points",
            value = "${state.stayPoints.size}"
        )
        StatCard(
            Modifier.weight(1f),
            emoji = "🎯", label = "Predictability",
            value = "${state.predictabilityScore}%"
        )
    }
}

@Composable
fun StatCard(modifier: Modifier, emoji: String, label: String, value: String) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 22.sp)
            Spacer(Modifier.height(6.dp))
            Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(label, color = Color(0xFF64748B), fontSize = 10.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────
private fun hasLocationPerm(ctx: Context) =
    ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
    ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

private fun startTracking(ctx: Context) {
    val intent = Intent(ctx, LocationTrackingService::class.java).apply { action = LocationTrackingService.ACTION_START }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(intent) else ctx.startService(intent)
}

private fun stopTracking(ctx: Context) {
    ctx.stopService(Intent(ctx, LocationTrackingService::class.java))
}
