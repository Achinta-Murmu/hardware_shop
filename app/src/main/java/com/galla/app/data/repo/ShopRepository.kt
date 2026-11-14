package com.galla.app.data.repo

import androidx.room.withTransaction
import com.galla.app.core.model.MoveType
import com.galla.app.core.util.Codes
import com.galla.app.core.util.Qty
import com.galla.app.core.util.TimeRange
import com.galla.app.data.local.db.ExpenseEntity
import com.galla.app.data.local.db.GallaDatabase
import com.galla.app.data.local.db.ItemEntity
import com.galla.app.data.local.db.MovementEntity
import com.galla.app.data.local.db.PurchaseEntity
import com.galla.app.data.local.db.SaleEntity
import com.galla.app.data.local.db.SaleLineEntity
import kotlinx.coroutines.flow.Flow

/** Something the shop owner should be told in plain words ("Only 3 pcs left of Brass Tap"). */
class ShopException(message: String) : Exception(message)

/** The fields of the add/edit item form. */
data class ItemDraft(
    /** 0 means "a new item". */
    val id: Long = 0,
    val name: String,
    val category: String,
    val unit: String,
    val costPaise: Long,
    val pricePaise: Long,
    val minStockMilli: Long,
    /** Only used for a new item. Later changes to stock go through a purchase or a correction. */
    val openingStockMilli: Long = 0,
)

/**
 * The shop's rules live here. Screens never touch the database themselves: they ask this
 * class, and it makes sure every action either fully happens or does not happen at all
 * (that is what `withTransaction` does).
 *
 * The one rule that matters most: an item's stock never changes without a matching row in
 * the stock ledger (`movements`), written in the same transaction.
 */
class ShopRepository(private val db: GallaDatabase, private val now: () -> Long) {

    private val itemDao = db.itemDao()
    private val movementDao = db.movementDao()
    private val saleDao = db.saleDao()
    private val purchaseDao = db.purchaseDao()
    private val expenseDao = db.expenseDao()

    // ---- items ---------------------------------------------------------------------------

    val items: Flow<List<ItemEntity>> = itemDao.observeActive()
    val archivedItems: Flow<List<ItemEntity>> = itemDao.observeArchived()

    fun item(id: Long): Flow<ItemEntity?> = itemDao.observe(id)

    suspend fun getItem(id: Long): ItemEntity? = itemDao.get(id)

    suspend fun findByCode(code: String): ItemEntity? = itemDao.byCode(code)

    suspend fun itemCount(): Int = itemDao.count()

    fun movements(itemId: Long, limit: Int = 60): Flow<List<MovementEntity>> = movementDao.observeForItem(itemId, limit)

    /** Adds a new item (with its opening stock) or saves changes to an existing one. Returns the item id. */
    suspend fun saveItem(draft: ItemDraft, at: Long = now()): Long = db.withTransaction {
        val name = draft.name.trim()
        if (name.isEmpty()) throw ShopException("Give the item a name.")
        if (draft.id == 0L) {
            val opening = draft.openingStockMilli.coerceAtLeast(0L)
            val id = itemDao.insert(
                ItemEntity(
                    code = newUniqueCode(),
                    name = name,
                    category = draft.category.trim(),
                    unit = draft.unit,
                    costPaise = draft.costPaise,
                    pricePaise = draft.pricePaise,
                    stockMilli = opening,
                    minStockMilli = draft.minStockMilli.coerceAtLeast(0L),
                    createdAt = at,
                    updatedAt = at,
                ),
            )
            if (opening > 0L) {
                movementDao.insert(
                    MovementEntity(itemId = id, type = MoveType.OPENING, qtyMilli = opening, balanceMilli = opening, note = "Opening stock", at = at),
                )
            }
            id
        } else {
            val old = itemDao.get(draft.id) ?: throw ShopException("This item no longer exists.")
            itemDao.update(
                old.copy(
                    name = name,
                    category = draft.category.trim(),
                    unit = draft.unit,
                    costPaise = draft.costPaise,
                    pricePaise = draft.pricePaise,
                    minStockMilli = draft.minStockMilli.coerceAtLeast(0L),
                    updatedAt = at,
                ),
            )
            old.id
        }
    }

    private suspend fun newUniqueCode(): String {
        repeat(30) {
            val code = Codes.newCode()
            if (itemDao.byCode(code) == null) return code
        }
        throw ShopException("Could not create a code for this item. Please try again.")
    }

    /** Hides an item from the shop (or brings it back). Its history is kept. */
    suspend fun setArchived(itemId: Long, archived: Boolean) = db.withTransaction {
        val item = itemDao.get(itemId) ?: return@withTransaction
        itemDao.update(item.copy(archived = archived, updatedAt = now()))
    }

    /** "I counted the shelf and there are really `countedMilli`." Writes the difference to the ledger. */
    suspend fun correctStock(itemId: Long, countedMilli: Long, reason: String, at: Long = now()) = db.withTransaction {
        val item = itemDao.get(itemId) ?: throw ShopException("This item no longer exists.")
        if (countedMilli < 0L) throw ShopException("Stock cannot be less than zero.")
        val difference = countedMilli - item.stockMilli
        if (difference == 0L) return@withTransaction
        itemDao.update(item.copy(stockMilli = countedMilli, updatedAt = at))
        movementDao.insert(
            MovementEntity(itemId = itemId, type = MoveType.ADJUST, qtyMilli = difference, balanceMilli = countedMilli, note = reason, at = at),
        )
    }

