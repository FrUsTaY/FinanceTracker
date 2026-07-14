package com.emix.financetracker.data.db.dao

import androidx.room.*
import com.emix.financetracker.data.db.entity.ProductsMonthEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductsMonthDao {
    @Insert
    suspend fun insert(product: ProductsMonthEntity): Long

    @Update
    suspend fun update(product: ProductsMonthEntity)

    @Delete
    suspend fun delete(product: ProductsMonthEntity)

    @Query("SELECT * FROM ProductsMonth WHERE budgetId = :budgetId AND mergedIntoId IS NULL ORDER BY id DESC")
    fun getActiveProducts(budgetId: Int): Flow<List<ProductsMonthEntity>>

    @Query("SELECT * FROM ProductsMonth WHERE budgetId = :budgetId AND mergedIntoId IS NULL ORDER BY id DESC")
    suspend fun getActiveProductsOnce(budgetId: Int): List<ProductsMonthEntity>

    @Query("SELECT * FROM ProductsMonth WHERE budgetId = :budgetId AND canonicalName = :canonicalName AND unit = :unit AND mergedIntoId IS NULL LIMIT 1")
    suspend fun findByCanonicalAndUnit(budgetId: Int, canonicalName: String, unit: String): ProductsMonthEntity?

    @Query("UPDATE ProductsMonth SET totalQuantity = totalQuantity + :qty, totalAmount = totalAmount + :amount WHERE id = :id")
    suspend fun addToTotals(id: Int, qty: Double, amount: Double)

    @Query("UPDATE ProductsMonth SET mergedIntoId = :targetId, canonicalName = :targetCanonical WHERE id = :donorId")
    suspend fun mergeDonorIntoTarget(donorId: Int, targetId: Int, targetCanonical: String)

    @Query("SELECT SUM(totalAmount) FROM ProductsMonth WHERE budgetId = :budgetId AND mergedIntoId IS NULL")
    fun getTotalSpent(budgetId: Int): Flow<Double?>

    @Query("SELECT * FROM ProductsMonth WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): ProductsMonthEntity?

    @Query("DELETE FROM ProductsMonth")
    suspend fun deleteAll()

    @Query("SELECT * FROM ProductsMonth")
    suspend fun getAllProducts(): List<ProductsMonthEntity>
}