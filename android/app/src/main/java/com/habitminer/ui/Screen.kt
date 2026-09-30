package com.habitminer.ui

sealed class Screen(val route: String) {
    object Home : Screen("home")

    object History : Screen("history")

    object Insights : Screen("insights")

    object Settings : Screen("settings")

    object Health : Screen("health")
}