    // ---- stock in (purchases) --------------------------------------------------------------

    /** Stock bought from a supplier: stock goes up, and the item's buying price is updated. */
    suspend fun stockIn(itemId: Long, qtyMilli: Long, unitCostPaise: Long, supplier: String, at: Long = now()): Long = db.withTransaction {
        val item = itemDao.get(itemId) ?: throw ShopException("This item no longer exists.")
        if (qtyMilli <= 0L) throw ShopException("Enter how much came in.")
        val balance = item.stockMilli + qtyMilli
        itemDao.update(item.copy(stockMilli = balance, costPaise = unitCostPaise, updatedAt = at))
        val purchaseId = purchaseDao.insert(
            PurchaseEntity(
                itemId = itemId,
                itemName = item.name,
                unit = item.unit,
                qtyMilli = qtyMilli,
                unitCostPaise = unitCostPaise,
                totalPaise = Qty.times(unitCostPaise, qtyMilli),
                supplier = supplier.trim(),
                at = at,
            ),
        )
        movementDao.insert(
            MovementEntity(itemId = itemId, type = MoveType.PURCHASE, qtyMilli = qtyMilli, balanceMilli = balance, refId = purchaseId, note = supplier.trim(), at = at),
        )
        purchaseId
    }

    // ---- sales -----------------------------------------------------------------------------

    /**
     * Turns the bill at the counter into a saved sale. `quantities` is item id to quantity.
     * Everything is checked first; only if every line is fine is anything written.
     * Returns the new bill's id.
     */
    suspend fun completeSale(quantities: Map<Long, Long>, at: Long = now()): Long = db.withTransaction {
        if (quantities.isEmpty()) throw ShopException("The bill is empty.")
        val picked = quantities.map { (itemId, qty) ->
            val item = itemDao.get(itemId) ?: throw ShopException("An item on this bill no longer exists.")
            if (item.archived) throw ShopException("${item.name} is archived and cannot be sold.")
            if (qty <= 0L) throw ShopException("Check the quantity of ${item.name}.")
            if (qty > item.stockMilli) {
                throw ShopException("Only ${Qty.withUnit(item.stockMilli, item.unit)} of ${item.name} in stock.")
            }
            item to qty
        }
        val lines = picked.map { (item, qty) ->
            SaleLineEntity(
                saleId = 0,
                itemId = item.id,
                name = item.name,
                unit = item.unit,
                qtyMilli = qty,
                pricePaise = item.pricePaise,
                costPaise = item.costPaise,
                totalPaise = Qty.times(item.pricePaise, qty),
                totalCostPaise = Qty.times(item.costPaise, qty),
            )
        }
        val saleId = saleDao.insertSale(
            SaleEntity(at = at, totalPaise = lines.sumOf { it.totalPaise }, costPaise = lines.sumOf { it.totalCostPaise }, lineCount = lines.size),
        )
        saleDao.insertLines(lines.map { it.copy(saleId = saleId) })
        picked.forEach { (item, qty) ->
            val balance = item.stockMilli - qty
            itemDao.update(item.copy(stockMilli = balance, updatedAt = at))
            movementDao.insert(
                MovementEntity(itemId = item.id, type = MoveType.SALE, qtyMilli = -qty, balanceMilli = balance, refId = saleId, at = at),
            )
        }
        saleId
    }

    fun sale(id: Long): Flow<SaleEntity?> = saleDao.observe(id)
    fun saleLines(saleId: Long): Flow<List<SaleLineEntity>> = saleDao.observeLines(saleId)
    fun sales(range: TimeRange): Flow<List<SaleEntity>> = saleDao.observeBetween(range.start, range.end)
    fun saleLines(range: TimeRange): Flow<List<SaleLineEntity>> = saleDao.observeLinesBetween(range.start, range.end)
    fun recentSales(limit: Int): Flow<List<SaleEntity>> = saleDao.observeRecent(limit)

    // ---- purchases and expenses, for the money screens ----------------------------------------

    fun purchases(range: TimeRange): Flow<List<PurchaseEntity>> = purchaseDao.observeBetween(range.start, range.end)
    fun recentPurchases(limit: Int): Flow<List<PurchaseEntity>> = purchaseDao.observeRecent(limit)

    fun expenses(range: TimeRange): Flow<List<ExpenseEntity>> = expenseDao.observeBetween(range.start, range.end)
    fun recentExpenses(limit: Int): Flow<List<ExpenseEntity>> = expenseDao.observeRecent(limit)

    suspend fun addExpense(category: String, note: String, amountPaise: Long, at: Long = now()): Long {
        if (amountPaise <= 0L) throw ShopException("Enter the amount spent.")
        return expenseDao.insert(ExpenseEntity(category = category, note = note.trim(), amountPaise = amountPaise, at = at))
    }

    suspend fun deleteExpense(id: Long) = expenseDao.delete(id)
}
