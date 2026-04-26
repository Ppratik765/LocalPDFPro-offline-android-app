package com.priyanshu.localpdf.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LocalPdfColorScheme = darkColorScheme(
    primary = CatppuccinPrimary,
    background = CatppuccinBackground,
    surface = CatppuccinSurface,
    onPrimary = CatppuccinBackground,
    onBackground = CatppuccinText,
    onSurface = CatppuccinText,
    onSurfaceVariant = CatppuccinSecondaryText
)

@Composable
fun LocalPDFTheme(
    darkTheme: Boolean = true, // Force dark theme as requested
    dynamicColor: Boolean = false, // Force custom colors
    content: @Composable () -> Unit
) {
    val colorScheme = LocalPdfColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}