package com.cangzr.neocard.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val PrimaryLight = Color(0xFF000000) // Saf siyah
val PrimaryDark = Color(0xFFFFFFFF) // Saf beyaz

val SecondaryLight = Color(0xFF1A1A1A) // Zengin siyah
val SecondaryDark = Color(0xFFF0F0F0) // İnce beyaz

val TertiaryLight = Color(0xFF2C2C2C) // Antrasit siyah
val TertiaryDark = Color(0xFFE0E0E0) // İnci beyazı

val BackgroundLight = Color(0xFFFCFCFC) // Krem beyaz
val BackgroundDark = Color(0xFF0A0A0A) // Gece siyahı
val SurfaceLight = Color(0xFFFFFFFF) // Saf beyaz
val SurfaceDark = Color(0xFF141414) // Kadife siyah

val OnPrimaryLight = Color(0xFFFFFFFF) // Saf beyaz
val OnPrimaryDark = Color(0xFF000000) // Saf siyah
val OnSecondaryLight = Color(0xFFF5F5F5) // Parlak beyaz
val OnSecondaryDark = Color(0xFF0A0A0A) // Derin siyah
val OnBackgroundLight = Color(0xFF0A0A0A) // Derin siyah
val OnBackgroundDark = Color(0xFFF8F8F8) // İpek beyazı
val OnSurfaceLight = Color(0xFF0A0A0A) // Derin siyah
val OnSurfaceDark = Color(0xFFF8F8F8) // İpek beyazı

val ShadowLight = Color(0x29000000) // Yumuşak siyah gölge (%16)
val ShadowDark = Color(0x29FFFFFF) // Yumuşak beyaz gölge (%16)
val HighlightLight = Color(0x40333333) // Zarif siyah vurgu (%25)
val HighlightDark = Color(0x40DDDDDD) // Zarif beyaz vurgu (%25)

val ElevatedSurfaceLight = Color(0xFFF8F8F8) // Hafif yükseltilmiş yüzey
val ElevatedSurfaceDark = Color(0xFF1C1C1C) // Hafif yükseltilmiş yüzey
val SubtleAccentLight = Color(0xFF3A3A3A) // Hafif vurgu
val SubtleAccentDark = Color(0xFFD0D0D0) // Hafif vurgu

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    secondary = SecondaryDark,
    tertiary = TertiaryDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    onPrimary = OnPrimaryDark,
    onSecondary = OnSecondaryDark,
    onBackground = OnBackgroundDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = ElevatedSurfaceDark,
    onSurfaceVariant = SubtleAccentDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    secondary = SecondaryLight,
    tertiary = TertiaryLight,
    background = BackgroundLight,
    surface = SurfaceLight,
    onPrimary = OnPrimaryLight,
    onSecondary = OnSecondaryLight,
    onBackground = OnBackgroundLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = ElevatedSurfaceLight,
    onSurfaceVariant = SubtleAccentLight
)

@Composable
fun NeoCardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Özel renk şemasını kullanmak için false yapıldı
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
