package com.fitpaisa.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * fitPaisa modern luxury theme (v3).
 * Always dark, always high-contrast, premium midnight obsidian emerald & electric gold.
 */
private val FitPaisaColors = darkColorScheme(
    primary = Gold,
    onPrimary = OnGold,
    primaryContainer = GoldDark,
    onPrimaryContainer = CreamWhite,
    secondary = GoldLight,
    onSecondary = OnGold,
    tertiary = NeonEmerald,
    onTertiary = OnGold,
    background = ForestGreen,
    onBackground = CreamWhite,
    surface = ForestGreenSurface,
    onSurface = CreamWhite,
    surfaceVariant = ForestGreenCard,
    onSurfaceVariant = MutedSage,
    outline = CardBorder,
    outlineVariant = RingTrack,
    error = ErrorRed,
    onError = CreamWhite,
    scrim = NavBarGreen
)

val FitPaisaTypography = Typography()

@Composable
fun FitPaisaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FitPaisaColors,
        typography = FitPaisaTypography,
        content = content
    )
}
