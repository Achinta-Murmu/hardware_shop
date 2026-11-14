package com.galla.app.ui.sell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.galla.app.core.model.StockLevel
import com.galla.app.core.model.Units
import com.galla.app.core.util.Money
import com.galla.app.core.util.Qty
import com.galla.app.data.local.db.ItemEntity
import com.galla.app.ui.components.Btn
import com.galla.app.ui.components.BtnSize
import com.galla.app.ui.components.EmptyState
import com.galla.app.ui.components.Eyebrow
import com.galla.app.ui.components.FieldKind
import com.galla.app.ui.components.GField
import com.galla.app.ui.components.Gap
import com.galla.app.ui.components.GroupItem
import com.galla.app.ui.components.IconBtn
import com.galla.app.ui.components.IconBtnStyle
import com.galla.app.ui.components.QtyStepper
import com.galla.app.ui.components.RollingText
import com.galla.app.ui.components.RowContent
import com.galla.app.ui.components.SearchField
import com.galla.app.ui.components.ShapeAvatar
import com.galla.app.ui.components.TabHeader
import com.galla.app.ui.components.Tag
import com.galla.app.ui.components.TagKind
import com.galla.app.ui.components.rememberHaptics
import com.galla.app.ui.icons.Ic
import com.galla.app.ui.nav.ItemEdit
import com.galla.app.ui.nav.Items
import com.galla.app.ui.nav.LocalContainer
import com.galla.app.ui.nav.LocalMessenger
import com.galla.app.ui.nav.LocalNav
import com.galla.app.ui.nav.Receipt
import com.galla.app.ui.nav.Scan
import com.galla.app.ui.theme.GallaRadius
import com.galla.app.ui.theme.GallaType

/**
 * The Sell tab: the counter. Items get onto the bill by scanning their sticker or by
 * typing a few letters. The total and the one button that matters stay pinned at the bottom.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SellScreen() {
    val app = LocalContainer.current
    val nav = LocalNav.current
    val messenger = LocalMessenger.current
    val haptics = rememberHaptics()
    val vm: SellViewModel = viewModel { SellViewModel(app) }
    val ui by vm.state.collectAsStateWithLifecycle()
    val scheme = MaterialTheme.colorScheme
    /** The line whose quantity is being typed, if the box is open. */
    var typing by remember { mutableStateOf<CartLine?>(null) }

    fun tell(problem: String?) {
        if (problem != null) {
            haptics.reject()
            messenger.show(problem)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(scheme.surface)
            .statusBarsPadding(),
    ) {
        TabHeader(
            title = "Counter",
            eyebrow = when {
                ui.lines.isEmpty() -> "New bill"
                ui.lines.size == 1 -> "1 item on the bill"
                else -> "${ui.lines.size} items on the bill"
            },
            actions = {
                if (ui.lines.isNotEmpty()) IconBtn(Ic.delete, "Clear the bill", { vm.clear() })
            },
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SearchField(ui.query, vm::setQuery, "Find an item to add", Modifier.weight(1f))
            IconBtn(Ic.qr_code_scanner, "Scan a sticker", { nav.go(Scan("sell")) }, style = IconBtnStyle.FILLED, size = 56.dp, iconSize = 26.dp)
        }
        Gap(10.dp)

        when {
            // 1. Searching: show matching items; a tap adds one and goes back to the bill.
            ui.searching -> {
                if (ui.results.isEmpty()) {
                    EmptyState("Nothing matches", "No item has \"${ui.query.trim()}\" in its name, category or code.", Ic.search_off, Modifier.weight(1f))
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                    ) {
                        itemsIndexed(ui.results, key = { _, item -> item.id }) { index, item ->
                            GroupItem(index, ui.results.size, onClick = {
                                val problem = vm.add(item)
                                if (problem == null) {
                                    haptics.tick()
                                    vm.setQuery("")
                                } else {
                                    tell(problem)
                                }
                            }) {
                                SearchResultRow(item)
                            }
                        }
                    }
                }
            }

            // 2. Nothing on the bill yet.
            ui.lines.isEmpty() -> {
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    if (ui.loaded && !ui.shopHasItems) {
                        EmptyState(
                            title = "No items to sell yet",
                            text = "Add the things your shop sells first. Then they can go on a bill.",
                            icon = Ic.inventory_2,
                            action = { Btn("Add an item", { nav.openIn(Items, ItemEdit()) }, icon = Ic.add) },
                        )
                    } else {
                        EmptyState(
                            title = "Start a bill",
                            text = "Scan the sticker on an item, or type a few letters of its name.",
                            icon = Ic.point_of_sale,
                            action = { Btn("Scan a sticker", { nav.go(Scan("sell")) }, icon = Ic.qr_code_scanner, size = BtnSize.LG) },
                        )
                    }
                }
            }

            // 3. The bill.
            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                ) {
                    itemsIndexed(ui.lines, key = { _, line -> line.item.id }) { index, line ->
                        GroupItem(index, ui.lines.size) {
                            CartRow(
                                line = line,
                                onMinus = { vm.minus(line) },
                                onPlus = { tell(vm.plus(line)) },
                                onType = { typing = line },
                                onRemove = { vm.remove(line) },
                            )
                        }
                    }
                }
                // The total and the save button, pinned to the bottom.
                Surface(
                    color = scheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(topStart = GallaRadius.l, topEnd = GallaRadius.l),
                ) {
                    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 16.dp)) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Column(Modifier.weight(1f)) {
                                Eyebrow("Total")
                                RollingText(Money.format(ui.totalPaise), style = GallaType.figure)
                            }
                            Text(
                                "${ui.lines.size} ${if (ui.lines.size == 1) "item" else "items"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = scheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 6.dp),
                            )
                        }
                        Gap(12.dp)
                        Btn(
                            text = if (ui.saving) "Saving" else "Save sale",
                            onClick = {
                                vm.save(
                                    onSaved = { saleId ->
                                        haptics.confirm()
                                        nav.go(Receipt(saleId, fresh = true))
                                    },
                                    onProblem = { tell(it) },
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            icon = Ic.check,
                            size = BtnSize.LG,
                            enabled = !ui.saving,
                        )
                    }
                }
            }
        }
    }

    val line = typing
    if (line != null) {
        QuantityDialog(
            line = line,
            onDismiss = { typing = null },
            onSet = { text ->
                val problem = vm.setQuantity(line, text)
                if (problem == null) typing = null
                problem
            },
        )
    }
}

