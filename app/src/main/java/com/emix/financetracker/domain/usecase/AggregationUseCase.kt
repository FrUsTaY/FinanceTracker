package com.emix.financetracker.domain.usecase

import com.emix.financetracker.data.db.dao.ProductsMonthDao
import com.emix.financetracker.data.db.dao.PurchaseHistoryDao
import com.emix.financetracker.data.db.entity.ProductsMonthEntity
import com.emix.financetracker.data.db.entity.PurchaseHistoryEntity
import com.emix.financetracker.data.network.ReceiptItem
import java.time.LocalDate
import javax.inject.Inject

class AggregationUseCase @Inject constructor(
    private val productsMonthDao: ProductsMonthDao,
    private val purchaseHistoryDao: PurchaseHistoryDao
) {

    suspend fun processItems(budgetId: Int, items: List<ReceiptItem>): Result<Unit> {
        return try {
            val today = LocalDate.now().toString()
            for (item in items) {
                val canonical = item.name.trim().lowercase()
                val normalizedUnit = item.unit.replace(".", "").trim().ifEmpty { "шт" }
                val existing = productsMonthDao.findByCanonicalAndUnit(budgetId, canonical, normalizedUnit)

                if (existing != null) {
                    productsMonthDao.addToTotals(existing.id, item.quantity, item.totalAmount)
                } else {
                    productsMonthDao.insert(
                        ProductsMonthEntity(
                            budgetId = budgetId,
                            productName = item.name,
                            canonicalName = canonical,
                            unit = normalizedUnit,
                            totalQuantity = item.quantity,
                            totalAmount = item.totalAmount
                        )
                    )
                }

                purchaseHistoryDao.insert(
                    PurchaseHistoryEntity(
                        budgetId = budgetId,
                        productName = item.name,
                        canonicalName = canonical,
                        unit = normalizedUnit,
                        purchaseDate = today,
                        pricePerUnit = item.pricePerUnit
                    )
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}