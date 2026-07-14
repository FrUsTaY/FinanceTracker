package com.emix.financetracker.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emix.financetracker.data.repository.BudgetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository
) : ViewModel() {
    // Автоподстановка текущего MM-YYYY
    val defaultPeriodName: String = run {
        val cal = java.util.Calendar.getInstance()
        val month = String.format("%02d", cal.get(java.util.Calendar.MONTH) + 1)
        val year = cal.get(java.util.Calendar.YEAR)
        "$month-$year"
    }

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _success = MutableStateFlow(false)
    val success: StateFlow<Boolean> = _success

    suspend fun createBudget(monthYear: String, limit: Double): Result<Unit> {
        return try {
            viewModelScope.launch {
                budgetRepository.createBudget(monthYear, limit)
            }.join()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun setSuccess() {
        _success.value = true
    }

    fun setErrorMessage(message: String) {
        _errorMessage.value = message
    }

    fun reset() {
        _errorMessage.value = null
        _success.value = false
    }
}
