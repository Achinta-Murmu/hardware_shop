package com.galla.app.data.repo

import androidx.room.withTransaction
import com.galla.app.data.local.db.ExpenseEntity
import com.galla.app.data.local.db.GallaDatabase
import com.galla.app.data.local.db.ItemEntity
import com.galla.app.data.local.db.MovementEntity
import com.galla.app.data.local.db.PurchaseEntity
import com.galla.app.data.local.db.SaleEntity
import com.galla.app.data.local.db.SaleLineEntity
import com.galla.app.data.local.prefs.SettingsStore
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Everything the shop has, as one file. */
@Serializable
data class BackupFile(
    val app: String = "galla",
    /** The layout of this file, so a newer app can still read an older backup. */
    val format: Int = 1,
    val exportedAt: Long = 0,
    val shopName: String = "",
    val items: List<ItemEntity> = emptyList(),
    val movements: List<MovementEntity> = emptyList(),
    val sales: List<SaleEntity> = emptyList(),
    val saleLines: List<SaleLineEntity> = emptyList(),
    val purchases: List<PurchaseEntity> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList(),
)

data class BackupSummary(val items: Int, val sales: Int)

/**
 * Because the shop's data lives only on this phone, the owner needs a way to keep a copy.
 * This turns the whole database into one text file (JSON) and back. Where the file is saved
 * is the owner's choice (the phone's own file picker), so the app still needs no internet.
 */
class BackupRepository(
    private val db: GallaDatabase,
    private val settings: SettingsStore,
    private val now: () -> Long,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun export(): String {
        val file = db.withTransaction {
            BackupFile(
                exportedAt = now(),
                shopName = settings.current().shopName,
                items = db.itemDao().all(),
                movements = db.movementDao().all(),
                sales = db.saleDao().allSales(),
                saleLines = db.saleDao().allLines(),
                purchases = db.purchaseDao().all(),
                expenses = db.expenseDao().all(),
            )
        }
        return json.encodeToString(BackupFile.serializer(), file)
    }

    /** Replaces everything in the app with the contents of a backup file. */
    suspend fun import(text: String): BackupSummary {
        val file = try {
            json.decodeFromString(BackupFile.serializer(), text)
        } catch (e: Exception) {
            throw ShopException("This file is not a Galla backup.")
        }
        if (file.app != "galla") throw ShopException("This file is not a Galla backup.")
        if (file.format > 1) throw ShopException("This backup was made by a newer version of Galla. Update the app first.")
        db.withTransaction {
            clearAll()
            db.itemDao().insertAll(file.items)
            db.movementDao().insertAll(file.movements)
            db.saleDao().insertSales(file.sales)
            db.saleDao().insertLines(file.saleLines)
            db.purchaseDao().insertAll(file.purchases)
            db.expenseDao().insertAll(file.expenses)
        }
        if (file.shopName.isNotBlank()) settings.update { it.copy(shopName = file.shopName) }
        return BackupSummary(items = file.items.size, sales = file.sales.size)
    }

    /** Wipes the shop. Used by "Erase everything" in Settings. */
    suspend fun eraseEverything() = db.withTransaction { clearAll() }

    private suspend fun clearAll() {
        db.saleDao().clearLines()
        db.saleDao().clearSales()
        db.purchaseDao().clear()
        db.expenseDao().clear()
        db.movementDao().clear()
        db.itemDao().clear()
    }
}
