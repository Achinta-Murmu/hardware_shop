package com.galla.app.ui.stock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.galla.app.AppContainer
import com.galla.app.core.model.StockLevel
import com.galla.app.core.model.Units
import com.galla.app.core.util.Money
import com.galla.app.core.util.Qty
import com.galla.app.data.local.db.ItemEntity
import com.galla.app.data.repo.ShopException
import com.galla.app.hardware.Printer
import com.galla.app.ui.components.AmountRow
import com.galla.app.ui.components.AppBar
import com.galla.app.ui.components.Btn
import com.galla.app.ui.components.BtnSize
import com.galla.app.ui.components.BtnStyle
import com.galla.app.ui.components.CardKind
import com.galla.app.ui.components.Chip
import com.galla.app.ui.components.ChipRow
import com.galla.app.ui.components.EmptyState
import com.galla.app.ui.components.Eyebrow
import com.galla.app.ui.components.FieldKind
import com.galla.app.ui.components.GCard
import com.galla.app.ui.components.GField
import com.galla.app.ui.components.GGroup
import com.galla.app.ui.components.Gap
import com.galla.app.ui.components.GroupItem
import com.galla.app.ui.components.RowContent
import com.galla.app.ui.components.ScreenColumn
import com.galla.app.ui.components.SearchField
import com.galla.app.ui.components.ShapeAvatar
import com.galla.app.ui.components.StockGauge
import com.galla.app.ui.components.asState
import com.galla.app.ui.components.rememberHaptics
import com.galla.app.ui.icons.Ic
import com.galla.app.ui.items.ItemRow
import com.galla.app.ui.nav.ItemEdit
import com.galla.app.ui.nav.LocalContainer
import com.galla.app.ui.nav.LocalMessenger
import com.galla.app.ui.nav.LocalNav
import com.galla.app.ui.nav.StockCount
import com.galla.app.ui.nav.StockIn
import com.galla.app.ui.sell.matches
import com.galla.app.ui.theme.GallaType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// Shared: one ViewModel that watches the item list and does the two stock actions
// ---------------------------------------------------------------------------

data class StockUi(
    val loaded: Boolean = false,
    val items: List<ItemEntity> = emptyList(),
    val shopName: String = "",
) {
    fun item(id: Long): ItemEntity? = items.firstOrNull { it.id == id }

    /** Items that are low or sold out, most urgent first. */
    val restock: List<ItemEntity>
        get() = items.filter { it.level != StockLevel.OK }
            .sortedWith(compareBy({ it.level != StockLevel.OUT }, { it.stockMilli.toDouble() / (it.minStockMilli + 1) }))
}

class StockViewModel(private val app: AppContainer) : ViewModel() {

    /** The item picked on the "which item?" step of Stock in. */
    val picked = MutableStateFlow(0L)

    val state: StateFlow<StockUi> =
        combine(app.shop.items, app.settings.settings) { items, settings -> StockUi(true, items, settings.shopName) }
            .asState(this, StockUi())

    private fun attempt(onDone: () -> Unit, onProblem: (String) -> Unit, action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
                onDone()
            } catch (e: ShopException) {
                onProblem(e.message ?: "That did not work.")
            } catch (e: Exception) {
                onProblem("That did not work. Please try again.")
            }
        }
    }

    fun stockIn(itemId: Long, qtyMilli: Long, unitCostPaise: Long, supplier: String, onDone: () -> Unit, onProblem: (String) -> Unit) =
        attempt(onDone, onProblem) { app.shop.stockIn(itemId, qtyMilli, unitCostPaise, supplier) }

    fun correct(itemId: Long, countedMilli: Long, reason: String, onDone: () -> Unit, onProblem: (String) -> Unit) =
        attempt(onDone, onProblem) { app.shop.correctStock(itemId, countedMilli, reason) }
}

/** How much to buy to fill the shelf back up: enough to reach three times the "low" level. */
fun suggestedOrder(item: ItemEntity): Long {
    val target = if (item.minStockMilli > 0L) item.minStockMilli * 3 else Qty.ONE
    return (target - item.stockMilli).coerceAtLeast(Qty.ONE)
}

// ---------------------------------------------------------------------------
// Stock in: goods arriving from a supplier
// ---------------------------------------------------------------------------

