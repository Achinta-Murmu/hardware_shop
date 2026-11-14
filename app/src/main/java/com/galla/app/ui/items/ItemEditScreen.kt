package com.galla.app.ui.items

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.galla.app.AppContainer
import com.galla.app.core.model.Units
import com.galla.app.core.util.Money
import com.galla.app.core.util.Qty
import com.galla.app.data.repo.ItemDraft
import com.galla.app.data.repo.ShopException
import com.galla.app.ui.components.AppBar
import com.galla.app.ui.components.Btn
import com.galla.app.ui.components.BtnSize
import com.galla.app.ui.components.CardKind
import com.galla.app.ui.components.Chip
import com.galla.app.ui.components.ChipRow
import com.galla.app.ui.components.Eyebrow
import com.galla.app.ui.components.FieldKind
import com.galla.app.ui.components.GCard
import com.galla.app.ui.components.GField
import com.galla.app.ui.components.Gap
import com.galla.app.ui.components.ScreenColumn
import com.galla.app.ui.icons.Ic
import com.galla.app.ui.nav.ItemDetail
import com.galla.app.ui.nav.ItemEdit
import com.galla.app.ui.nav.LocalContainer
import com.galla.app.ui.nav.LocalMessenger
import com.galla.app.ui.nav.LocalNav
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** The form, as the text in each box. */
data class ItemForm(
    val name: String = "",
    val category: String = "",
    val unit: String = "pcs",
    val cost: String = "",
    val price: String = "",
    val stock: String = "",
    val min: String = "5",
    val nameError: String? = null,
    val costError: String? = null,
    val priceError: String? = null,
    val stockError: String? = null,
    val minError: String? = null,
)

class ItemEditViewModel(private val app: AppContainer, private val itemId: Long) : ViewModel() {

    val isNew: Boolean = itemId == 0L

    var form by mutableStateOf(ItemForm())
        private set

    /** Categories already used in the shop, offered as one-tap choices. */
    var categories by mutableStateOf<List<String>>(emptyList())
        private set

    var saving by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            categories = app.shop.items.first().map { it.category }.filter { it.isNotBlank() }.distinct().sorted()
            if (!isNew) {
                app.shop.getItem(itemId)?.let { item ->
                    form = ItemForm(
                        name = item.name,
                        category = item.category,
                        unit = item.unit,
                        cost = Money.plain(item.costPaise),
                        price = Money.plain(item.pricePaise),
                        stock = Qty.format(item.stockMilli),
                        min = Qty.format(item.minStockMilli),
                    )
                }
            }
        }
    }

    fun change(update: (ItemForm) -> ItemForm) {
        form = update(form)
    }

    /** Checks the form and saves. Calls `onSaved` with the item id, or `onProblem` with a sentence. */
    fun save(onSaved: (Long) -> Unit, onProblem: (String) -> Unit) {
        if (saving) return
        val fractional = Units.of(form.unit).fractional
        val whole = "Enter a whole number for this unit."
        val cost = Money.parse(form.cost.ifBlank { "0" })
        val price = Money.parse(form.price)
        val stock = if (isNew) Qty.parse(form.stock.ifBlank { "0" }, fractional) else 0L
        val min = Qty.parse(form.min.ifBlank { "0" }, fractional)
        val checked = form.copy(
            nameError = if (form.name.isBlank()) "Give the item a name." else null,
            costError = if (cost == null) "Enter a price in rupees." else null,
            priceError = if (price == null) "Enter the price you sell it for." else null,
            stockError = if (stock == null) (if (fractional) "Enter a number." else whole) else null,
            minError = if (min == null) (if (fractional) "Enter a number." else whole) else null,
        )
        form = checked
        if (checked.nameError != null || cost == null || price == null || stock == null || min == null) {
            onProblem("Check the boxes marked in red.")
            return
        }
        saving = true
        viewModelScope.launch {
            try {
                val id = app.shop.saveItem(
                    ItemDraft(
                        id = itemId,
                        name = form.name,
                        category = form.category,
                        unit = form.unit,
                        costPaise = cost,
                        pricePaise = price,
                        minStockMilli = min,
                        openingStockMilli = stock,
                    ),
                )
                onSaved(id)
            } catch (e: ShopException) {
                onProblem(e.message ?: "Could not save the item.")
            } catch (e: Exception) {
                onProblem("Could not save the item. Please try again.")
            } finally {
                saving = false
            }
        }
    }
}

