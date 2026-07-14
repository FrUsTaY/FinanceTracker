package com.emix.financetracker.domain.model

data class BudgetUiState(
    val budgetId: Int = 0,
    val periodName: String = "",
    val budgetLimit: Double = 0.0,
    val totalSpent: Double = 0.0,
    val isActive: Boolean = false
) {
    val remaining: Double get() = budgetLimit - totalSpent
    val progress: Float get() = if (budgetLimit > 0) (totalSpent / budgetLimit).toFloat() else 0f
}
