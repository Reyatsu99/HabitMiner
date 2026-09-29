@file:Suppress("ktlint:standard:function-naming")

package com.habitminer.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.habitminer.engine.HabitViewModel
import com.habitminer.ui.theme.HabitMinerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: HabitViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HabitMinerTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                var selectedTab by remember { mutableIntStateOf(0) }

                Scaffold(
                    bottomBar = {
                        NavigationBar(
                            containerColor = Color(0xFF1E293B),
                        ) {
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Home, contentDescription = "Today") },
                                label = { Text("Today") },
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                colors =
                                    NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFF38BDF8),
                                        unselectedIconColor = Color.LightGray,
                                        selectedTextColor = Color(0xFF38BDF8),
                                        unselectedTextColor = Color.LightGray,
                                        indicatorColor = Color(0xFF0F172A),
                                    ),
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Psychology, contentDescription = "Habits") },
                                label = { Text("Habits") },
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                colors =
                                    NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFF38BDF8),
                                        unselectedIconColor = Color.LightGray,
                                        selectedTextColor = Color(0xFF38BDF8),
                                        unselectedTextColor = Color.LightGray,
                                        indicatorColor = Color(0xFF0F172A),
                                    ),
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Analytics, contentDescription = "Insights") },
                                label = { Text("Insights") },
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                colors =
                                    NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFF38BDF8),
                                        unselectedIconColor = Color.LightGray,
                                        selectedTextColor = Color(0xFF38BDF8),
                                        unselectedTextColor = Color.LightGray,
                                        indicatorColor = Color(0xFF0F172A),
                                    ),
                            )
                        }
                    },
                    containerColor = Color(0xFF0F172A),
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier.padding(innerPadding),
                        color = Color(0xFF0F172A),
                    ) {
                        if (!state.hasUsagePermission) {
                            PermissionScreen(
                                hasUsage = state.hasUsagePermission,
                                onRequestUsage = {
                                    startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                },
                            )
                        } else {
                            when (selectedTab) {
                                0 -> HomeScreen(state, viewModel)
                                1 -> HabitsScreen(state)
                                2 -> InsightsScreen(state)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkPermissions()
    }
}