/** Add a new item, or change an existing one. */
@Composable
fun ItemEditScreen(route: ItemEdit) {
    val app = LocalContainer.current
    val nav = LocalNav.current
    val messenger = LocalMessenger.current
    val vm: ItemEditViewModel = viewModel(key = "edit-${route.itemId}") { ItemEditViewModel(app, route.itemId) }
    val form = vm.form
    val unit = Units.of(form.unit)
    val scheme = MaterialTheme.colorScheme

    ScreenColumn {
        AppBar(onBack = { nav.back() }, title = if (vm.isNew) "New item" else "Edit item")

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            GField(
                value = form.name,
                onValueChange = { text -> vm.change { it.copy(name = text, nameError = null) } },
                label = "Name",
                kind = FieldKind.NAME,
                supporting = "What you call it at the counter.",
                error = form.nameError,
            )
            GField(
                value = form.category,
                onValueChange = { text -> vm.change { it.copy(category = text) } },
                label = "Category (optional)",
                kind = FieldKind.NAME,
                supporting = "For example Paint, Electrical, Plumbing.",
            )
        }
        if (vm.categories.isNotEmpty()) {
            ChipRow {
                vm.categories.forEach { category ->
                    Chip(category, { vm.change { it.copy(category = category) } }, selected = form.category.trim().equals(category, ignoreCase = true))
                }
            }
        }

        Gap(14.dp)
        Eyebrow("Sold by", Modifier.padding(horizontal = 20.dp))
        Gap(4.dp)
        ChipRow {
            Units.all.forEach { option ->
                Chip(option.label, { vm.change { it.copy(unit = option.key, stockError = null, minError = null) } }, selected = form.unit == option.key)
            }
        }
        Gap(14.dp)

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GField(
                    value = form.cost,
                    onValueChange = { text -> vm.change { it.copy(cost = text, costError = null) } },
                    label = "You buy at",
                    modifier = Modifier.weight(1f),
                    kind = FieldKind.MONEY,
                    prefix = "₹",
                    error = form.costError,
                )
                GField(
                    value = form.price,
                    onValueChange = { text -> vm.change { it.copy(price = text, priceError = null) } },
                    label = "You sell at",
                    modifier = Modifier.weight(1f),
                    kind = FieldKind.MONEY,
                    prefix = "₹",
                    error = form.priceError,
                )
            }

            // What one sale earns, worked out as the owner types.
            val cost = Money.parse(form.cost.ifBlank { "0" })
            val price = Money.parse(form.price)
            if (cost != null && price != null && price > 0L) {
                val earn = price - cost
                GCard(
                    modifier = Modifier.fillMaxWidth(),
                    kind = if (earn < 0L) CardKind.ERROR else CardKind.HERO,
                    padding = 14.dp,
                ) {
                    Text(
                        if (earn < 0L) "You lose ${Money.format(-earn)} on each ${unit.label.lowercase()}"
                        else "You earn ${Money.format(earn)} on each ${unit.label.lowercase()} (${earn * 100 / price}%)",
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                Gap(4.dp)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (vm.isNew) {
                    GField(
                        value = form.stock,
                        onValueChange = { text -> vm.change { it.copy(stock = text, stockError = null) } },
                        label = "In stock now",
                        modifier = Modifier.weight(1f),
                        kind = FieldKind.QUANTITY,
                        suffix = unit.key,
                        error = form.stockError,
                    )
                }
                GField(
                    value = form.min,
                    onValueChange = { text -> vm.change { it.copy(min = text, minError = null) } },
                    label = "Low at",
                    modifier = Modifier.weight(1f),
                    kind = FieldKind.QUANTITY,
                    suffix = unit.key,
                    error = form.minError,
                    last = true,
                )
            }
            Text(
                if (vm.isNew) "\"Low at\" is when Galla should warn you to buy more."
                else "To change the stock itself, use Stock in or Count on the item's page. That keeps the history honest.",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }

        Gap(20.dp)
        Btn(
            text = if (vm.saving) "Saving" else if (vm.isNew) "Add item" else "Save changes",
            onClick = {
                vm.save(
                    onSaved = { id ->
                        messenger.show(if (vm.isNew) "Item added. Its sticker is ready to print." else "Changes saved")
                        if (vm.isNew) nav.replaceTop(ItemDetail(id)) else nav.back()
                    },
                    onProblem = { messenger.show(it) },
                )
            },
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            icon = Ic.check,
            size = BtnSize.LG,
            enabled = !vm.saving,
        )
        Gap(12.dp)
    }
}
