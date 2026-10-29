package com.alexvicente.moviedb.core.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MovieDbDarkColors = darkColorScheme(
    background = Color(0xFF1A1A2E),
    surface = Color(0xFF1E1E2E),
    primary = Color(0xFFE91E63) // PROVISIONAL: igualar al color "checked" de res/color/bottom_nav_selector.xml
)

@Composable
fun MovieDbTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = MovieDbDarkColors, content = content)
}