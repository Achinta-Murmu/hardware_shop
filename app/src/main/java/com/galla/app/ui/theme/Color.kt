package com.galla.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/*
 * Galla's own colours: "money green and lime on warm paper".
 *
 *   - Surfaces are a warm cream (light) or a green-tinted near-black (dark), like ledger paper
 *     and a counter at night.
 *   - Green is the brand colour: it stands for money coming in.
 *   - Lime is the signature. The one big number on a screen sits on lime (the "hero").
 *   - Amber means "pay attention" (stock running low). Red means "stop" (sold out, a loss).
 */

val GallaLime = Color(0xFFC8F25C)
val GallaLimeInk = Color(0xFF142100)

val LightScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF1D6B33),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = GallaLime,
    onPrimaryContainer = GallaLimeInk,
    inversePrimary = Color(0xFFA6D46A),
    secondary = Color(0xFF55624C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDDE7CC),
    onSecondaryContainer = Color(0xFF131F0D),
    tertiary = Color(0xFF7A5900),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDF9E),
    onTertiaryContainer = Color(0xFF261A00),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFBF9EE),
    onBackground = Color(0xFF1B1C15),
    surface = Color(0xFFFBF9EE),
    onSurface = Color(0xFF1B1C15),
    surfaceVariant = Color(0xFFE2E4D3),
    onSurfaceVariant = Color(0xFF45483B),
    surfaceTint = Color(0xFF1D6B33),
    inverseSurface = Color(0xFF1B2412),
    inverseOnSurface = Color(0xFFF1F4E4),
    outline = Color(0xFF76796A),
    outlineVariant = Color(0xFFC6C8B7),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFBF9EE),
    surfaceDim = Color(0xFFDBDACD),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F3E7),
    surfaceContainer = Color(0xFFEFEDE1),
    surfaceContainerHigh = Color(0xFFE9E7DB),
    surfaceContainerHighest = Color(0xFFE3E2D5),
)

val DarkScheme: ColorScheme = darkColorScheme(
    primary = GallaLime,
    onPrimary = Color(0xFF1D3200),
    primaryContainer = Color(0xFF34500F),
    onPrimaryContainer = Color(0xFFDCF7A0),
    inversePrimary = Color(0xFF1D6B33),
    secondary = Color(0xFFC1CBB1),
    onSecondary = Color(0xFF2B3421),
    secondaryContainer = Color(0xFF414B36),
    onSecondaryContainer = Color(0xFFDDE7CC),
    tertiary = Color(0xFFF5BF48),
    onTertiary = Color(0xFF402D00),
    tertiaryContainer = Color(0xFF5C4300),
    onTertiaryContainer = Color(0xFFFFDF9E),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF11140D),
    onBackground = Color(0xFFE3E3D6),
    surface = Color(0xFF11140D),
    onSurface = Color(0xFFE3E3D6),
    surfaceVariant = Color(0xFF45483B),
    onSurfaceVariant = Color(0xFFC6C8B7),
    surfaceTint = GallaLime,
    inverseSurface = Color(0xFFE3E3D6),
    inverseOnSurface = Color(0xFF2F3128),
    outline = Color(0xFF909282),
    outlineVariant = Color(0xFF45483B),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF373A30),
    surfaceDim = Color(0xFF11140D),
    surfaceContainerLowest = Color(0xFF0C0F08),
    surfaceContainerLow = Color(0xFF191C14),
    surfaceContainer = Color(0xFF1D2018),
    surfaceContainerHigh = Color(0xFF282B22),
    surfaceContainerHighest = Color(0xFF32362C),
)

/** The few colours Material has no role for. */
data class GallaExtras(
    /** Background of the one big number on a screen. */
    val hero: Color,
    /** Text and icons on [hero]. */
    val onHero: Color,
    /** "This is good news": profit, healthy stock. */
    val good: Color,
)

fun gallaExtras(dark: Boolean): GallaExtras = GallaExtras(
    hero = GallaLime,
    onHero = GallaLimeInk,
    good = if (dark) Color(0xFFA6D46A) else Color(0xFF1D6B33),
)
