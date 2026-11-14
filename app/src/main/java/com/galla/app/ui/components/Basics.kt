@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.galla.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.galla.app.core.model.StockLevel
import com.galla.app.hardware.QrCodes
import com.galla.app.ui.icons.Ic
import com.galla.app.ui.theme.GallaTheme
import com.galla.app.ui.theme.GallaType

/*
 * Small building blocks used on every screen. Icons and text are Material's own; the shapes
 * come from Material 3 Expressive's shape library (MaterialShapes).
 */

/** Draws one icon from [Ic]. `fill = true` uses the solid version. */
@Composable
fun GIcon(
    icon: Ic,
    modifier: Modifier = Modifier,
    fill: Boolean = false,
    size: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
    contentDescription: String? = null,
) {
    Icon(
        painter = painterResource(if (fill) icon.fill else icon.outline),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        tint = tint,
    )
}

@Composable
fun Gap(height: Dp) = Spacer(Modifier.height(height))

@Composable
fun HGap(width: Dp) = Spacer(Modifier.width(width))

// ---------------------------------------------------------------------------
// Text pieces
// ---------------------------------------------------------------------------

/** A small capital label above something: "TODAY'S SALES". */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(text.uppercase(), style = GallaType.eyebrow, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = modifier)
}

/** The title of a section of a screen, with an optional link on the right ("See all"). */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, link: String? = null, onLink: (() -> Unit)? = null) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 22.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 10.dp)
                .semantics { heading() },
        )
        if (link != null && onLink != null) TextButton(onClick = onLink) { Text(link) }
    }
}

/** Small grey text at the bottom of a screen. */
@Composable
fun Foot(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 20.dp, vertical = 12.dp),
    )
}

/** A number that rolls to its new value when it changes. */
@Composable
fun RollingText(text: String, style: TextStyle, modifier: Modifier = Modifier, color: Color = LocalContentColor.current) {
    AnimatedContent(
        targetState = text,
        modifier = modifier,
        transitionSpec = {
            (slideInVertically { it / 2 } + fadeIn()) togetherWith (slideOutVertically { -it / 2 } + fadeOut()) using SizeTransform(clip = false)
        },
        label = "rolling text",
    ) { value ->
        Text(value, style = style, color = color, maxLines = 1, softWrap = false)
    }
}

enum class TagKind { NEUTRAL, GOOD, WARN, BAD }

/** A small pill with a word in it: "Low", "Sold out". */
@Composable
fun Tag(text: String, kind: TagKind, modifier: Modifier = Modifier, icon: Ic? = null) {
    val scheme = MaterialTheme.colorScheme
    val (bg, fg) = when (kind) {
        TagKind.NEUTRAL -> scheme.surfaceContainerHighest to scheme.onSurfaceVariant
        TagKind.GOOD -> scheme.primaryContainer to scheme.onPrimaryContainer
        TagKind.WARN -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        TagKind.BAD -> scheme.errorContainer to scheme.onErrorContainer
    }
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icon != null) GIcon(icon, size = 14.dp, tint = fg, fill = true)
        Text(text, style = MaterialTheme.typography.labelSmall, color = fg, maxLines = 1)
    }
}

// ---------------------------------------------------------------------------
// Shapes
// ---------------------------------------------------------------------------

private val avatarShapes = listOf(
    MaterialShapes.Cookie9Sided,
    MaterialShapes.Clover4Leaf,
    MaterialShapes.Sunny,
    MaterialShapes.Pentagon,
    MaterialShapes.Cookie6Sided,
    MaterialShapes.Flower,
    MaterialShapes.Gem,
    MaterialShapes.Puffy,
    MaterialShapes.Slanted,
    MaterialShapes.Arch,
    MaterialShapes.ClamShell,
    MaterialShapes.SoftBurst,
    MaterialShapes.Cookie4Sided,
    MaterialShapes.Bun,
    MaterialShapes.Ghostish,
    MaterialShapes.Pill,
)

/**
 * The picture that stands for an item: its first letter on a Material shape. Items in the
 * same category share a shape and colour, so a category can be spotted in a long list
 * without reading.
 */
