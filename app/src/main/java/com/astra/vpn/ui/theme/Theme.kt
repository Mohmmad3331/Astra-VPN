package com.astra.vpn.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Astra VPN premium palette — deep charcoal base, electric indigo → cyan accent.
val AstraBackground = Color(0xFF0B0E14)
val AstraSurface = Color(0xFF12151F)
val AstraSurfaceElevated = Color(0xFF191D2A)
val AstraIndigo = Color(0xFF6C5CE7)
val AstraCyan = Color(0xFF00D9FF)
val AstraGreen = Color(0xFF2EE6A8)   // connected state
val AstraRed = Color(0xFFFF5C72)     // disconnected / alert state
val AstraTextPrimary = Color(0xFFF5F6FA)
val AstraTextSecondary = Color(0xFF8C92A8)
val AstraDivider = Color(0xFF232838)

private val AstraColorScheme = darkColorScheme(
    primary = AstraIndigo,
    secondary = AstraCyan,
    background = AstraBackground,
    surface = AstraSurface,
    surfaceVariant = AstraSurfaceElevated,
    onPrimary = AstraTextPrimary,
    onBackground = AstraTextPrimary,
    onSurface = AstraTextPrimary,
    error = AstraRed
)

@Composable
fun AstraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AstraColorScheme,
        typography = AstraTypography,
        content = content
    )
}
