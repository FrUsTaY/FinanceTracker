package com.emix.financetracker.ui.screens.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emix.financetracker.data.db.dao.ProductsMonthDao
import com.emix.financetracker.data.db.entity.ProductsMonthEntity
import com.emix.financetracker.data.repository.BudgetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val productsMonthDao: ProductsMonthDao
) : ViewModel() {

    private val _budgetState = MutableStateFlow<BudgetUiState?>(null)
    val budgetState: StateFlow<BudgetUiState?> = _budgetState.asStateFlow()

    private val _products = MutableStateFlow<List<ProductsMonthEntity>>(emptyList())
    val products: StateFlow<List<ProductsMonthEntity>> = _products.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.DATE_DESC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    val sortedProducts: StateFlow<List<ProductsMonthEntity>> = combine(
        products,
        sortOrder
    ) { list, order ->
        when (order) {
            SortOrder.NAME_ASC -> list.sortedBy { it.productName.lowercase() }
            SortOrder.NAME_DESC -> list.sortedByDescending { it.productName.lowercase() }
            SortOrder.AMOUNT_ASC -> list.sortedBy { it.totalAmount }
            SortOrder.AMOUNT_DESC -> list.sortedByDescending { it.totalAmount }
            SortOrder.QTY_ASC -> list.sortedBy { it.totalQuantity }
            SortOrder.QTY_DESC -> list.sortedByDescending { it.totalQuantity }
            SortOrder.DATE_ASC -> list.sortedBy { it.id }
            SortOrder.DATE_DESC -> list.sortedByDescending { it.id }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Склейка: id товара-донора
    private val _mergeSourceId = MutableStateFlow<Int?>(null)
    val mergeSourceId: StateFlow<Int?> = _mergeSourceId.asStateFlow()

    // Отмена корутин при смене бюджета
    private var observeBudgetJob: Job? = null

    init {
        viewModelScope.launch {
            val initialBudget = budgetRepository.getActiveBudgetOnce()
            Log.d("MainViewModel", "Initial budget: $initialBudget")
            if (initialBudget != null) {
                observeBudgetState(initialBudget.id)
            }
            budgetRepository.getActiveBudget().collect { budget ->
                Log.d("MainViewModel", "Budget flow update: $budget")
                if (budget != null) {
                    observeBudgetState(budget.id)
                } else {
                    _budgetState.value = null
                }
            }
        }
    }

    private fun observeBudgetState(budgetId: Int) {
        // Отменить предыдущие корутины перед запуском новых
        observeBudgetJob?.cancel()
        observeBudgetJob = viewModelScope.launch {
            productsMonthDao.getActiveProducts(budgetId).collect { productList ->
                _products.value = productList
            }
        }
        observeBudgetJob = viewModelScope.launch {
            productsMonthDao.getTotalSpent(budgetId).collect { total ->
                val budget = budgetRepository.getActiveBudgetOnce()
                if (budget != null && budget.id == budgetId) {
                    _budgetState.value = BudgetUiState(
                        budgetId = budget.id,
                        periodName = budget.monthYear,
                        budgetLimit = budget.budgetLimit,
                        totalSpent = total ?: 0.0,
                        isActive = budget.isActive == 1,
                        startDay = budget.startDay
                    )
                    Log.d("MainViewModel", "Budget state updated: totalSpent = ${total ?: 0.0}")
                }
            }
        }
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun deleteProduct(product: ProductsMonthEntity) {
        viewModelScope.launch {
            productsMonthDao.delete(product)
        }
    }

    // Склейка
    fun startMerge(productId: Int) {
        _mergeSourceId.value = productId
    }

    fun cancelMerge() {
        _mergeSourceId.value = null
    }

    fun mergeProducts(donorId: Int, targetId: Int) {
        viewModelScope.launch {
            val donor = productsMonthDao.getById(donorId)
            val target = productsMonthDao.getById(targetId)
            if (donor != null && target != null) {
                // Суммируем количество и сумму донора к цели
                productsMonthDao.addToTotals(targetId, donor.totalQuantity, donor.totalAmount)
                // Помечаем донора как объединённый, меняем canonicalName на целевой
                productsMonthDao.mergeDonorIntoTarget(donorId, targetId, target.canonicalName)
                // Сбрасываем режим склейки
                _mergeSourceId.value = null
            }
        }
    }
}

data class BudgetUiState(
    val budgetId: Int,
    val periodName: String,
    val budgetLimit: Double,
    val totalSpent: Double,
    val isActive: Boolean,
    val startDay: Int = 1
) {
    val remaining: Double get() = budgetLimit - totalSpent
    val progress: Float get() = if (budgetLimit > 0) (totalSpent / budgetLimit).toFloat() else 0f
}

enum class SortOrder {
    NAME_ASC, NAME_DESC, AMOUNT_ASC, AMOUNT_DESC, QTY_ASC, QTY_DESC, DATE_ASC, DATE_DESC
}