/** A row in the search results: tap to put one on the bill. */
@Composable
private fun SearchResultRow(item: ItemEntity) {
    val scheme = MaterialTheme.colorScheme
    RowContent(
        title = item.name,
        subtitle = when (item.level) {
            StockLevel.OUT -> "Sold out"
            else -> "${Qty.withUnit(item.stockMilli, item.unit)} in stock"
        },
        leading = { ShapeAvatar(item.name, item.category, size = 44.dp) },
        trailing = {
            Column(horizontalAlignment = Alignment.End) {
                Text(Money.format(item.pricePaise), style = GallaType.amount)
                Text("per ${item.unit}", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
        },
    )
}

/** One line of the bill: what it is, how many, what it comes to. */
@Composable
private fun CartRow(line: CartLine, onMinus: () -> Unit, onPlus: () -> Unit, onType: () -> Unit, onRemove: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.padding(start = 14.dp, end = 10.dp, top = 12.dp, bottom = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ShapeAvatar(line.item.name, line.item.category, size = 44.dp)
            Column(Modifier.weight(1f)) {
                Text(line.item.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    "${Money.format(line.item.pricePaise)} per ${line.item.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            Text(Money.format(line.totalPaise), style = GallaType.amount, modifier = Modifier.padding(end = 6.dp))
        }
        Gap(6.dp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBtn(Ic.close, "Remove ${line.item.name}", onRemove, size = 40.dp, iconSize = 20.dp, tint = scheme.onSurfaceVariant)
            if (line.tooMany) Tag("Only ${Qty.withUnit(line.item.stockMilli, line.item.unit)} left", TagKind.BAD)
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
                QtyStepper(
                    text = Qty.format(line.qtyMilli),
                    onMinus = onMinus,
                    onPlus = onPlus,
                    onType = onType,
                    canPlus = line.qtyMilli < line.item.stockMilli,
                )
            }
        }
    }
}

/** A small box to type an exact quantity, for example 2.5 metres. */
@Composable
private fun QuantityDialog(line: CartLine, onDismiss: () -> Unit, onSet: (String) -> String?) {
    var text by remember { mutableStateOf(Qty.format(line.qtyMilli)) }
    var problem by remember { mutableStateOf<String?>(null) }
    val unit = Units.of(line.item.unit)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("How much?") },
        text = {
            Column {
                Text(line.item.name, style = MaterialTheme.typography.bodyLarge)
                Gap(12.dp)
                GField(
                    value = text,
                    onValueChange = {
                        text = it
                        problem = null
                    },
                    label = "Quantity",
                    kind = FieldKind.QUANTITY,
                    suffix = unit.key,
                    supporting = "${Qty.withUnit(line.item.stockMilli, line.item.unit)} in stock",
                    error = problem,
                    last = true,
                )
            }
        },
        confirmButton = { TextButton(onClick = { problem = onSet(text) }) { Text("Set") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
