package com.galla.app.core.state

import com.galla.app.core.util.Qty
import com.galla.app.data.local.db.ItemEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * The bill being built at the counter: which items, and how many of each.
 * It lives for as long as the app is open, so switching tabs or scanning does not lose it.
 * It holds only item ids and quantities; names, prices and stock are always read fresh
 * from the database when the bill is shown.
 */
class CartStore {
    /** Item id to quantity (in thousandths of a unit), in the order items were added. */
    private val _lines = MutableStateFlow<Map<Long, Long>>(LinkedHashMap())
    val lines: StateFlow<Map<Long, Long>> = _lines.asStateFlow()

    fun quantityOf(itemId: Long): Long = _lines.value[itemId] ?: 0L

    fun set(itemId: Long, qtyMilli: Long) {
        _lines.update { current ->
            val next = LinkedHashMap(current)
            if (qtyMilli <= 0L) next.remove(itemId) else next[itemId] = qtyMilli
            next
        }
    }

    /**
     * Puts one more of an item on the bill, never more than there is in stock.
     * Returns a sentence to show the shop owner if it could not, or null when it worked.
     */
    fun addOne(item: ItemEntity): String? {
        if (item.archived) return "${item.name} is archived."
        if (item.stockMilli <= 0L) return "${item.name} is sold out."
        val have = quantityOf(item.id)
        val next = have + Qty.ONE
        if (next > item.stockMilli) {
            // Less than one whole unit left (say 0.5 kg): put what is left on the bill.
            if (have < item.stockMilli) {
                set(item.id, item.stockMilli)
                return null
            }
            return "Only ${Qty.withUnit(item.stockMilli, item.unit)} of ${item.name} in stock."
        }
        set(item.id, next)
        return null
    }

    fun remove(itemId: Long) = set(itemId, 0L)

    fun clear() {
        _lines.value = LinkedHashMap()
    }
}
