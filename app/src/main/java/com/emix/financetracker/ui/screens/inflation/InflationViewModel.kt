package com.emix.financetracker.ui.screens.inflation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emix.financetracker.data.db.dao.PurchaseHistoryDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class PricePoint(val date: String, val price: Double)

@HiltViewModel
class InflationViewModel @Inject constructor(
    private val purchaseHistoryDao: PurchaseHistoryDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Декодируем canonicalName и unit из аргументов навигации
    private val rawCanonicalName: String = savedStateHandle["canonicalName"] ?: ""
    private val rawUnit: String = savedStateHandle["unit"] ?: ""

    val canonicalName: String = try {
        java.net.URLDecoder.decode(rawCanonicalName, "UTF-8")
    } catch (e: Exception) {
        rawCanonicalName
    }

    val unit: String = try {
        java.net.URLDecoder.decode(rawUnit, "UTF-8")
    } catch (e: Exception) {
        rawUnit
    }.replace(".", "").trim() // удаляем точки из единицы (если есть)

    val priceHistory: StateFlow<List<PricePoint>> =
        if (canonicalName.isBlank() || unit.isBlank()) {
            MutableStateFlow(emptyList())
        } else {
            purchaseHistoryDao
                .getPriceHistory(canonicalName, unit)
                .catch { emit(emptyList()) }
                .map { list -> list.map { PricePoint(it.purchaseDate, it.pricePerUnit) } }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        }
}