@Composable
fun StockInScreen(route: StockIn) {
    val app = LocalContainer.current
    val nav = LocalNav.current
    val messenger = LocalMessenger.current
    val haptics = rememberHaptics()
    val vm: StockViewModel = viewModel(key = "stock-in-${route.itemId}") { StockViewModel(app) }
    val ui by vm.state.collectAsStateWithLifecycle()
    val pickedId by vm.picked.collectAsStateWithLifecycle()
    val itemId = if (route.itemId != 0L) route.itemId else pickedId
    val item = ui.item(itemId)
    val scheme = MaterialTheme.colorScheme

    var query by rememberSaveable { mutableStateOf("") }
    var qty by rememberSaveable(itemId) { mutableStateOf("") }
    var cost by rememberSaveable(itemId) { mutableStateOf("") }
    var supplier by rememberSaveable { mutableStateOf("") }
    var qtyError by rememberSaveable(itemId) { mutableStateOf<String?>(null) }
    var costError by rememberSaveable(itemId) { mutableStateOf<String?>(null) }

    ScreenColumn {
        AppBar(
            onBack = {
                // From the form, Back returns to the item list first (when the item was picked here).
                if (route.itemId == 0L && pickedId != 0L) vm.picked.value = 0L else nav.back()
            },
            title = "Stock in",
            subtitle = if (item == null) "Which item came in?" else "",
        )

        if (item == null) {
            // Step 1: pick the item.
            if (ui.loaded && ui.items.isEmpty()) {
                EmptyState(
                    title = "No items yet",
                    text = "Add an item first. Then you can record stock coming in for it.",
                    icon = Ic.inventory_2,
                    action = { Btn("Add an item", { nav.replaceTop(ItemEdit()) }, icon = Ic.add) },
                )
                return@ScreenColumn
            }
            SearchField(query, { query = it }, "Search items", Modifier.padding(horizontal = 16.dp))
            Gap(10.dp)
            val matches = ui.items.filter { it.matches(query) }
            GGroup {
                matches.forEachIndexed { index, candidate ->
                    GroupItem(index, matches.size, onClick = { vm.picked.value = candidate.id }) { ItemRow(candidate) }
                }
            }
            if (matches.isEmpty() && ui.loaded) {
                Text(
                    "No item matches \"${query.trim()}\".",
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(20.dp),
                )
            }
            return@ScreenColumn
        }

        // Step 2: how much, at what price, from whom.
        val unit = Units.of(item.unit)
        val qtyMilli = Qty.parse(qty, unit.fractional)
        val costPaise = Money.parse(cost.ifBlank { Money.plain(item.costPaise) })

        ItemHeader(item)
        Gap(14.dp)
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            GField(
                value = qty,
                onValueChange = {
                    qty = it
                    qtyError = null
                },
                label = "How much came in",
                kind = FieldKind.QUANTITY,
                suffix = unit.key,
                supporting = "You have ${Qty.withUnit(item.stockMilli, item.unit)} now.",
                error = qtyError,
            )
            GField(
                value = cost,
                onValueChange = {
                    cost = it
                    costError = null
                },
                label = "Price paid for one ${unit.label.lowercase()}",
                kind = FieldKind.MONEY,
                prefix = "₹",
                supporting = "Leave empty to keep ${Money.format(item.costPaise)}.",
                error = costError,
            )
            GField(
                value = supplier,
                onValueChange = { supplier = it },
                label = "Supplier (optional)",
                kind = FieldKind.NAME,
                last = true,
            )
        }
        Gap(12.dp)
        GCard(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            kind = CardKind.INK,
        ) {
            AmountRow(
                "You pay",
                Money.format(if (qtyMilli != null && costPaise != null) Qty.times(costPaise, qtyMilli) else 0L),
                strong = true,
            )
            if (qtyMilli != null && qtyMilli > 0L) {
                Text(
                    "Stock goes from ${Qty.format(item.stockMilli)} to ${Qty.withUnit(item.stockMilli + qtyMilli, item.unit)}.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Gap(16.dp)
        Btn(
            text = "Save stock in",
            onClick = {
                qtyError = when {
                    qtyMilli == null -> if (unit.fractional) "Enter a number." else "Enter a whole number."
                    qtyMilli <= 0L -> "Enter how much came in."
                    else -> null
                }
                costError = if (costPaise == null) "Enter a price in rupees." else null
                if (qtyMilli != null && qtyMilli > 0L && costPaise != null) {
                    vm.stockIn(
                        item.id, qtyMilli, costPaise, supplier,
                        onDone = {
                            haptics.confirm()
                            messenger.show("${Qty.withUnit(qtyMilli, item.unit)} of ${item.name} added to stock")
                            nav.back()
                        },
                        onProblem = { messenger.show(it) },
                    )
                } else {
                    haptics.reject()
                }
            },
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            icon = Ic.check,
            size = BtnSize.LG,
        )
    }
}

/** The item a stock form is about. */
@Composable
private fun ItemHeader(item: ItemEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ShapeAvatar(item.name, item.category, size = 56.dp)
        Column(Modifier.weight(1f)) {
            Text(item.name, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(item.code, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ---------------------------------------------------------------------------
// Count: "I counted the shelf, the real number is ..."
// ---------------------------------------------------------------------------

private val countReasons = listOf("Counted the shelf", "Damaged", "Lost", "Returned by customer", "Used in the shop")

@Composable
fun StockCountScreen(route: StockCount) {
    val app = LocalContainer.current
    val nav = LocalNav.current
    val messenger = LocalMessenger.current
    val haptics = rememberHaptics()
    val vm: StockViewModel = viewModel(key = "count-${route.itemId}") { StockViewModel(app) }
    val ui by vm.state.collectAsStateWithLifecycle()
    val item = ui.item(route.itemId)

    var counted by rememberSaveable { mutableStateOf("") }
    var reason by rememberSaveable { mutableStateOf(countReasons.first()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    ScreenColumn {
        AppBar(onBack = { nav.back() }, title = "Count stock")
        if (item == null) {
            if (ui.loaded) EmptyState("Item not found", "It may have been removed.", Ic.search_off)
            return@ScreenColumn
        }
        val unit = Units.of(item.unit)
        val countedMilli = Qty.parse(counted, unit.fractional)

        ItemHeader(item)
        Gap(14.dp)
        Text(
            "Galla thinks there are ${Qty.withUnit(item.stockMilli, item.unit)}. Count the shelf and enter what is really there.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Gap(12.dp)
        GField(
            value = counted,
            onValueChange = {
                counted = it
                error = null
            },
            label = "Really on the shelf",
            modifier = Modifier.padding(horizontal = 16.dp),
            kind = FieldKind.QUANTITY,
            suffix = unit.key,
            error = error,
            last = true,
        )
        Gap(10.dp)
        Eyebrow("Why is it different?", Modifier.padding(horizontal = 20.dp))
        Gap(4.dp)
        ChipRow {
            countReasons.forEach { option -> Chip(option, { reason = option }, selected = reason == option) }
        }
        if (countedMilli != null && countedMilli != item.stockMilli) {
            val difference = countedMilli - item.stockMilli
            Gap(12.dp)
            GCard(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                kind = if (difference < 0L) CardKind.WARN else CardKind.SECONDARY,
            ) {
                Text(
                    if (difference < 0L) "${Qty.withUnit(-difference, item.unit)} will be taken off the stock."
                    else "${Qty.withUnit(difference, item.unit)} will be added to the stock.",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text("The change is written to this item's history.", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Gap(16.dp)
        Btn(
            text = "Save count",
            onClick = {
                if (countedMilli == null) {
                    error = if (unit.fractional) "Enter a number." else "Enter a whole number."
                    haptics.reject()
                } else {
                    vm.correct(
                        item.id, countedMilli, reason,
                        onDone = {
                            haptics.confirm()
                            messenger.show("Stock of ${item.name} set to ${Qty.withUnit(countedMilli, item.unit)}")
                            nav.back()
                        },
                        onProblem = { messenger.show(it) },
                    )
                }
            },
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            icon = Ic.check,
            size = BtnSize.LG,
        )
    }
}

// ---------------------------------------------------------------------------
// Restock: what to buy
// ---------------------------------------------------------------------------

@Composable
fun RestockScreen() {
    val app = LocalContainer.current
    val nav = LocalNav.current
    val context = LocalContext.current
    val vm: StockViewModel = viewModel(key = "restock") { StockViewModel(app) }
    val ui by vm.state.collectAsStateWithLifecycle()
    val list = ui.restock
    val scheme = MaterialTheme.colorScheme

    ScreenColumn {
        AppBar(
            onBack = { nav.back() },
            title = "To restock",
            subtitle = when {
                !ui.loaded -> ""
                list.isEmpty() -> "Nothing is low"
                list.size == 1 -> "1 item is low or sold out"
                else -> "${list.size} items are low or sold out"
            },
        )
        if (list.isEmpty()) {
            if (ui.loaded) EmptyState("All shelves are healthy", "Nothing is at or below its low level right now.", Ic.check_circle)
            return@ScreenColumn
        }
        Btn(
            text = "Send this list",
            onClick = {
                val text = buildString {
                    appendLine("To buy for ${ui.shopName.ifBlank { "the shop" }}:")
                    list.forEach { item ->
                        appendLine("- ${item.name}: ${Qty.withUnit(suggestedOrder(item), item.unit)} (have ${Qty.format(item.stockMilli)})")
                    }
                }.trim()
                Printer.shareText(context, "Restock list", text)
            },
            modifier = Modifier.padding(horizontal = 16.dp),
            icon = Ic.share,
            style = BtnStyle.TONAL,
        )
        Gap(12.dp)
        GGroup {
            list.forEachIndexed { index, item ->
                GroupItem(index, list.size, onClick = { nav.go(StockIn(item.id)) }) {
                    RowContent(
                        title = item.name,
                        subtitle = if (item.level == StockLevel.OUT) "Sold out · keep at least ${Qty.withUnit(item.minStockMilli, item.unit)}"
                        else "${Qty.withUnit(item.stockMilli, item.unit)} left · low at ${Qty.format(item.minStockMilli)}",
                        leading = { ShapeAvatar(item.name, item.category) },
                        below = {
                            Gap(4.dp)
                            StockGauge(item.stockMilli, item.minStockMilli, item.level, Modifier.fillMaxWidth())
                        },
                        trailing = {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Buy", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                                Text(Qty.withUnit(suggestedOrder(item), item.unit), style = GallaType.amount)
                            }
                        },
                    )
                }
            }
        }
        Gap(8.dp)
        Text(
            "\"Buy\" fills the shelf to three times its low level. Tap an item when the stock arrives.",
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
    }
}
