@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.galla.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.galla.app.ui.icons.Ic
import com.galla.app.ui.theme.GallaTheme
import androidx.compose.material3.FilterChip as M3FilterChip

/*
 * Every control in this file IS a Google Material 3 component (the same ones Google's own
 * apps are built from). The functions here only choose which Material component to use and
 * fill in Galla's icon and text, so screens can say `Btn("Go", ...)` instead of repeating the
 * same eight lines. Nothing is drawn by hand.
 */

// ---------------------------------------------------------------------------
// Icon buttons: Material IconButton / FilledIconButton / FilledTonalIconButton
// ---------------------------------------------------------------------------

enum class IconBtnStyle { PLAIN, TONAL, FILLED, SURFACE, CONTAINER }

@Composable
fun IconBtn(
    icon: Ic,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: IconBtnStyle = IconBtnStyle.PLAIN,
    fill: Boolean = false,
    /** Shows a small Material badge on the icon (unread items). */
    dot: Boolean = false,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp,
    tint: Color? = null,
    enabled: Boolean = true,
    /** For on/off buttons (a star, a bell): the current state, so the tap gives the matching haptic. */
    toggled: Boolean? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = rememberHaptics()
    val sized = modifier.size(size)
    // Material 3 Expressive shapes: round at rest, squarer while pressed.
    val shapes = IconButtonDefaults.shapes()
    val tap = {
        if (toggled != null) haptics.toggle(!toggled)
        onClick()
    }
    val content: @Composable () -> Unit = {
        if (dot) {
            BadgedBox(badge = { Badge() }) {
                GIcon(icon, fill = fill, size = iconSize, tint = tint ?: LocalContentColor.current, contentDescription = contentDescription)
            }
        } else {
            GIcon(icon, fill = fill, size = iconSize, tint = tint ?: LocalContentColor.current, contentDescription = contentDescription)
        }
    }
    when (style) {
        IconBtnStyle.PLAIN -> IconButton(tap, shapes, sized, enabled, content = content)
        IconBtnStyle.TONAL -> FilledTonalIconButton(tap, shapes, sized, enabled, content = content)
        IconBtnStyle.FILLED -> FilledIconButton(tap, shapes, sized, enabled, content = content)
        IconBtnStyle.SURFACE -> FilledTonalIconButton(
            tap, shapes, sized, enabled,
            colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = scheme.surface, contentColor = scheme.onSurface),
            content = content,
        )
        IconBtnStyle.CONTAINER -> FilledTonalIconButton(
            tap, shapes, sized, enabled,
            colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = scheme.surfaceContainerHighest, contentColor = scheme.onSurface),
            content = content,
        )
    }
}

// ---------------------------------------------------------------------------
// Buttons: Material Button / FilledTonalButton / OutlinedButton / TextButton
// ---------------------------------------------------------------------------

/**
 * FILLED is the main action. TONAL and OUTLINE are quieter. WARN is amber ("needs attention"),
 * DANGER is red, and HERO is the dark button that sits on a lime hero card.
 */
enum class BtnStyle { FILLED, TONAL, OUTLINE, TEXT, WARN, DANGER, HERO }

/** Button heights: Small 40dp, Medium 56dp, Large 64dp. */
enum class BtnSize { SM, MD, LG }

