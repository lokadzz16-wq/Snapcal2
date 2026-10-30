package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SnapDarkColorScheme = darkColorScheme(
    primary = SnapPrimary,
    onPrimary = SnapPrimaryForeground,
    primaryContainer = SnapPrimarySoft,
    onPrimaryContainer = SnapForeground,
    secondary = SnapSecondary,
    onSecondary = SnapSecondaryForeground,
    secondaryContainer = SnapMuted,
    onSecondaryContainer = SnapForeground,
    background = SnapBackground,
    onBackground = SnapForeground,
    surface = SnapCard,
    onSurface = SnapForeground,
    surfaceVariant = SnapMuted,
    onSurfaceVariant = SnapMutedForeground,
    outline = SnapCardBorder,
    outlineVariant = SnapInput,
    error = SnapDestructive,
    onError = SnapDestructiveForeground
)

@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SnapDarkColorScheme,
        typography = SnapCalTypography,
        content = content
    )
}
