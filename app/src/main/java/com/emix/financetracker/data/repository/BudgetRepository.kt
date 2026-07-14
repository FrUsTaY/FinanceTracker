package com.emix.financetracker.data.repository

import com.emix.financetracker.data.db.dao.BudgetDao
import com.emix.financetracker.data.db.dao.ProductsMonthDao
import com.emix.financetracker.data.db.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepository @Inject constructor(
    private val budgetDao: BudgetDao,
    private val productsMonthDao: ProductsMonthDao
) {
    companion object {
        const val DATE_FORMAT_MONTH = "MM-yyyy"
        const val DATE_FORMAT_FULL = "yyyy-MM-dd"
    }
    fun getActiveBudget(): Flow<BudgetEntity?> = budgetDao.getActiveBudget()
    suspend fun getActiveBudgetOnce(): BudgetEntity? = budgetDao.getActiveBudgetOnce()

    suspend fun createBudget(monthYear: String, limit: Double, startDay: Int = 1): Long {
        return budgetDao.insert(BudgetEntity(monthYear = monthYear, budgetLimit = limit, isActive = 1, startDay = startDay))
    }

    suspend fun updateLimit(id: Int, limit: Double) = budgetDao.updateLimit(id, limit)
    suspend fun updateDefaultLimit(id: Int, defaultLimit: Double) = budgetDao.updateDefaultLimit(id, defaultLimit)
    suspend fun updateStartDay(id: Int, startDay: Int) = budgetDao.updateStartDay(id, startDay)
    suspend fun deactivateAll() = budgetDao.deactivateAll()

    suspend fun deactivateCurrentAndCreateNew(defaultLimit: Double, startDay: Int = 1): Long {
        deactivateAll()
        val currentMonthYear = SimpleDateFormat(DATE_FORMAT_MONTH, Locale.getDefault()).format(Date())
        val existing = budgetDao.getBudgetByMonthYear(currentMonthYear)
        return if (existing == null) {
            budgetDao.insert(BudgetEntity(monthYear = currentMonthYear, budgetLimit = defaultLimit, isActive = 1, defaultLimit = defaultLimit, startDay = startDay))
        } else {
            budgetDao.update(existing.copy(isActive = 1, budgetLimit = defaultLimit, defaultLimit = defaultLimit, startDay = startDay))
            existing.id.toLong()
        }
    }

    fun getAllBudgets(): Flow<List<BudgetEntity>> = budgetDao.getAllBudgets()
    fun getTotalSpent(budgetId: Int): Flow<Double?> = productsMonthDao.getTotalSpent(budgetId)
}