@Composable
fun ShapeAvatar(name: String, group: String, modifier: Modifier = Modifier, size: Dp = 48.dp) {
    val scheme = MaterialTheme.colorScheme
    val seed = (group.ifBlank { name }.lowercase().hashCode() and 0x7fffffff)
    val shape = avatarShapes[seed % avatarShapes.size].toShape()
    val (bg, fg) = when ((seed / 7) % 5) {
        0 -> scheme.primaryContainer to scheme.onPrimaryContainer
        1 -> scheme.secondaryContainer to scheme.onSecondaryContainer
        2 -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        3 -> scheme.inverseSurface to scheme.inverseOnSurface
        else -> scheme.primary to scheme.onPrimary
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(bg)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.trim().take(1).uppercase().ifEmpty { "?" },
            style = if (size >= 64.dp) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium,
            color = fg,
        )
    }
}

/** An icon on a Material shape, for empty states and headers. */
@Composable
fun ShapeIcon(
    icon: Ic,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    shape: androidx.graphics.shapes.RoundedPolygon = MaterialShapes.Cookie9Sided,
    background: Color = MaterialTheme.colorScheme.primaryContainer,
    tint: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(shape.toShape())
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        GIcon(icon, size = size * 0.46f, tint = tint, fill = true)
    }
}

/** The Galla mark: a lime coin-like Material shape with a rupee sign drawn in strokes. */
@Composable
fun GallaLogo(size: Dp, modifier: Modifier = Modifier) {
    val fill = GallaTheme.extras.hero
    val stroke = GallaTheme.extras.onHero
    Box(
        modifier
            .size(size)
            .clip(MaterialShapes.Cookie12Sided.toShape())
            .background(fill)
            .clearAndSetSemantics { },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val u = this.size.minDimension / 48f
            val rupee = Path().apply {
                moveTo(16.5f * u, 14.5f * u); lineTo(31.5f * u, 14.5f * u)
                moveTo(16.5f * u, 21f * u); lineTo(31.5f * u, 21f * u)
                moveTo(17f * u, 14.5f * u); lineTo(22.5f * u, 14.5f * u)
                cubicTo(31.2f * u, 14.5f * u, 31.2f * u, 27.5f * u, 22.5f * u, 27.5f * u)
                lineTo(17f * u, 27.5f * u); lineTo(28.5f * u, 37.5f * u)
            }
            drawPath(rupee, stroke, style = Stroke(width = 3.6f * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

// ---------------------------------------------------------------------------
// Stock gauge and QR
// ---------------------------------------------------------------------------

/**
 * How full an item's shelf is, as Material's progress bar. A healthy shelf is a calm
 * straight bar; a low shelf turns amber and WAVY, so it catches the eye.
 */
@Composable
fun StockGauge(stockMilli: Long, minStockMilli: Long, level: StockLevel, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    // The bar is "full" at three times the minimum.
    val full = (minStockMilli * 3).coerceAtLeast(1L)
    val fraction = when {
        stockMilli <= 0L -> 0f
        minStockMilli <= 0L -> 1f
        else -> (stockMilli.toFloat() / full).coerceIn(0.06f, 1f)
    }
    val color = when (level) {
        StockLevel.OK -> scheme.primary
        StockLevel.LOW -> scheme.tertiary
        StockLevel.OUT -> scheme.error
    }
    val wavy = level == StockLevel.LOW
    LinearWavyProgressIndicator(
        progress = { fraction },
        modifier = modifier.clearAndSetSemantics { },
        color = color,
        trackColor = if (level == StockLevel.OUT) scheme.errorContainer else scheme.surfaceContainerHighest,
        amplitude = { if (wavy) 1f else 0f },
    )
}

/** A QR code drawn square by square, black on a white rounded tile so any scanner can read it. */
@Composable
fun QrImage(text: String, modifier: Modifier = Modifier) {
    val grid = remember(text) { runCatching { QrCodes.matrix(text) }.getOrNull() }
    Canvas(
        modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .padding(14.dp)
            .semantics { contentDescription = "QR code" },
    ) {
        if (grid == null) return@Canvas
        val cell = size.minDimension / grid.width
        for (y in 0 until grid.height) {
            for (x in 0 until grid.width) {
                if (grid.get(x, y)) {
                    drawRect(Color.Black, topLeft = Offset(x * cell, y * cell), size = Size(cell + 0.6f, cell + 0.6f))
                }
            }
        }
    }
}
