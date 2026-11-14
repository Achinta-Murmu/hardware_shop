package com.galla.app.ui.theme

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/** Corner sizes. Galla is round and chunky: nothing has a sharp corner. */
object GallaRadius {
    val xs = 8.dp
    val s = 14.dp
    val m = 22.dp
    val l = 30.dp
    val xl = 38.dp
}

private val GallaShapes = Shapes(
    extraSmall = RoundedCornerShape(GallaRadius.xs),
    small = RoundedCornerShape(GallaRadius.s),
    medium = RoundedCornerShape(GallaRadius.m),
    large = RoundedCornerShape(GallaRadius.l),
    extraLarge = RoundedCornerShape(GallaRadius.xl),
)

val LocalGallaExtras = staticCompositionLocalOf { gallaExtras(dark = false) }
val LocalDarkTheme = staticCompositionLocalOf { false }

object GallaTheme {
    /** The colours Material has no role for: the lime "hero" and the "good news" green. */
    val extras: GallaExtras
        @Composable @ReadOnlyComposable get() = LocalGallaExtras.current

    val dark: Boolean
        @Composable @ReadOnlyComposable get() = LocalDarkTheme.current
}

/** Wallpaper-based colours exist from Android 12 (API 31). */
fun supportsDynamicColor(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * The app's look. `dynamicColor` swaps Galla's green for the colours of the phone's
 * wallpaper (Material You), the way Google's own apps do.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GallaTheme(dark: Boolean, dynamicColor: Boolean, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val useDynamic = dynamicColor && supportsDynamicColor()
    val scheme = remember(dark, useDynamic, context) {
        when {
            useDynamic && dark -> dynamicDarkColorScheme(context)
            useDynamic -> dynamicLightColorScheme(context)
            dark -> DarkScheme
            else -> LightScheme
        }
    }
    val extras = remember(dark, useDynamic, scheme) {
        if (useDynamic) {
            // With phone colours, the hero takes the phone's own accent instead of lime.
            GallaExtras(hero = scheme.primaryContainer, onHero = scheme.onPrimaryContainer, good = scheme.primary)
        } else {
            gallaExtras(dark)
        }
    }
    CompositionLocalProvider(LocalGallaExtras provides extras, LocalDarkTheme provides dark) {
        // Material 3 Expressive: the usual colour roles, type and shapes, plus springy motion.
        MaterialExpressiveTheme(
            colorScheme = scheme,
            motionScheme = MotionScheme.expressive(),
            shapes = GallaShapes,
            typography = GallaTypography,
            content = content,
        )
    }
}
