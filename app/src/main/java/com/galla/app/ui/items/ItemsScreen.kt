package com.galla.app.ui.items

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.galla.app.AppContainer
import com.galla.app.core.model.StockLevel
import com.galla.app.core.util.Money
import com.galla.app.core.util.Qty
import com.galla.app.data.local.db.ItemEntity
import com.galla.app.ui.components.Btn
import com.galla.app.ui.components.ChipRow
import com.galla.app.ui.components.EmptyState
import com.galla.app.ui.components.FilterChip
import com.galla.app.ui.components.GIcon
import com.galla.app.ui.components.Gap
import com.galla.app.ui.components.GroupItem
import com.galla.app.ui.components.IconBtn
import com.galla.app.ui.components.SearchField
import com.galla.app.ui.components.ShapeAvatar
import com.galla.app.ui.components.StockGauge
import com.galla.app.ui.components.TabHeader
import com.galla.app.ui.components.asState
import com.galla.app.ui.components.rememberHaptics
import com.galla.app.ui.icons.Ic
import com.galla.app.ui.nav.ItemDetail
import com.galla.app.ui.nav.ItemEdit
import com.galla.app.ui.nav.LocalContainer
import com.galla.app.ui.nav.LocalNav
import com.galla.app.ui.nav.Scan
import com.galla.app.ui.sell.matches
import com.galla.app.ui.theme.GallaType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine

enum class ItemFilter { ALL, LOW, OUT }

/** Everything the Items tab shows. */
data class ItemsUi(
    val loaded: Boolean = false,
    val all: List<ItemEntity> = emptyList(),
    val query: String = "",
    val filter: ItemFilter = ItemFilter.ALL,
) {
    val lowCount: Int get() = all.count { it.level == StockLevel.LOW }
    val outCount: Int get() = all.count { it.level == StockLevel.OUT }

    /** The items after the search box and the filter chip. */
    val visible: List<ItemEntity>
        get() = all.filter { item ->
            when (filter) {
                ItemFilter.ALL -> true
                ItemFilter.LOW -> item.level == StockLevel.LOW
                ItemFilter.OUT -> item.level == StockLevel.OUT
            } && item.matches(query)
        }
}

class ItemsViewModel(app: AppContainer) : ViewModel() {
    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(ItemFilter.ALL)

    val state: StateFlow<ItemsUi> =
        combine(app.shop.items, query, filter) { items, query, filter -> ItemsUi(true, items, query, filter) }
            .asState(this, ItemsUi())

    fun setQuery(text: String) {
        query.value = text
    }

    fun setFilter(choice: ItemFilter) {
        filter.value = choice
    }
}

/**
 * The Items tab: everything the shop sells. Each row shows the shelf level as a bar
 * (it turns amber and wavy when low), so a glance down the list shows what needs buying.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ItemsScreen() {
    val app = LocalContainer.current
    val nav = LocalNav.current
    val haptics = rememberHaptics()
    val vm: ItemsViewModel = viewModel { ItemsViewModel(app) }
    val ui by vm.state.collectAsStateWithLifecycle()
    val scheme = MaterialTheme.colorScheme
    val visible = ui.visible
    val listState = rememberLazyListState()
    // A new filter or search is a new list: start it from the top, not where the old one was scrolled to.
    LaunchedEffect(ui.filter, ui.query) { listState.scrollToItem(0) }

    Box(
        Modifier
            .fillMaxSize()
            .background(scheme.surface)
            .statusBarsPadding(),
    ) {
        Column(Modifier.fillMaxSize()) {
            TabHeader(
                title = "Items",
                eyebrow = if (ui.all.size == 1) "1 item in the shop" else "${ui.all.size} items in the shop",
                actions = { IconBtn(Ic.qr_code_scanner, "Find an item by its sticker", { nav.go(Scan("find")) }) },
            )
            SearchField(ui.query, vm::setQuery, "Search name, category or code", Modifier.padding(horizontal = 16.dp))
            Gap(6.dp)
            ChipRow {
                FilterChip("All ${ui.all.size}", ui.filter == ItemFilter.ALL, { vm.setFilter(ItemFilter.ALL) })
                FilterChip("Low ${ui.lowCount}", ui.filter == ItemFilter.LOW, { vm.setFilter(ItemFilter.LOW) })
                FilterChip("Sold out ${ui.outCount}", ui.filter == ItemFilter.OUT, { vm.setFilter(ItemFilter.OUT) })
            }
            Gap(6.dp)

            when {
                ui.loaded && ui.all.isEmpty() -> EmptyState(
                    title = "Your shelves are empty",
                    text = "Add the things your shop sells. Each one gets its own QR sticker.",
                    icon = Ic.inventory_2,
                    action = { Btn("Add your first item", { nav.go(ItemEdit()) }, icon = Ic.add) },
                )
                visible.isEmpty() && ui.loaded -> EmptyState(
                    title = "Nothing here",
                    text = if (ui.query.isNotBlank()) "No item matches \"${ui.query.trim()}\"." else "No items in this group right now.",
                    icon = Ic.search_off,
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                ) {
                    itemsIndexed(visible, key = { _, item -> item.id }) { index, item ->
                        GroupItem(index, visible.size, onClick = { nav.go(ItemDetail(item.id)) }) {
                            ItemRow(item)
                        }
                    }
                }
            }
        }

        if (ui.all.isNotEmpty()) {
            ExtendedFloatingActionButton(
                text = { Text("Add item") },
                icon = { GIcon(Ic.add) },
                onClick = {
                    haptics.click()
                    nav.go(ItemEdit())
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            )
        }
    }
}

/** One item in the list: its shape, name, shelf level and price. */
@Composable
fun ItemRow(item: ItemEntity) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ShapeAvatar(item.name, item.category)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(item.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StockGauge(item.stockMilli, item.minStockMilli, item.level, Modifier.width(64.dp))
                Text(
                    when (item.level) {
                        StockLevel.OUT -> "Sold out"
                        StockLevel.LOW -> "${Qty.withUnit(item.stockMilli, item.unit)} · low"
                        StockLevel.OK -> Qty.withUnit(item.stockMilli, item.unit)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = when (item.level) {
                        StockLevel.OUT -> scheme.error
                        StockLevel.LOW -> scheme.tertiary
                        StockLevel.OK -> scheme.onSurfaceVariant
                    },
                    maxLines = 1,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(Money.format(item.pricePaise), style = GallaType.amount)
            if (item.category.isNotBlank()) {
                Text(item.category, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}
