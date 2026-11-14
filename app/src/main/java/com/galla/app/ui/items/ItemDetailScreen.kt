package com.galla.app.ui.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.galla.app.core.model.MoveType
import com.galla.app.core.model.StockLevel
import com.galla.app.core.util.Codes
import com.galla.app.core.util.Dates
import com.galla.app.core.util.Money
import com.galla.app.core.util.Qty
import com.galla.app.data.local.db.ItemEntity
import com.galla.app.data.local.db.MovementEntity
import com.galla.app.hardware.Printer
import com.galla.app.ui.components.AppBar
import com.galla.app.ui.components.Btn
import com.galla.app.ui.components.BtnSize
import com.galla.app.ui.components.BtnStyle
import com.galla.app.ui.components.CardKind
import com.galla.app.ui.components.EmptyState
import com.galla.app.ui.components.Eyebrow
import com.galla.app.ui.components.GCard
import com.galla.app.ui.components.GGroup
import com.galla.app.ui.components.Gap
import com.galla.app.ui.components.GroupItem
import com.galla.app.ui.components.IconBtn
import com.galla.app.ui.components.QrImage
import com.galla.app.ui.components.RollingText
import com.galla.app.ui.components.RowContent
import com.galla.app.ui.components.ScreenColumn
import com.galla.app.ui.components.SectionHeader
import com.galla.app.ui.components.ShapeAvatar
import com.galla.app.ui.components.ShapeIcon
import com.galla.app.ui.components.StatTile
import com.galla.app.ui.components.StockGauge
import com.galla.app.ui.components.Tag
import com.galla.app.ui.components.TagKind
import com.galla.app.ui.components.asState
import com.galla.app.ui.components.rememberHaptics
import com.galla.app.ui.icons.Ic
import com.galla.app.ui.nav.ItemDetail
import com.galla.app.ui.nav.ItemEdit
import com.galla.app.ui.nav.LocalContainer
import com.galla.app.ui.nav.LocalMessenger
import com.galla.app.ui.nav.LocalNav
import com.galla.app.ui.nav.StockCount
import com.galla.app.ui.nav.StockIn
import com.galla.app.ui.theme.GallaTheme
import com.galla.app.ui.theme.GallaType
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** Everything the item screen shows. */
data class ItemDetailUi(
    val loaded: Boolean = false,
    val item: ItemEntity? = null,
    val history: List<MovementEntity> = emptyList(),
    val shopName: String = "",
)

class ItemDetailViewModel(private val app: AppContainer, private val itemId: Long) : ViewModel() {

    val state: StateFlow<ItemDetailUi> =
        combine(app.shop.item(itemId), app.shop.movements(itemId), app.settings.settings) { item, history, settings ->
            ItemDetailUi(loaded = true, item = item, history = history, shopName = settings.shopName)
        }.asState(this, ItemDetailUi())

    fun setArchived(archived: Boolean) {
        viewModelScope.launch { app.shop.setArchived(itemId, archived) }
    }

    /** Adds one to the bill. Returns a sentence to show if it could not. */
    fun addToBill(item: ItemEntity): String? = app.cart.addOne(item)

    fun now(): Long = app.now()
}

/**
 * One item: how much is on the shelf, what it costs and earns, its QR sticker, and the
 * full record of every time its stock changed.
 */
