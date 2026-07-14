package com.emix.financetracker.data.db.dao

import androidx.room.*
import com.emix.financetracker.data.db.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Insert
    suspend fun insert(budget: BudgetEntity): Long

    @Update
    suspend fun update(budget: BudgetEntity)

    @Query("SELECT * FROM Budgets WHERE isActive = 1 LIMIT 1")
    fun getActiveBudget(): Flow<BudgetEntity?>

    @Query("SELECT * FROM Budgets WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveBudgetOnce(): BudgetEntity?

    @Query("SELECT * FROM Budgets ORDER BY id DESC")
    fun getAllBudgets(): Flow<List<BudgetEntity>>

    @Query("UPDATE Budgets SET isActive = 0 WHERE isActive = 1")
    suspend fun deactivateAll()

    @Query("UPDATE Budgets SET budgetLimit = :limit WHERE id = :id")
    suspend fun updateLimit(id: Int, limit: Double)

    @Query("UPDATE Budgets SET defaultLimit = :defaultLimit WHERE id = :id")
    suspend fun updateDefaultLimit(id: Int, defaultLimit: Double)

    @Query("SELECT * FROM Budgets WHERE monthYear = :monthYear LIMIT 1")
    suspend fun getBudgetByMonthYear(monthYear: String): BudgetEntity?

    @Query("UPDATE Budgets SET startDay = :startDay WHERE id = :id")
    suspend fun updateStartDay(id: Int, startDay: Int)

    @Delete
    suspend fun delete(budget: BudgetEntity)
}