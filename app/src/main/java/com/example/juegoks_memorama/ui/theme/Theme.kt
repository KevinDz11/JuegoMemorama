package com.example.juegoks_memorama.ui.theme

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
// Necesitarás importar tu modelo de AppThemeOption, que crearemos en el siguiente paso
import com.example.juegoks_memorama.model.AppThemeOption

// Definición de Esquemas de Color IPN
private val IPNLightColorScheme = lightColorScheme(
    primary = IPN_Light_Primary,
    onPrimary = IPN_Light_OnPrimary,
    primaryContainer = IPN_Light_PrimaryContainer,
    secondary = IPN_Light_Secondary,
    background = IPN_Light_Background,
    surface = IPN_Light_Surface,
)

private val IPNDarkColorScheme = darkColorScheme(
    primary = IPN_Dark_Primary,
    onPrimary = IPN_Dark_OnPrimary,
    primaryContainer = IPN_Dark_PrimaryContainer,
    secondary = IPN_Dark_Secondary,
    background = IPN_Dark_Background,
    surface = IPN_Dark_Surface,
)

// Definición de Esquemas de Color ESCOM
private val ESCOMLightColorScheme = lightColorScheme(
    primary = ESCOM_Light_Primary,
    onPrimary = ESCOM_Light_OnPrimary,
    primaryContainer = ESCOM_Light_PrimaryContainer,
    secondary = ESCOM_Light_Secondary,
    background = ESCOM_Light_Background,
    surface = ESCOM_Light_Surface,
)

private val ESCOMDarkColorScheme = darkColorScheme(
    primary = ESCOM_Dark_Primary,
    onPrimary = ESCOM_Dark_OnPrimary,
    primaryContainer = ESCOM_Dark_PrimaryContainer,
    secondary = ESCOM_Dark_Secondary,
    background = ESCOM_Dark_Background,
    surface = ESCOM_Dark_Surface,
)

@Composable
fun MemoryGameTheme(
    // Parámetro nuevo para seleccionar el tema (por defecto IPN)
    themeOption: AppThemeOption = AppThemeOption.IPN,
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Lo desactivamos por defecto para que se vean tus temas
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // Lógica de selección de tema
        themeOption == AppThemeOption.IPN -> {
            if (darkTheme) IPNDarkColorScheme else IPNLightColorScheme
        }
        themeOption == AppThemeOption.ESCOM -> {
            if (darkTheme) ESCOMDarkColorScheme else ESCOMLightColorScheme
        }
        // Valor por defecto en caso de que algo falle
        else -> if (darkTheme) IPNDarkColorScheme else IPNLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        // typography = Typography, // Puedes definir tipografía si quieres luego
        content = content
    )
}