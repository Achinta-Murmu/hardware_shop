@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.galla.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.galla.app.ui.icons.Ic

/*
 * Top bars and the search box, built from Google's Material 3 components:
 *   AppBar      -> TopAppBar (with Material 3 Expressive's subtitle slot)
 *   TabHeader   -> a large heading for the four main tabs
 *   SearchField -> TextField, pill shaped
 */

/** Bars sit inside a screen that is already below the status bar, so they add no inset of their own. */
private val NoInsets = WindowInsets(0, 0, 0, 0)

/** The bar of every inner screen: back arrow, title, optional second line, actions. */
@Composable
fun AppBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "",
    subtitle: String = "",
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = {
            if (title.isNotEmpty()) {
                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() })
            }
        },
        subtitle = {
            if (subtitle.isNotEmpty()) Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        modifier = modifier,
        navigationIcon = { IconBtn(Ic.arrow_back, "Back", onBack) },
        actions = actions,
        windowInsets = NoInsets,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent),
    )
}

/** The big heading of a main tab: a small line above, the title, and actions on the right. */
@Composable
fun TabHeader(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 14.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            if (eyebrow != null) Eyebrow(eyebrow)
            Text(
                title,
                style = MaterialTheme.typography.displaySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
        }
        actions()
    }
}

/** The search box: a pill-shaped Material TextField with a search icon and a clear button. */
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val focus = LocalFocusManager.current
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        placeholder = { Text(placeholder, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = { GIcon(Ic.search) },
        trailingIcon = {
            if (value.isNotEmpty()) IconBtn(Ic.close, "Clear search", { onValueChange("") })
        },
        singleLine = true,
        shape = CircleShape,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = scheme.surfaceContainerHigh,
            unfocusedContainerColor = scheme.surfaceContainerHigh,
            // A pill has no underline.
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
    )
}
