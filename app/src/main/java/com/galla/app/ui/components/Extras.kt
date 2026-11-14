package com.galla.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.galla.app.ui.icons.Ic
import com.galla.app.ui.theme.GallaType
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Minus, the quantity, plus. Tapping the number itself opens a box to type an exact
 * quantity (useful for 2.5 m of wire).
 */
@Composable
fun QtyStepper(
    text: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onType: () -> Unit,
    modifier: Modifier = Modifier,
    canPlus: Boolean = true,
) {
    val haptics = rememberHaptics()
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        IconBtn(Ic.remove, "One less", { haptics.tick(); onMinus() }, style = IconBtnStyle.CONTAINER, size = 40.dp, iconSize = 20.dp)
        Text(
            text,
            style = GallaType.amount,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier
                .widthIn(min = 44.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .clickable(onClickLabel = "Type the quantity", onClick = onType)
                .padding(horizontal = 6.dp, vertical = 8.dp),
        )
        IconBtn(Ic.add, "One more", { haptics.tick(); onPlus() }, style = IconBtnStyle.FILLED, size = 40.dp, iconSize = 20.dp, enabled = canPlus)
    }
}

/** One bar of a [BarChart]. */
data class Bar(val label: String, val value: Long, val highlight: Boolean = false)

/**
 * A simple bar chart. Each bar grows from the bottom with a spring; the highlighted bar
 * (today) takes the brand colour, the rest stay quiet.
 */
@Composable
fun BarChart(bars: List<Bar>, modifier: Modifier = Modifier, height: Dp = 132.dp, description: String = "Chart") {
    val scheme = MaterialTheme.colorScheme
    val top = max(1L, bars.maxOfOrNull { it.value } ?: 1L)
    val dense = bars.size > 12
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(if (dense) 2.dp else 8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        bars.forEachIndexed { index, bar ->
            val target = if (bar.value <= 0L) 0.02f else (bar.value.toFloat() / top).coerceIn(0.04f, 1f)
            val grown by animateFloatAsState(target, spring(dampingRatio = 0.7f, stiffness = 220f), label = "bar")
            Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(grown)
                            .clip(RoundedCornerShape(if (dense) 4.dp else 10.dp))
                            .background(
                                when {
                                    bar.highlight -> scheme.primary
                                    bar.value <= 0L -> scheme.surfaceContainerHighest
                                    else -> scheme.secondaryContainer
                                },
                            ),
                    )
                }
                // In a month view only every fifth day is labelled, or the labels would collide.
                val showLabel = !dense || index % 5 == 0
                Text(
                    if (showLabel) bar.label else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (bar.highlight) scheme.primary else scheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    // A label can be wider than a thin bar. Let it spill sideways rather than be cut off.
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .wrapContentWidth(unbounded = true),
                )
            }
        }
    }
}

/** The outline of a paper receipt: straight sides and a torn, zigzag bottom edge. */
class ReceiptShape(private val tooth: Dp = 9.dp, private val corner: Dp = 18.dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val t = with(density) { tooth.toPx() }
        val r = with(density) { corner.toPx() }
        val teeth = max(1, (size.width / (2 * t)).roundToInt())
        val step = size.width / teeth
        val path = Path().apply {
            moveTo(0f, r)
            quadraticTo(0f, 0f, r, 0f)
            lineTo(size.width - r, 0f)
            quadraticTo(size.width, 0f, size.width, r)
            lineTo(size.width, size.height - t)
            for (i in teeth downTo 1) {
                lineTo(step * (i - 0.5f), size.height)
                lineTo(step * (i - 1), size.height - t)
            }
            close()
        }
        return Outline.Generic(path)
    }
}

/** A row with a label on the left and an amount on the right, for totals and breakdowns. */
@Composable
fun AmountRow(label: String, amount: String, modifier: Modifier = Modifier, strong: Boolean = false, color: Color = Color.Unspecified) {
    Row(modifier = modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = if (strong) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            color = color,
            modifier = Modifier.weight(1f),
        )
        Text(
            amount,
            style = if (strong) GallaType.figure.copy(fontSize = GallaType.figure.fontSize * 0.8f) else GallaType.amount,
            color = color,
            maxLines = 1,
        )
    }
}
