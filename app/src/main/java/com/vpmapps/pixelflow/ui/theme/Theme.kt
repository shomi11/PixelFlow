package com.vpmapps.pixelflow.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PixelBluePrimary,
    onPrimary = Color.White,
    primaryContainer = PixelBlueOnContainer,
    onPrimaryContainer = PixelBlueContainer,
    secondary = PixelAmber,
    onSecondary = Color.White,
    tertiary = PixelGreen,
    background = DarkSurface,
    onBackground = Color(0xFFE3E3E8),
    surface = DarkSurfaceVariant,
    onSurface = Color(0xFFE3E3E8),
    surfaceVariant = Color(0xFF28282F),
    onSurfaceVariant = Color(0xFFC7C7D1)
)

private val LightColorScheme = lightColorScheme(
    primary = PixelBluePrimary,
    onPrimary = Color.White,
    primaryContainer = PixelBlueContainer,
    onPrimaryContainer = PixelBlueOnContainer,
    secondary = PixelAmber,
    onSecondary = Color.White,
    tertiary = PixelGreen,
    background = Color(0xFFF8F9FD),
    onBackground = Color(0xFF1B1B1F),
    surface = Color.White,
    onSurface = Color(0xFF1B1B1F),
    surfaceVariant = Color(0xFFEFF2F9),
    onSurfaceVariant = Color(0xFF45464F)
)

/**
 * Status colors Material 3 doesn't define. Use these instead of hard-coded greens and oranges so
 * success and warning states stay readable in both light and dark mode.
 */
@Immutable
data class ExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color
)

private val LightExtendedColors = ExtendedColors(
    success = SuccessLight,
    onSuccess = OnSuccessLight,
    successContainer = SuccessContainerLight,
    onSuccessContainer = OnSuccessContainerLight,
    warning = WarningLight,
    onWarning = OnWarningLight,
    warningContainer = WarningContainerLight,
    onWarningContainer = OnWarningContainerLight
)

private val DarkExtendedColors = ExtendedColors(
    success = SuccessDark,
    onSuccess = OnSuccessDark,
    successContainer = SuccessContainerDark,
    onSuccessContainer = OnSuccessContainerDark,
    warning = WarningDark,
    onWarning = OnWarningDark,
    warningContainer = WarningContainerDark,
    onWarningContainer = OnWarningContainerDark
)

private val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }

/** Success / warning colors for the current light or dark theme. */
val MaterialTheme.extendedColors: ExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalExtendedColors.current

/**
 * Expressive shape scale. Screens should use these (or component defaults) instead of their own
 * RoundedCornerShape values.
 */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/**
 * Material type scale with bolder headlines and titles, so screens don't need to override
 * FontWeight. The *Emphasized styles provided by MaterialExpressiveTheme are used for hero text.
 */
private val BaseTypography = Typography()
val AppTypography = BaseTypography.copy(
    headlineLarge = BaseTypography.headlineLarge.copy(fontWeight = FontWeight.Bold),
    headlineMedium = BaseTypography.headlineMedium.copy(fontWeight = FontWeight.Bold),
    headlineSmall = BaseTypography.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = BaseTypography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = BaseTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = BaseTypography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalExtendedColors provides if (darkTheme) DarkExtendedColors else LightExtendedColors) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = MotionScheme.expressive(),
            shapes = AppShapes,
            typography = AppTypography,
            content = content
        )
    }
}
