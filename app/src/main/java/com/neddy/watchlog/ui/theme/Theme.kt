package com.neddy.watchlog.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val WatchlogDarkColorScheme = darkColorScheme(
    primary = WatchPrimary,
    onPrimary = WatchOnPrimary,
    primaryContainer = WatchPrimaryContainer,
    onPrimaryContainer = WatchOnPrimaryContainer,
    secondary = WatchSecondary,
    onSecondary = WatchOnSecondary,
    secondaryContainer = WatchSecondaryContainer,
    onSecondaryContainer = WatchOnSecondaryContainer,
    tertiary = WatchTertiary,
    onTertiary = WatchOnTertiary,
    tertiaryContainer = WatchTertiaryContainer,
    onTertiaryContainer = WatchOnTertiaryContainer,
    error = WatchError,
    onError = WatchOnError,
    errorContainer = WatchErrorContainer,
    onErrorContainer = WatchOnErrorContainer,
    background = WatchBackground,
    onBackground = WatchOnBackground,
    surface = WatchSurface,
    onSurface = WatchOnSurface,
    surfaceVariant = WatchSurfaceVariant,
    onSurfaceVariant = WatchOnSurfaceVariant,
    outline = WatchOutline,
    outlineVariant = WatchOutlineVariant,
    inverseSurface = WatchInverseSurface,
    inverseOnSurface = WatchInverseOnSurface,
    inversePrimary = WatchInversePrimary,
    scrim = WatchScrim,
    surfaceTint = WatchSurfaceTint
)

@Composable
fun WatchlogTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current

    // Use Material You (dynamic colors) on Android 12+ (API 31+), fall back to custom scheme
    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicDarkColorScheme(context)
    } else {
        WatchlogDarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = WatchlogTypography,
        content = content
    )
}
