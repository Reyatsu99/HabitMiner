package com.habitminer.ui

sealed class Screen(val route: String) {
    object Home : Screen("home")

    object Habits : Screen("habits")

    object Insights : Screen("insights")

    object Settings : Screen("settings")
}
