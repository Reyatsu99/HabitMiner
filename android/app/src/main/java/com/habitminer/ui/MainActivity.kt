package com.habitminer.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "HabitMiner \uD83E\uDDE0\uD83D\uDCCD",
                    style = MaterialTheme.typography.headlineMedium
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text("Tracking Status: INACTIVE", color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Total Points Collected: 0")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Last Export: N/A")
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(onClick = { /* TODO: Trigger LocationTrackingService Start */ }) {
                    Text("Start Tracking")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedButton(onClick = { /* TODO: Trigger ExportWorker */ }) {
                    Text("Export Data")
                }
            }
        }
    }
}