@Composable
fun ItemDetailScreen(route: ItemDetail) {
    val app = LocalContainer.current
    val nav = LocalNav.current
    val messenger = LocalMessenger.current
    val haptics = rememberHaptics()
    val context = LocalContext.current
    val vm: ItemDetailViewModel = viewModel(key = "item-${route.itemId}") { ItemDetailViewModel(app, route.itemId) }
    val ui by vm.state.collectAsStateWithLifecycle()
    val scheme = MaterialTheme.colorScheme
    var confirmArchive by remember { mutableStateOf(false) }
    val item = ui.item

    ScreenColumn {
        AppBar(
            onBack = { nav.back() },
            actions = {
                if (item != null && !item.archived) {
                    IconBtn(Ic.edit, "Edit this item", { nav.go(ItemEdit(item.id)) })
                    IconBtn(Ic.archive, "Archive this item", { confirmArchive = true })
                }
            },
        )

        if (item == null) {
            if (ui.loaded) EmptyState("Item not found", "It may have been removed.", Ic.search_off)
            return@ScreenColumn
        }

        // Name
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ShapeAvatar(item.name, item.category, size = 76.dp)
            Column(Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.headlineMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Text(
                    listOf(item.category, item.code).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
        Gap(16.dp)

        if (item.archived) {
            GCard(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                kind = CardKind.SECONDARY,
            ) {
                Text("This item is archived", style = MaterialTheme.typography.titleMedium)
                Text("It is hidden from the shop and cannot be sold. Its history is kept.", style = MaterialTheme.typography.bodyMedium)
                Gap(12.dp)
                Btn("Bring it back", { vm.setArchived(false) }, icon = Ic.unarchive, style = BtnStyle.TONAL, size = BtnSize.SM)
            }
            Gap(10.dp)
        }

        // Stock
        GCard(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            kind = when (item.level) {
                StockLevel.OK -> CardKind.PLAIN
                StockLevel.LOW -> CardKind.WARN
                StockLevel.OUT -> CardKind.ERROR
            },
            padding = 20.dp,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Eyebrow("On the shelf", Modifier.weight(1f), color = androidx.compose.material3.LocalContentColor.current)
                when (item.level) {
                    StockLevel.OK -> Tag("Healthy", TagKind.GOOD, icon = Ic.check_circle)
                    StockLevel.LOW -> Tag("Running low", TagKind.NEUTRAL, icon = Ic.warning)
                    StockLevel.OUT -> Tag("Sold out", TagKind.NEUTRAL, icon = Ic.error)
                }
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RollingText(Qty.format(item.stockMilli), style = GallaType.hero)
                Text(item.unit, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 10.dp))
            }
            Gap(6.dp)
            StockGauge(item.stockMilli, item.minStockMilli, item.level, Modifier.fillMaxWidth())
            Gap(8.dp)
            Text(
                "Low at ${Qty.withUnit(item.minStockMilli, item.unit)} or less · worth ${Money.format(item.stockValuePaise)}",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (!item.archived) {
                Gap(16.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Btn("Stock in", { nav.go(StockIn(item.id)) }, Modifier.weight(1f), icon = Ic.local_shipping, style = BtnStyle.FILLED)
                    Btn("Count", { nav.go(StockCount(item.id)) }, Modifier.weight(1f), icon = Ic.checklist, style = BtnStyle.OUTLINE)
                }
            }
        }
        Gap(10.dp)

        // Prices
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatTile("Buy", Money.format(item.costPaise), Modifier.weight(1f), valueStyle = MaterialTheme.typography.titleLarge)
            StatTile("Sell", Money.format(item.pricePaise), Modifier.weight(1f), valueStyle = MaterialTheme.typography.titleLarge)
            StatTile(
                "Margin",
                "${item.marginPercent}%",
                Modifier.weight(1f),
                kind = if (item.pricePaise < item.costPaise) CardKind.ERROR else CardKind.INK,
                valueStyle = MaterialTheme.typography.titleLarge,
            )
        }
        Gap(10.dp)

        // Sticker
        GCard(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                QrImage(Codes.qrPayload(item.code), Modifier.size(128.dp))
                Column(Modifier.weight(1f)) {
                    Eyebrow("QR sticker")
                    Text(item.code, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Print it and stick it on the item or its shelf. Scan it to sell.",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                    Gap(10.dp)
                    Btn(
                        "Print",
                        {
                            val ok = Printer.print(context, "Sticker ${item.code}", Printer.labelBitmap(ui.shopName, item))
                            if (!ok) messenger.show("This phone cannot print.")
                        },
                        icon = Ic.print,
                        style = BtnStyle.TONAL,
                        size = BtnSize.SM,
                    )
                }
            }
        }

        if (!item.archived) {
            Gap(12.dp)
            Btn(
                text = if (item.level == StockLevel.OUT) "Sold out" else "Add to the bill",
                onClick = {
                    val problem = vm.addToBill(item)
                    if (problem == null) {
                        haptics.confirm()
                        messenger.show("${item.name} is on the bill")
                    } else {
                        haptics.reject()
                        messenger.show(problem)
                    }
                },
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                icon = Ic.add_shopping_cart,
                size = BtnSize.LG,
                enabled = item.level != StockLevel.OUT,
            )
        }

        // The stock ledger for this item
        SectionHeader("History")
        if (ui.history.isEmpty()) {
            Text(
                "No stock has moved yet.",
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        } else {
            val now = vm.now()
            GGroup {
                ui.history.forEachIndexed { index, move ->
                    GroupItem(index, ui.history.size) {
                        val coming = move.qtyMilli >= 0
                        RowContent(
                            title = when (move.type) {
                                MoveType.SALE -> "Sold"
                                MoveType.PURCHASE -> "Stock in"
                                MoveType.OPENING -> "Opening stock"
                                else -> "Counted"
                            },
                            // The opening row's note only repeats its title, so leave it out.
                            subtitle = listOf(if (move.type == MoveType.OPENING) "" else move.note, Dates.friendly(move.at, now))
                                .filter { it.isNotBlank() }.joinToString(" · "),
                            leading = {
                                ShapeIcon(
                                    when (move.type) {
                                        MoveType.SALE -> Ic.receipt_long
                                        MoveType.PURCHASE -> Ic.local_shipping
                                        MoveType.OPENING -> Ic.package_2
                                        else -> Ic.checklist
                                    },
                                    size = 40.dp,
                                    background = scheme.surfaceContainerHighest,
                                    tint = scheme.onSurfaceVariant,
                                )
                            },
                            trailing = {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        (if (coming) "+" else "-") + Qty.format(kotlin.math.abs(move.qtyMilli)),
                                        style = GallaType.amount,
                                        color = if (coming) GallaTheme.extras.good else scheme.onSurface,
                                    )
                                    Text(
                                        "${Qty.format(move.balanceMilli)} left",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = scheme.onSurfaceVariant,
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }
        Gap(12.dp)
    }

    if (confirmArchive && item != null) {
        AlertDialog(
            onDismissRequest = { confirmArchive = false },
            title = { Text("Archive ${item.name}?") },
            text = { Text("It will be hidden from the shop and cannot be sold. Old bills and its history are kept, and you can bring it back from Settings.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmArchive = false
                    vm.setArchived(true)
                    messenger.show("${item.name} archived")
                    nav.back()
                }) { Text("Archive") }
            },
            dismissButton = { TextButton(onClick = { confirmArchive = false }) { Text("Cancel") } },
        )
    }
}
