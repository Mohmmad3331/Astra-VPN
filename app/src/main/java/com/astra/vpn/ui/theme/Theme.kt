package com.astra.vpn.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

// Astra VPN — black & gold palette.
val AstraBlack = Color(0xFF050505)
val AstraCharcoal = Color(0xFF121212)
val AstraGold = Color(0xFFFFB400)
val AstraGoldLight = Color(0xFFFFD66B)
val AstraGoldDeep = Color(0xFFB87900)
val AstraGreen = Color(0xFF3DDC84)
val AstraAmber = Color(0xFFFFC857)
val AstraRed = Color(0xFFFF5C5C)
val AstraTextPrimary = Color(0xFFF6F1E6)
val AstraTextSecondary = Color(0xFFB3A98F)

// Kept for existing references.
val AstraBackground = AstraBlack
val AstraSurface = AstraBlack
val AstraSurfaceElevated = AstraCharcoal

private val AstraColorScheme = darkColorScheme(
    primary = AstraGold,
    secondary = AstraGoldLight,
    background = AstraBlack,
    surface = AstraCharcoal,
    onPrimary = AstraBlack,
    onBackground = AstraTextPrimary,
    onSurface = AstraTextPrimary,
    error = AstraRed
)

/** The UI is Persian, so the whole tree is laid out right-to-left. */
@Composable
fun AstraTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = AstraColorScheme,
            typography = AstraTypography,
            content = content
        )
    }
}