@Composable
fun Btn(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Ic? = null,
    trailing: Ic? = null,
    style: BtnStyle = BtnStyle.FILLED,
    size: BtnSize = BtnSize.MD,
    enabled: Boolean = true,
    fillIcon: Boolean = false,
    contentDescription: String? = null,
) {
    val height = when (size) {
        BtnSize.SM -> ButtonDefaults.MinHeight
        BtnSize.MD -> ButtonDefaults.MediumContainerHeight
        // Material's own "large" is 96dp, too tall for a phone counter. 64dp is the tallest we use.
        BtnSize.LG -> 64.dp
    }
    // Material's own shapes (they squash slightly while pressed), padding, icon size and text style for this height.
    val shapes = ButtonDefaults.shapesFor(height)
    val padding = ButtonDefaults.contentPaddingFor(height, hasStartIcon = icon != null, hasEndIcon = trailing != null)
    val iconSize = ButtonDefaults.iconSizeFor(height)
    val spacing = ButtonDefaults.iconSpacingFor(height)
    val textStyle = ButtonDefaults.textStyleFor(height)
    val sized = modifier
        .heightIn(min = height)
        .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier)

    val haptics = rememberHaptics()
    val tap = {
        haptics.click()
        onClick()
    }
    val content: @Composable RowScope.() -> Unit = {
        if (icon != null) {
            GIcon(icon, fill = fillIcon, size = iconSize)
            Spacer(Modifier.width(spacing))
        }
        Text(
            text,
            style = textStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = if (trailing != null) Modifier.weight(1f, fill = false) else Modifier,
        )
        if (trailing != null) {
            Spacer(Modifier.width(spacing))
            GIcon(trailing, size = iconSize)
        }
    }
    when (style) {
        BtnStyle.FILLED -> Button(tap, shapes, sized, enabled, contentPadding = padding, content = content)
        BtnStyle.TONAL -> FilledTonalButton(tap, shapes, sized, enabled, contentPadding = padding, content = content)
        BtnStyle.OUTLINE -> OutlinedButton(tap, shapes, sized, enabled, contentPadding = padding, content = content)
        BtnStyle.TEXT -> TextButton(tap, shapes, sized, enabled, contentPadding = padding, content = content)
        BtnStyle.WARN -> FilledTonalButton(
            tap, shapes, sized, enabled,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            ),
            contentPadding = padding,
            content = content,
        )
        BtnStyle.DANGER -> FilledTonalButton(
            tap, shapes, sized, enabled,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
            contentPadding = padding,
            content = content,
        )
        BtnStyle.HERO -> Button(
            tap, shapes, sized, enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = GallaTheme.extras.onHero,
                contentColor = GallaTheme.extras.hero,
            ),
            contentPadding = padding,
            content = content,
        )
    }
}

// ---------------------------------------------------------------------------
// Chips: Material FilterChip
// ---------------------------------------------------------------------------

@Composable
fun Chip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    icon: Ic? = null,
    trailing: Ic? = null,
    fillIcon: Boolean = false,
    iconTint: Color? = null,
) {
    // Typed out so Kotlin knows these optional slots are composable.
    val leading: (@Composable () -> Unit)? = if (icon != null) {
        { GIcon(icon, fill = fillIcon, size = FilterChipDefaults.IconSize, tint = iconTint ?: LocalContentColor.current) }
    } else {
        null
    }
    val trailingIcon: (@Composable () -> Unit)? = if (trailing != null) {
        { GIcon(trailing, size = FilterChipDefaults.IconSize) }
    } else {
        null
    }
    val haptics = rememberHaptics()
    M3FilterChip(
        selected = selected,
        onClick = {
            haptics.tick()
            onClick()
        },
        label = { Text(text, maxLines = 1) },
        // Material 3 Expressive chip shapes (they change slightly while pressed).
        shapes = FilterChipDefaults.shapes(),
        modifier = modifier,
        leadingIcon = leading,
        trailingIcon = trailingIcon,
    )
}

/** A chip that shows a tick while it is selected. */
@Composable
fun FilterChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, icon: Ic? = null) {
    Chip(text, onClick, modifier, selected = selected, icon = if (selected) Ic.check else icon)
}

/** A row of chips that scrolls sideways. */
@Composable
fun ChipRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

// ---------------------------------------------------------------------------
// Connected button group (Material 3 Expressive): ToggleButtons that share corners,
// with the shapes Material defines for the first, middle and last button.
// ---------------------------------------------------------------------------

data class GroupOption(val label: String, val icon: Ic? = null, val sub: String? = null)

@Composable
fun ButtonGroup(
    options: List<GroupOption>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    /** true = buttons share the width equally; false = natural width, scrolls sideways. */
    fill: Boolean = true,
) {
    val row = if (fill) modifier.fillMaxWidth() else modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
    val haptics = rememberHaptics()
    Row(
        modifier = row,
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        options.forEachIndexed { index, option ->
            val icon: (@Composable () -> Unit)? = option.icon?.let { ic -> { GIcon(ic, size = ButtonDefaults.IconSize) } }
            ToggleButton(
                checked = index == selectedIndex,
                onCheckedChange = {
                    if (index != selectedIndex) haptics.tick()
                    onSelect(index)
                },
                modifier = Modifier
                    .then(if (fill) Modifier.weight(1f) else Modifier)
                    .semantics { role = Role.RadioButton },
                icon = icon,
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
            ) {
                Text(option.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Switch: Material Switch
// ---------------------------------------------------------------------------

@Composable
fun GSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    /** Locked switches are always on and cannot be changed. */
    locked: Boolean = false,
) {
    val haptics = rememberHaptics()
    Switch(
        checked = checked,
        onCheckedChange = { on ->
            haptics.toggle(on)
            onCheckedChange(on)
        },
        modifier = modifier.semantics {
            contentDescription = label
            stateDescription = if (checked) "On" else "Off"
        },
        enabled = !locked,
    )
}
