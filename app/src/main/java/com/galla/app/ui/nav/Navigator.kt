package com.galla.app.ui.nav

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.galla.app.AppContainer
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Moves between screens. It owns the back stack (a plain list of [Route] keys) and
 * remembers which tab is lit in the navigation bar.
 *
 * Tapping a tab starts that tab fresh; `go` adds a screen on top; `back` removes the top one.
 */
class Navigator {
    val backStack = mutableStateListOf<Route>(Today)

    var tab: Route by mutableStateOf(Today)
        private set

    /** How the last move should animate: "push", "pop" or "tab". */
    var lastMove: String by mutableStateOf("tab")
        private set

    /** True when one of the four main tabs is showing (so the navigation bar should be visible). */
    val atTab: Boolean get() = backStack.size == 1

    fun switchTab(target: Route) {
        lastMove = "tab"
        tab = target
        backStack.clear()
        backStack.add(target)
    }

    fun go(route: Route) {
        if (backStack.lastOrNull() == route) return
        lastMove = "push"
        backStack.add(route)
    }

    /** Replaces the screen on top (the scanner hands over to the item it found). */
    fun replaceTop(route: Route) {
        lastMove = "push"
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
        backStack.add(route)
    }

    /** Opens `routes` on top of a tab's first screen, e.g. Items > one item. */
    fun openIn(target: Route, vararg routes: Route) {
        lastMove = "push"
        tab = target
        backStack.clear()
        backStack.add(target)
        backStack.addAll(routes)
    }

    /** Returns false when there is nothing left to go back to (the system then closes the app). */
    fun back(): Boolean {
        lastMove = "pop"
        return when {
            backStack.size > 1 -> {
                backStack.removeAt(backStack.lastIndex)
                true
            }
            tab != Today -> {
                switchTab(Today)
                lastMove = "pop"
                true
            }
            else -> false
        }
    }
}

/**
 * Short messages at the bottom of the screen ("Sale saved", "Only 3 left").
 * They are Material Snackbars; this class just makes showing one a single call.
 */
class Messenger(val host: SnackbarHostState, private val launch: (suspend () -> Unit) -> Job) {
    private var current: Job? = null

    fun show(text: String) {
        // A new message replaces the one on screen instead of queueing behind it.
        current?.cancel()
        current = launch { host.showSnackbar(text) }
    }
}

/** Keeps the [Navigator] and the message host alive when the screen rotates or the theme changes. */
class NavViewModel : ViewModel() {
    val navigator = Navigator()
    val messenger = Messenger(SnackbarHostState()) { block -> viewModelScope.launch { block() } }
}

val LocalNav = staticCompositionLocalOf<Navigator> { error("No Navigator provided") }
val LocalContainer = staticCompositionLocalOf<AppContainer> { error("No AppContainer provided") }
val LocalMessenger = staticCompositionLocalOf<Messenger> { error("No Messenger provided") }
