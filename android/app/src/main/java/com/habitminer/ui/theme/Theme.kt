@file:Suppress("ktlint:standard:function-naming")

package com.habitminer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
    darkColorScheme(
        primary = Color(0xFF38BDF8),
        secondary = Color(0xFF4ADE80),
        background = Color(0xFF0F172A),
        surface = Color(0xFF1E293B),
        onPrimary = Color.White,
        onSecondary = Color.Black,
        onBackground = Color.White,
        onSurface = Color.White,
        error = Color(0xFFEF4444),
        errorContainer = Color(0x33EF4444),
        onErrorContainer = Color(0xFFFCA5A5),
    )

@Composable
fun HabitMinerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content,
    )
}
