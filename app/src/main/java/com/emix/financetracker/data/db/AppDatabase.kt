package com.emix.financetracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.emix.financetracker.data.db.dao.BudgetDao
import com.emix.financetracker.data.db.dao.ProductsMonthDao
import com.emix.financetracker.data.db.dao.PurchaseHistoryDao
import com.emix.financetracker.data.db.entity.BudgetEntity
import com.emix.financetracker.data.db.entity.ProductsMonthEntity
import com.emix.financetracker.data.db.entity.PurchaseHistoryEntity

@Database(
    entities = [BudgetEntity::class, ProductsMonthEntity::class, PurchaseHistoryEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun budgetDao(): BudgetDao
    abstract fun productsMonthDao(): ProductsMonthDao
    abstract fun purchaseHistoryDao(): PurchaseHistoryDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE Budgets ADD COLUMN startDay INTEGER NOT NULL DEFAULT 1")
            }
        }
    }
}