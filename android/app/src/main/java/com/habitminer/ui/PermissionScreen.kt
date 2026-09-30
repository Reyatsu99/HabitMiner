@file:Suppress("ktlint:standard:function-naming")

package com.habitminer.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun PermissionScreen(
    hasUsage: Boolean,
    hasRuntime: Boolean,
    hasNotification: Boolean,
    onRequestUsage: () -> Unit,
    onRuntimePermissionsGranted: () -> Unit,
    onRequestNotification: () -> Unit,
) {
    val context = LocalContext.current
    val permissionsToRequest =
        remember {
            mutableListOf<String>().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    add(Manifest.permission.ACTIVITY_RECOGNITION)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }

    val launcher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { result ->
            if (result.values.all { it }) {
                onRuntimePermissionsGranted()
            }
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(80.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Welcome to HabitMiner",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (!hasRuntime && permissionsToRequest.isNotEmpty()) {
            PermissionStep(
                icon = Icons.Default.DirectionsRun,
                title = "Physical Activity & Alerts",
                description =
                    "HabitMiner requires Activity Recognition to monitor step counts and physical motion state, " +
                        "and Notification permissions to alert you of behavioral deviations.",
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { launcher.launch(permissionsToRequest.toTypedArray()) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text("Grant App Permissions", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = {
                    val intent =
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                    context.startActivity(intent)
                },
            ) {
                Text("Open App Settings")
            }
        } else if (!hasUsage) {
            PermissionStep(
                icon = Icons.Default.Smartphone,
                title = "App Usage Access",
                description =
                    "To discover your digital habits, we securely analyze your app usage statistics purely on this device. " +
                        "We do not track keystrokes or screen content.",
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onRequestUsage,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text("Grant Usage Access", style = MaterialTheme.typography.titleMedium)
            }
        } else if (!hasNotification) {
            PermissionStep(
                icon = Icons.Default.NotificationsActive,
                title = "Notification Listener",
                description =
                    "To correlate your phone usage with incoming notifications without reading their content, " +
                        "we need Notification Listener access.",
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onRequestNotification,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text("Enable Notification Listener", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun PermissionStep(
    icon: ImageVector,
    title: String,
    description: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )
    }
}
