// SPDX-License-Identifier: GPL-3.0-or-later
package io.nsflip.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1F8A4C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB7F4CB),
    onPrimaryContainer = Color(0xFF002110),
    secondary = Color(0xFF4D6357),
    tertiary = Color(0xFF8A5D1F),
    surface = Color(0xFFF9FAF7),
    background = Color(0xFFF9FAF7),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF34C77B),
    onPrimary = Color(0xFF00391B),
    primaryContainer = Color(0xFF0F5130),
    onPrimaryContainer = Color(0xFFB7F4CB),
    secondary = Color(0xFFB6CCBC),
    tertiary = Color(0xFFF4B75C),
    surface = Color(0xFF101418),
    background = Color(0xFF0C1013),
)

@Composable
fun NSFlipTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
