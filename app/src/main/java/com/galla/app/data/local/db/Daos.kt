package com.galla.app.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/*
 * DAOs are the only code that talks SQL. Anything that returns a Flow is "live": it sends a
 * fresh answer every time the table changes, which is how every screen stays up to date.
 */

@Dao
interface ItemDao {
    @Query("SELECT * FROM items WHERE archived = 0 ORDER BY name COLLATE NOCASE")
    fun observeActive(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE archived = 1 ORDER BY name COLLATE NOCASE")
    fun observeArchived(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE id = :id")
    fun observe(id: Long): Flow<ItemEntity?>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun get(id: Long): ItemEntity?

    @Query("SELECT * FROM items WHERE code = :code")
    suspend fun byCode(code: String): ItemEntity?

    @Query("SELECT * FROM items")
    suspend fun all(): List<ItemEntity>

    @Query("SELECT COUNT(*) FROM items")
    suspend fun count(): Int

    @Insert
    suspend fun insert(item: ItemEntity): Long

    @Insert
    suspend fun insertAll(items: List<ItemEntity>)

    @Update
    suspend fun update(item: ItemEntity)

    @Query("DELETE FROM items")
    suspend fun clear()
}

@Dao
interface MovementDao {
    @Query("SELECT * FROM movements WHERE itemId = :itemId ORDER BY at DESC, id DESC LIMIT :limit")
    fun observeForItem(itemId: Long, limit: Int): Flow<List<MovementEntity>>

    @Query("SELECT COUNT(*) FROM movements WHERE itemId = :itemId")
    suspend fun countForItem(itemId: Long): Int

    @Query("SELECT * FROM movements")
    suspend fun all(): List<MovementEntity>

    @Insert
    suspend fun insert(movement: MovementEntity): Long

    @Insert
    suspend fun insertAll(movements: List<MovementEntity>)

    @Query("DELETE FROM movements")
    suspend fun clear()
}

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales WHERE at >= :start AND at < :end ORDER BY at DESC, id DESC")
    fun observeBetween(start: Long, end: Long): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales ORDER BY at DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE id = :id")
    fun observe(id: Long): Flow<SaleEntity?>

    @Query("SELECT * FROM sale_lines WHERE saleId = :saleId ORDER BY id")
    fun observeLines(saleId: Long): Flow<List<SaleLineEntity>>

    @Query("SELECT l.* FROM sale_lines l JOIN sales s ON s.id = l.saleId WHERE s.at >= :start AND s.at < :end")
    fun observeLinesBetween(start: Long, end: Long): Flow<List<SaleLineEntity>>

    @Query("SELECT * FROM sales")
    suspend fun allSales(): List<SaleEntity>

    @Query("SELECT * FROM sale_lines")
    suspend fun allLines(): List<SaleLineEntity>

    @Insert
    suspend fun insertSale(sale: SaleEntity): Long

    @Insert
    suspend fun insertLines(lines: List<SaleLineEntity>)

    @Insert
    suspend fun insertSales(sales: List<SaleEntity>)

    @Query("DELETE FROM sales")
    suspend fun clearSales()

    @Query("DELETE FROM sale_lines")
    suspend fun clearLines()
}

@Dao
interface PurchaseDao {
    @Query("SELECT * FROM purchases WHERE at >= :start AND at < :end ORDER BY at DESC, id DESC")
    fun observeBetween(start: Long, end: Long): Flow<List<PurchaseEntity>>

    @Query("SELECT * FROM purchases ORDER BY at DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<PurchaseEntity>>

    @Query("SELECT * FROM purchases")
    suspend fun all(): List<PurchaseEntity>

    @Insert
    suspend fun insert(purchase: PurchaseEntity): Long

    @Insert
    suspend fun insertAll(purchases: List<PurchaseEntity>)

    @Query("DELETE FROM purchases")
    suspend fun clear()
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE at >= :start AND at < :end ORDER BY at DESC, id DESC")
    fun observeBetween(start: Long, end: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses ORDER BY at DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses")
    suspend fun all(): List<ExpenseEntity>

    @Insert
    suspend fun insert(expense: ExpenseEntity): Long

    @Insert
    suspend fun insertAll(expenses: List<ExpenseEntity>)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM expenses")
    suspend fun clear()
}
