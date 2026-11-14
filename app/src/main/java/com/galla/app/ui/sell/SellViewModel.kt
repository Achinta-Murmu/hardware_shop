package com.galla.app.ui.sell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.galla.app.AppContainer
import com.galla.app.core.model.Units
import com.galla.app.core.util.Qty
import com.galla.app.data.local.db.ItemEntity
import com.galla.app.data.repo.ShopException
import com.galla.app.ui.components.asState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** One line of the bill being built. */
data class CartLine(val item: ItemEntity, val qtyMilli: Long) {
    val totalPaise: Long get() = Qty.times(item.pricePaise, qtyMilli)
    /** True when the bill asks for more than the shelf has (stock changed since it was added). */
    val tooMany: Boolean get() = qtyMilli > item.stockMilli
}

/** Everything the Sell tab shows. */
data class SellUi(
    val loaded: Boolean = false,
    val lines: List<CartLine> = emptyList(),
    val query: String = "",
    /** Items matching the search box. Empty while the box is empty. */
    val results: List<ItemEntity> = emptyList(),
    val shopHasItems: Boolean = false,
    val saving: Boolean = false,
) {
    val totalPaise: Long get() = lines.sumOf { it.totalPaise }
    val searching: Boolean get() = query.isNotBlank()
}

/**
 * ViewModel for the counter. The bill itself lives in the app-wide CartStore (so the scanner
 * can add to it too); this joins it with the live item list, so prices and stock on the
 * bill are always the current ones.
 */
class SellViewModel(private val app: AppContainer) : ViewModel() {

    private val query = MutableStateFlow("")
    private val saving = MutableStateFlow(false)

    val state: StateFlow<SellUi> =
        combine(app.shop.items, app.cart.lines, query, saving) { items, cart, query, saving ->
            val byId = items.associateBy { it.id }
            val text = query.trim()
            SellUi(
                loaded = true,
                lines = cart.mapNotNull { (id, qty) -> byId[id]?.let { CartLine(it, qty) } },
                query = query,
                results = if (text.isEmpty()) emptyList() else items.filter { it.matches(text) }.take(40),
                shopHasItems = items.isNotEmpty(),
                saving = saving,
            )
        }.asState(this, SellUi())

    fun setQuery(text: String) {
        query.value = text
    }

    /** Adds one of an item. Returns a sentence to show if it could not be added. */
    fun add(item: ItemEntity): String? = app.cart.addOne(item)

    fun plus(line: CartLine): String? = app.cart.addOne(line.item)

    fun minus(line: CartLine) {
        val next = line.qtyMilli - Qty.ONE
        app.cart.set(line.item.id, if (next < 0L) 0L else next)
    }

    fun remove(line: CartLine) = app.cart.remove(line.item.id)

    fun clear() = app.cart.clear()

    /** Sets a typed quantity. Returns a sentence to show if the text is not a usable quantity. */
    fun setQuantity(line: CartLine, text: String): String? {
        val unit = Units.of(line.item.unit)
        val qty = Qty.parse(text, allowFraction = unit.fractional)
            ?: return if (unit.fractional) "Enter a number." else "Enter a whole number. ${unit.label} items cannot be split."
        if (qty > line.item.stockMilli) return "Only ${Qty.withUnit(line.item.stockMilli, line.item.unit)} in stock."
        app.cart.set(line.item.id, qty)
        return null
    }

    /** Saves the bill. Calls `onSaved` with the new bill's id, or `onProblem` with a sentence to show. */
    fun save(onSaved: (Long) -> Unit, onProblem: (String) -> Unit) {
        if (saving.value) return
        saving.value = true
        viewModelScope.launch {
            try {
                val saleId = app.shop.completeSale(app.cart.lines.value)
                app.cart.clear()
                query.value = ""
                onSaved(saleId)
            } catch (e: ShopException) {
                onProblem(e.message ?: "Could not save the sale.")
            } catch (e: Exception) {
                onProblem("Could not save the sale. Please try again.")
            } finally {
                saving.value = false
            }
        }
    }
}

/** Does this item match what was typed? Looks at the name, the category and the sticker code. */
fun ItemEntity.matches(text: String): Boolean {
    val needle = text.trim().lowercase()
    if (needle.isEmpty()) return true
    return name.lowercase().contains(needle) ||
        category.lowercase().contains(needle) ||
        code.lowercase().replace("-", "").contains(needle.replace("-", ""))
}
