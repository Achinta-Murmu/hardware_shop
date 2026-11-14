@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.galla.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.TextStyle
import androidx.graphics.shapes.RoundedPolygon
import com.galla.app.ui.theme.GallaRadius
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.galla.app.ui.icons.Ic
import com.galla.app.ui.theme.GallaTheme
import com.galla.app.ui.theme.GallaType

/*
 * Lists and cards, built from Google's Material 3 components:
 *   grouped rows -> SegmentedListItem (the Material 3 Expressive segmented list)
 *   cards        -> Card
 * These functions only pick the component and pass Galla's content into its slots.
 */

// ---------------------------------------------------------------------------
// Grouped list: Material's segmented list items
// ---------------------------------------------------------------------------

/** The corner shape Material gives row number `index` in a segmented group of `count` rows. */
@Composable
fun groupShape(index: Int, count: Int): Shape = ListItemDefaults.segmentedShapes(index, count).shape

/** A stack of rows that belong together. Put [GroupItem]s inside. */
@Composable
fun GGroup(modifier: Modifier = Modifier, horizontal: Dp = 16.dp, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontal),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        content = content,
    )
}

/** One row of a group: a Material SegmentedListItem holding whatever `content` draws. */
@Composable
fun GroupItem(
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    background: Color = Color.Unspecified,
    semanticLabel: String? = null,
    content: @Composable () -> Unit,
) {
    val shapes = ListItemDefaults.segmentedShapes(index, count)
    val container = if (background == Color.Unspecified) MaterialTheme.colorScheme.surfaceContainer else background
    val colors = ListItemDefaults.segmentedColors(containerColor = container)
    val placed = modifier
        .fillMaxWidth()
        .then(if (semanticLabel != null) Modifier.semantics(mergeDescendants = true) { contentDescription = semanticLabel } else Modifier)
    // The row's own content brings its padding, so the list item adds none.
    val noPadding = PaddingValues(0.dp)
    if (onClick != null) {
        SegmentedListItem(onClick = onClick, shapes = shapes, modifier = placed, colors = colors, contentPadding = noPadding, content = content)
    } else {
        SegmentedListItem(shapes = shapes, modifier = placed, colors = colors, contentPadding = noPadding, content = content)
    }
}

/**
 * The usual inside of a row: something on the left, a title with a line under it,
 * and something on the right.
 */
@Composable
fun RowContent(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    below: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            below?.invoke()
        }
        trailing?.invoke()
    }
}

@Composable
fun Chevron() = GIcon(Ic.chevron_right, size = 22.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant)

// ---------------------------------------------------------------------------
// Cards
// ---------------------------------------------------------------------------

enum class CardKind { PLAIN, HERO, INK, WARN, ERROR, SECONDARY }

@Composable
fun cardColors(kind: CardKind): Pair<Color, Color> {
    val s = MaterialTheme.colorScheme
    return when (kind) {
        CardKind.PLAIN -> s.surfaceContainer to s.onSurface
        // The lime card that holds the one big number on a screen.
        CardKind.HERO -> GallaTheme.extras.hero to GallaTheme.extras.onHero
        // A dark slab in the light theme (a light slab in the dark theme): maximum contrast.
        CardKind.INK -> s.inverseSurface to s.inverseOnSurface
        CardKind.WARN -> s.tertiaryContainer to s.onTertiaryContainer
        CardKind.ERROR -> s.errorContainer to s.onErrorContainer
        CardKind.SECONDARY -> s.secondaryContainer to s.onSecondaryContainer
    }
}

@Composable
fun GCard(
    modifier: Modifier = Modifier,
    kind: CardKind = CardKind.PLAIN,
    onClick: (() -> Unit)? = null,
    padding: Dp = 18.dp,
    shape: Shape = MaterialTheme.shapes.medium,
    content: @Composable ColumnScope.() -> Unit,
) {
    val (bg, fg) = cardColors(kind)
    val colors = CardDefaults.cardColors(containerColor = bg, contentColor = fg)
    val body: @Composable ColumnScope.() -> Unit = { Column(Modifier.padding(padding), content = content) }
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, shape = shape, colors = colors, content = body)
    } else {
        Card(modifier = modifier, shape = shape, colors = colors, content = body)
    }
}

/** A small card with one number and what it means. */
@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    kind: CardKind = CardKind.PLAIN,
    icon: Ic? = null,
    shape: Shape = MaterialTheme.shapes.medium,
    valueStyle: TextStyle = MaterialTheme.typography.headlineSmall,
    onClick: (() -> Unit)? = null,
) {
    GCard(modifier = modifier, kind = kind, onClick = onClick, padding = 16.dp, shape = shape) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (icon != null) GIcon(icon, size = 16.dp, fill = true)
            Text(label.uppercase(), style = GallaType.eyebrow, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Gap(8.dp)
            RollingText(value, style = valueStyle.copy(fontFeatureSettings = "tnum"))
    }
}

/**
 * The lime card that holds the one big number on a screen. A large Material shape turns
 * slowly in its corner, which is what makes the card feel alive rather than flat.
 */
@Composable
fun HeroCard(
    modifier: Modifier = Modifier,
    kind: CardKind = CardKind.HERO,
    decoration: RoundedPolygon = MaterialShapes.Sunny,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val (bg, fg) = cardColors(kind)
    val shape = RoundedCornerShape(GallaRadius.xl)
    val colors = CardDefaults.cardColors(containerColor = bg, contentColor = fg)
    val turn by rememberInfiniteTransition(label = "hero").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 48_000, easing = LinearEasing)),
        label = "hero turn",
    )
    val body: @Composable ColumnScope.() -> Unit = {
        Box {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 52.dp, y = (-52).dp)
                    .size(190.dp)
                    .rotate(turn)
                    .clip(decoration.toShape())
                    .background(fg.copy(alpha = 0.08f)),
            )
            Column(Modifier.padding(22.dp), content = content)
        }
    }
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, shape = shape, colors = colors, content = body)
    } else {
        Card(modifier = modifier, shape = shape, colors = colors, content = body)
    }
}

/** A coloured card with an icon, a bold line and a second line. Usually tappable. */
@Composable
fun Banner(
    title: String,
    subtitle: String,
    icon: Ic,
    modifier: Modifier = Modifier,
    kind: CardKind = CardKind.WARN,
    onClick: (() -> Unit)? = null,
) {
    GCard(modifier = modifier, kind = kind, onClick = onClick, padding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GIcon(icon, fill = true)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            }
            if (onClick != null) GIcon(Ic.chevron_right)
        }
    }
}

// ---------------------------------------------------------------------------
// States
// ---------------------------------------------------------------------------

/** Shown where a list would be when there is nothing in it yet. */
@Composable
fun EmptyState(
    title: String,
    text: String,
    icon: Ic,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ShapeIcon(icon, size = 96.dp)
        Gap(6.dp)
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (action != null) {
            Gap(8.dp)
            action()
        }
    }
}
