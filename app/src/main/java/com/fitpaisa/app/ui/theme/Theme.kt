package com.fitpaisa.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * fitPaisa theme (v2 redesign). The app is ALWAYS dark: deep forest green
 * background with gold accents, matching the brand logo. There is no light
 * scheme — the dark palette is the product identity.
 */
private val FitPaisaColors = darkColorScheme(
    primary = Gold,
    onPrimary = OnGold,
    primaryContainer = GoldDark,
    onPrimaryContainer = CreamWhite,
    secondary = GoldLight,
    onSecondary = OnGold,
    tertiary = SuccessGreen,
    background = ForestGreen,
    onBackground = CreamWhite,
    surface = ForestGreenSurface,
    onSurface = CreamWhite,
    surfaceVariant = ForestGreenCard,
    onSurfaceVariant = MutedSage,
    outline = Gold,
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
