package com.galla.app.ui.nav

import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.galla.app.ui.components.GIcon
import com.galla.app.ui.components.rememberHaptics
import com.galla.app.ui.icons.Ic

private data class NavItem(val route: Route, val label: String, val icon: Ic)

private val items = listOf(
    NavItem(Today, "Today", Ic.storefront),
    NavItem(Sell, "Sell", Ic.point_of_sale),
    NavItem(Items, "Items", Ic.inventory_2),
    NavItem(MoneyTab, "Money", Ic.account_balance_wallet),
)

/**
 * The bottom navigation bar: Material 3 Expressive's ShortNavigationBar, the shorter bar
 * Google's current apps use. The number of lines on the open bill shows as a Material
 * Badge on "Sell". Changing tab gives a light tick.
 */
@Composable
fun GallaNavBar(selected: Route, cartLines: Int, onSelect: (Route) -> Unit, modifier: Modifier = Modifier) {
    val haptics = rememberHaptics()
    ShortNavigationBar(modifier = modifier) {
        items.forEach { item ->
            val isSelected = item.route == selected
            ShortNavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (!isSelected) haptics.tick()
                    onSelect(item.route)
                },
                icon = {
                    if (item.route == Sell && cartLines > 0) {
                        BadgedBox(badge = { Badge { Text(if (cartLines > 9) "9+" else cartLines.toString()) } }) {
                            GIcon(item.icon, fill = isSelected)
                        }
                    } else {
                        GIcon(item.icon, fill = isSelected)
                    }
                },
                label = { Text(item.label, maxLines = 1) },
            )
        }
    }
}
