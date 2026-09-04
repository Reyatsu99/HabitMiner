package com.habitminer.ui

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.habitminer.engine.HabitUiState
import org.json.JSONArray
import org.json.JSONObject

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MapScreen(state: HabitUiState) {
    var webView by remember { mutableStateOf<WebView?>(null) }

    // Build JSON payload whenever state changes
    val mapJson = remember(state.allPoints, state.stayPoints, state.poiSummaries) {
        val gpsArr = JSONArray()
        // Send at most 500 points to keep WebView fast
        val step = maxOf(1, state.allPoints.size / 500)
        state.allPoints.filterIndexed { i, _ -> i % step == 0 }.forEach { p ->
            gpsArr.put(JSONObject().apply {
                put("lat", p.latitude)
                put("lon", p.longitude)
            })
        }

        val stayArr = JSONArray()
        state.stayPoints.forEach { sp ->
            val summary = state.poiSummaries.find { it.clusterId == sp.clusterId }
            stayArr.put(JSONObject().apply {
                put("lat", sp.lat)
                put("lon", sp.lon)
                put("cluster", sp.clusterId)
                put("label", summary?.label ?: "Place ${sp.clusterId}")
                put("visits", summary?.visitCount ?: 1)
            })
        }

        JSONObject().apply {
            put("gps", gpsArr)
            put("stay", stayArr)
        }.toString()
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = WebViewClient()
                    loadUrl("file:///android_asset/map.html")
                    webView = this
                }
            },
            update = { wv ->
                // Push data into JS after page loads
                val escaped = mapJson.replace("\\", "\\\\").replace("'", "\\'")
                wv.evaluateJavascript("loadPoints('$escaped')", null)
            },
            modifier = Modifier.fillMaxSize()
        )

        if (state.allPoints.isEmpty()) {
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(
                    Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🗺️", style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "No GPS data yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Start tracking or load demo data\nto see your trajectory here",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}
