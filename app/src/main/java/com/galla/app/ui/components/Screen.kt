package com.galla.app.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * How far the screen has scrolled. [AppBar] reads this to stay pinned at the top while the
 * rest of the page scrolls underneath it.
 */
val LocalScreenScroll = compositionLocalOf<ScrollState?> { null }

/** The frame every scrolling screen sits in: full size, below the status bar, scrolls vertically. */
@Composable
fun ScreenColumn(
    modifier: Modifier = Modifier,
    scroll: ScrollState = rememberScrollState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    CompositionLocalProvider(LocalScreenScroll provides scroll) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .statusBarsPadding()
                .verticalScroll(scroll)
                .padding(bottom = 16.dp),
            content = content,
        )
    }
}

/**
 * Turns a Flow into the StateFlow a screen observes. It keeps collecting for 5 seconds after
 * the screen stops looking (so a rotation does not restart the work), then stops.
 */
fun <T> Flow<T>.asState(viewModel: ViewModel, initial: T): StateFlow<T> =
    stateIn(viewModel.viewModelScope, SharingStarted.WhileSubscribed(5_000L), initial)
