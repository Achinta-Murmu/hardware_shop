package com.galla.app.data.local.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.galla.app.core.model.StockLevel
import com.galla.app.core.util.Qty
import kotlinx.serialization.Serializable

/*
 * The tables of the shop's database. Everything here lives only on this phone.
 *
 * Two number rules run through all of them:
 *   - money is whole paise in a Long (see core/util/Money)
 *   - quantities are whole thousandths of a unit in a Long (see core/util/Qty)
 */

/** One thing the shop sells. */
@Serializable
@Entity(tableName = "items", indices = [Index(value = ["code"], unique = true)])
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** The short code printed on the sticker, e.g. "K7Q-3MX". Never changes. */
    val code: String,
    val name: String,
    /** Empty text means "no category". */
    val category: String,
    /** A key from core/model/Units, e.g. "pcs" or "kg". */
    val unit: String,
    /** What the shop pays for one unit now. */
    val costPaise: Long,
    /** What the shop sells one unit for. */
    val pricePaise: Long,
    /**
     * Stock on the shelf right now. This number is never typed over on its own: it only
     * changes together with a new row in `movements`, inside one database transaction.
     */
    val stockMilli: Long,
    /** At or below this, the item shows up as "low". */
    val minStockMilli: Long,
    /** Items with history are hidden (archived) instead of deleted, so old bills stay readable. */
    val archived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
) {
    val level: StockLevel
        get() = when {
            stockMilli <= 0L -> StockLevel.OUT
            stockMilli <= minStockMilli -> StockLevel.LOW
            else -> StockLevel.OK
        }

    /** What the stock on the shelf cost to buy. */
    val stockValuePaise: Long get() = Qty.times(costPaise, stockMilli)

    /** Profit on one unit as a share of the selling price, 0 to 100. */
    val marginPercent: Int
        get() = if (pricePaise <= 0L) 0 else (((pricePaise - costPaise) * 100) / pricePaise).toInt()
}

/**
 * The stock ledger. Every time an item's stock changes, one row is written here:
 * a purchase adds, a sale takes away, a correction can do either.
 */
@Serializable
@Entity(tableName = "movements", indices = [Index("itemId"), Index("at")])
data class MovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    /** One of core/model/MoveType. */
    val type: String,
    /** Plus for stock coming in, minus for stock going out. */
    val qtyMilli: Long,
    /** Stock left right after this change. */
    val balanceMilli: Long,
    /** The sale or purchase this belongs to, or 0. */
    val refId: Long = 0,
    val note: String = "",
    val at: Long,
)

/** One bill. */
@Serializable
@Entity(tableName = "sales", indices = [Index("at")])
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val at: Long,
    /** What the customer paid. */
    val totalPaise: Long,
    /** What the sold goods had cost the shop. Profit on the bill = total - cost. */
    val costPaise: Long,
    val lineCount: Int,
)

/**
 * One line of a bill. The item's name, unit, price and cost are copied in at the moment of
 * sale, so the bill and its profit stay correct even if the item is edited later.
 */
@Serializable
@Entity(tableName = "sale_lines", indices = [Index("saleId"), Index("itemId")])
data class SaleLineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    val itemId: Long,
    val name: String,
    val unit: String,
    val qtyMilli: Long,
    val pricePaise: Long,
    val costPaise: Long,
    val totalPaise: Long,
    val totalCostPaise: Long,
)

/** Stock bought from a supplier. */
@Serializable
@Entity(tableName = "purchases", indices = [Index("at"), Index("itemId")])
data class PurchaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    val itemName: String,
    val unit: String,
    val qtyMilli: Long,
    val unitCostPaise: Long,
    val totalPaise: Long,
    val supplier: String,
    val at: Long,
)

/** Money spent on anything that is not stock: rent, electricity, tea. */
@Serializable
@Entity(tableName = "expenses", indices = [Index("at")])
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val note: String,
    val amountPaise: Long,
    val at: Long,
)
