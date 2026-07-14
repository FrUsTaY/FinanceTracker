package com.emix.financetracker.ui.navigation

import androidx.lifecycle.ViewModel
import com.emix.financetracker.data.db.dao.BudgetDao
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AppNavViewModel @Inject constructor(private val budgetDao: BudgetDao) : ViewModel() {
    suspend fun hasActiveBudget(): Boolean = budgetDao.getActiveBudgetOnce() != null
}
