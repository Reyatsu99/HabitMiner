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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route

                Scaffold(
                    bottomBar = {
                        if (state.hasUsagePermission) {
                            NavigationBar(
                                containerColor = Color(0xFF1E293B),
                            ) {
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Home, contentDescription = "Today") },
                                    label = { Text("Today") },
                                    selected = currentRoute == Screen.Home.route,
                                    onClick = {
                                        navController.navigate(Screen.Home.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
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
                                    selected = currentRoute == Screen.Habits.route,
                                    onClick = {
                                        navController.navigate(Screen.Habits.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
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
                                    selected = currentRoute == Screen.Insights.route,
                                    onClick = {
                                        navController.navigate(Screen.Insights.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
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
                        }
                    },
                    containerColor = Color(0xFF0F172A),
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier.padding(innerPadding),
                        color = Color(0xFF0F172A),
                    ) {
                        if (!state.hasUsagePermission || !state.hasRuntimePermissions) {
                            PermissionScreen(
                                hasUsage = state.hasUsagePermission,
                                onRequestUsage = {
                                    startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                },
                            )
                        } else {
                            NavHost(navController = navController, startDestination = Screen.Home.route) {
                                composable(Screen.Home.route) { HomeScreen(state, viewModel) }
                                composable(Screen.Habits.route) { HabitsScreen(state) }
                                composable(Screen.Insights.route) { InsightsScreen(state) }
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
