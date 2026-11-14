package com.galla.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.galla.app.AppContainer
import com.galla.app.core.model.Settings
import com.galla.app.core.model.ThemeChoice
import com.galla.app.ui.items.ItemDetailScreen
import com.galla.app.ui.items.ItemEditScreen
import com.galla.app.ui.items.ItemsScreen
import com.galla.app.ui.money.ExpenseAddScreen
import com.galla.app.ui.money.MoneyScreen
import com.galla.app.ui.nav.ExpenseAdd
import com.galla.app.ui.nav.GallaNavBar
import com.galla.app.ui.nav.ItemDetail
import com.galla.app.ui.nav.ItemEdit
import com.galla.app.ui.nav.Items
import com.galla.app.ui.nav.LocalContainer
import com.galla.app.ui.nav.LocalMessenger
import com.galla.app.ui.nav.LocalNav
import com.galla.app.ui.nav.Messenger
import com.galla.app.ui.nav.MoneyTab
import com.galla.app.ui.nav.NavViewModel
import com.galla.app.ui.nav.Navigator
import com.galla.app.ui.nav.Receipt
import com.galla.app.ui.nav.Restock
import com.galla.app.ui.nav.Route
import com.galla.app.ui.nav.Scan
import com.galla.app.ui.nav.Sell
import com.galla.app.ui.nav.SettingsScreenKey
import com.galla.app.ui.nav.StockCount
import com.galla.app.ui.nav.StockIn
import com.galla.app.ui.nav.Today
import com.galla.app.ui.onboarding.OnboardingScreen
import com.galla.app.ui.receipt.ReceiptScreen
import com.galla.app.ui.scan.ScanScreen
import com.galla.app.ui.sell.SellScreen
import com.galla.app.ui.settings.SettingsScreen
import com.galla.app.ui.stock.RestockScreen
import com.galla.app.ui.stock.StockCountScreen
import com.galla.app.ui.stock.StockInScreen
import com.galla.app.ui.theme.GallaTheme
import com.galla.app.ui.today.TodayScreen

/**
 * The top of the UI tree. It turns Settings into a theme, shows the welcome screen on first
 * launch, and otherwise shows the current screen with the navigation bar under it.
 *
 * @param onThemeResolved tells the Activity whether the app is dark, so the phone's status
 *   and navigation bar icons get the right colour.
 */
@Composable
fun GallaRoot(container: AppContainer, onThemeResolved: (dark: Boolean) -> Unit) {
    val settingsState by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
    // Until settings are read from disk the splash screen stays up; draw nothing.
    val settings: Settings = settingsState ?: return

    val systemDark = isSystemInDarkTheme()
    val dark = when (settings.theme) {
        ThemeChoice.DARK -> true
        ThemeChoice.LIGHT -> false
        ThemeChoice.SYSTEM -> systemDark
    }
    DisposableEffect(dark) {
        onThemeResolved(dark)
        onDispose { }
    }

    val navViewModel: NavViewModel = viewModel()
    val nav = navViewModel.navigator
    val messenger = navViewModel.messenger

    GallaTheme(dark = dark, dynamicColor = settings.dynamicColor) {
        CompositionLocalProvider(
            LocalContainer provides container,
            LocalNav provides nav,
            LocalMessenger provides messenger,
        ) {
            // A Material Surface paints the background and tells everything inside which
            // colour text and icons should be (light in the dark theme, dark in the light one).
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                if (!settings.onboarded) {
                    OnboardingScreen()
                } else {
                    MainScreens(container, nav, messenger)
                }
            }
        }
    }
}

@Composable
private fun MainScreens(container: AppContainer, nav: Navigator, messenger: Messenger) {
    val cart by container.cart.lines.collectAsStateWithLifecycle()
    val density = LocalDensity.current
    val keyboardOpen = WindowInsets.ime.getBottom(density) > 0
    // The navigation bar shows on the four main tabs only, and hides while typing.
    val showBar = nav.atTab && !keyboardOpen

    // On a tab other than Today, Back returns to Today instead of closing the app.
    BackHandler(enabled = nav.atTab && nav.tab != Today) { nav.back() }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            NavDisplay(
                backStack = nav.backStack,
                onBack = { nav.back() },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                transitionSpec = { forward(nav.lastMove) },
                popTransitionSpec = { backward() },
                predictivePopTransitionSpec = { backward() },
                entryProvider = entryProvider<Route> {
                    entry<Today> { TodayScreen() }
                    entry<Sell> { SellScreen() }
                    entry<Items> { ItemsScreen() }
                    entry<MoneyTab> { MoneyScreen() }
                    entry<ItemDetail> { key -> ItemDetailScreen(key) }
                    entry<ItemEdit> { key -> ItemEditScreen(key) }
                    entry<StockIn> { key -> StockInScreen(key) }
                    entry<StockCount> { key -> StockCountScreen(key) }
                    entry<Restock> { RestockScreen() }
                    entry<Scan> { key -> ScanScreen(key) }
                    entry<Receipt> { key -> ReceiptScreen(key) }
                    entry<ExpenseAdd> { ExpenseAddScreen() }
                    entry<SettingsScreenKey> { SettingsScreen() }
                },
            )
            if (showBar) {
                GallaNavBar(selected = nav.tab, cartLines = cart.size, onSelect = { nav.switchTab(it) })
            } else {
                // No bar: leave room for the keyboard or the phone's own gesture bar instead.
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.safeDrawing))
            }
        }
        SnackbarHost(
            hostState = messenger.host,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                .padding(bottom = if (showBar) 76.dp else 12.dp),
        )
    }
}

/** Going deeper slides in from the right; switching tabs fades and grows slightly. */
private fun forward(move: String): ContentTransform = when (move) {
    "tab" -> (fadeIn(tween(220)) + scaleIn(tween(260), initialScale = 0.97f)) togetherWith fadeOut(tween(120))
    else -> (fadeIn(tween(220)) + slideInHorizontally(tween(300)) { it / 9 }) togetherWith fadeOut(tween(120))
}

private fun backward(): ContentTransform =
    (fadeIn(tween(220)) + slideInHorizontally(tween(300)) { -it / 9 }) togetherWith fadeOut(tween(120))
