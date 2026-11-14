@file:OptIn(ExperimentalTextApi::class)

package com.galla.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.galla.app.R

/*
 * One font does everything: Bricolage Grotesque, a "variable" font. A single file holds every
 * weight and width, and each line below asks that file for one setting.
 *
 *   - `Bricolage` is the reading version: normal width, tuned for small text.
 *   - `BricolageDisplay` is the loud version: narrower and tuned for very large sizes, so a
 *     big rupee amount fits on one line and still looks heavy.
 */

private fun bricolage(weight: Int, opticalSize: Float, width: Float): Font = Font(
    resId = R.font.bricolage_grotesque,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight),
        FontVariation.width(width),
        FontVariation.Setting("opsz", opticalSize),
    ),
)

val Bricolage = FontFamily(
    bricolage(400, 14f, 100f),
    bricolage(500, 14f, 100f),
    bricolage(600, 14f, 100f),
    bricolage(700, 14f, 100f),
    bricolage(800, 14f, 100f),
)

val BricolageDisplay = FontFamily(
    bricolage(600, 96f, 84f),
    bricolage(700, 96f, 84f),
    bricolage(800, 96f, 84f),
)

private val CenteredLines = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

/**
 * Builds a text style from plain numbers: `ts(18f, 700)` reads as "size 18, weight 700".
 *
 * @param lh line height as a multiple of the size (1.4 = 140%)
 * @param ls letter spacing in em (negative pulls letters together)
 * @param tabular true = every digit has the same width, so columns of money line up
 * @param display true = use the narrow, loud version of the font
 */
fun ts(
    size: Float,
    weight: Int = 400,
    lh: Float = 1.35f,
    ls: Float = 0f,
    tabular: Boolean = false,
    display: Boolean = false,
): TextStyle = TextStyle(
    fontFamily = if (display) BricolageDisplay else Bricolage,
    fontSize = size.sp,
    fontWeight = FontWeight(weight),
    lineHeight = (size * lh).sp,
    letterSpacing = ls.em,
    lineHeightStyle = CenteredLines,
    fontFeatureSettings = if (tabular) "tnum" else null,
)

/** Galla's own named styles, for things Material has no slot for. */
object GallaType {
    /** The one huge number on a screen. */
    val hero = ts(68f, 800, lh = 1.0f, ls = -0.02f, tabular = true, display = true)
    /** A big number on a card. */
    val figure = ts(34f, 800, lh = 1.05f, ls = -0.01f, tabular = true, display = true)
    /** A number inside a list row. */
    val amount = ts(17f, 700, lh = 1.2f, tabular = true)
    /** Small capital label above something. */
    val eyebrow = ts(12f, 700, lh = 1.3f, ls = 0.09f)
}

/** Material's own slots, filled with Bricolage so stock components match too. */
val GallaTypography = Typography(
    displayLarge = ts(56f, 800, lh = 1.02f, ls = -0.02f, display = true),
    displayMedium = ts(46f, 800, lh = 1.04f, ls = -0.02f, display = true),
    displaySmall = ts(38f, 800, lh = 1.06f, ls = -0.01f, display = true),
    headlineLarge = ts(32f, 800, lh = 1.1f, ls = -0.01f, display = true),
    headlineMedium = ts(28f, 800, lh = 1.12f, display = true),
    headlineSmall = ts(24f, 700, lh = 1.15f, display = true),
    titleLarge = ts(21f, 700, lh = 1.2f),
    titleMedium = ts(17f, 700, lh = 1.25f),
    titleSmall = ts(15f, 700, lh = 1.3f),
    bodyLarge = ts(16.5f, 400, lh = 1.45f),
    bodyMedium = ts(14.5f, 400, lh = 1.4f),
    bodySmall = ts(13f, 400, lh = 1.35f),
    labelLarge = ts(15f, 700, lh = 1.25f),
    labelMedium = ts(13f, 600, lh = 1.25f),
    labelSmall = ts(11.5f, 600, lh = 1.25f, ls = 0.02f),
)
