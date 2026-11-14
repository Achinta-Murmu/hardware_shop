package com.galla.app.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The shop's database (SQLite, through Room). It is the single source of truth: every screen
 * reads from here and nothing is kept anywhere else. There is no server copy.
 */
@Database(
    entities = [
        ItemEntity::class,
        MovementEntity::class,
        SaleEntity::class,
        SaleLineEntity::class,
        PurchaseEntity::class,
        ExpenseEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class GallaDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun movementDao(): MovementDao
    abstract fun saleDao(): SaleDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun expenseDao(): ExpenseDao

    companion object {
        fun create(context: Context): GallaDatabase =
            Room.databaseBuilder(context.applicationContext, GallaDatabase::class.java, "galla.db")
                // PREVIEW ONLY: while the table layout can still change, an old layout is wiped
                // instead of crashing. Before a real shop relies on the app this line must be
                // replaced by proper migrations, because it deletes data. See AGENTS.md section 7.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
