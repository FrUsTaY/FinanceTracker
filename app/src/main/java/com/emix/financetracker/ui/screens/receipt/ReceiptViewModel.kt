package com.emix.financetracker.ui.screens.receipt

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emix.financetracker.data.network.FnsApiService
import com.emix.financetracker.data.network.FnsReceipt
import com.emix.financetracker.data.network.FnsReceiptItem
import com.emix.financetracker.data.network.ReceiptItem
import com.emix.financetracker.data.network.TokenManager
import com.emix.financetracker.data.repository.BudgetRepository
import com.emix.financetracker.domain.usecase.AggregationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class ReceiptViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val aggregationUseCase: AggregationUseCase,
    private val fnsApiService: FnsApiService,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _items = MutableStateFlow<List<ReceiptUiItem>>(emptyList())
    val items: StateFlow<List<ReceiptUiItem>> = _items

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // Защита от дублирования API вызовов
    private var isProcessing = false

    // Индикатор подтверждения чека
    private val _isConfirming = MutableStateFlow(false)
    val isConfirming: StateFlow<Boolean> = _isConfirming

    // Ручной ввод: одна пустая строка
    fun initManual() {
        _items.value = listOf(ReceiptUiItem())
    }

    // Добавить новую пустую позицию
    fun addEmptyItem() {
        _items.value = _items.value + ReceiptUiItem()
    }

    // Удалить позицию по id
    fun removeItem(id: String) {
        _items.value = _items.value.filter { it.id != id }
        if (_items.value.isEmpty()) {
            _items.value = listOf(ReceiptUiItem())
        }
    }

    // Загрузка из QR через API
    fun loadFromQr(qrData: String) {
        // Защита от дублирования вызовов
        if (isProcessing) {
            _errorMessage.value = "Запрос уже обрабатывается"
            return
        }
        isProcessing = true
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val token = tokenManager.getToken() ?: ""
                val response = fnsApiService.checkReceipt(qrData, token)
                if (response.isSuccessful) {
                    val body = response.body()
                    val fnsReceipt = extractFnsReceipt(body)
                    if (fnsReceipt != null) {
                        // Если operationType не указан (null), считаем что это приход (1)
                        val opType = fnsReceipt.operationType
                        if (opType != null && opType != 1) {
                            _errorMessage.value = "Поддерживаются только чеки прихода (операция $opType)"
                            _items.value = listOf(ReceiptUiItem())
                            return@launch
                        }
                        val itemsList = fnsReceipt.items ?: emptyList()
                        val positiveItems = itemsList.filter { it.sum > 0 && it.price > 0 }
                        if (positiveItems.isEmpty()) {
                            _errorMessage.value = "Чек не содержит товаров для учёта"
                            _items.value = listOf(ReceiptUiItem())
                            return@launch
                        }
                        val uiItems = positiveItems.map { it.toUiItem() }
                        _items.value = uiItems
                    } else {
                        _errorMessage.value = "Не удалось распознать структуру чека"
                        _items.value = listOf(ReceiptUiItem())
                    }
                } else {
                    _errorMessage.value = "Ошибка сети: ${response.code()}"
                    _items.value = listOf(ReceiptUiItem())
                }
            } catch (e: Exception) {
                Log.e("ReceiptViewModel", "Network error", e)
                _errorMessage.value = "Нет соединения. Введите данные вручную."
                _items.value = listOf(ReceiptUiItem())
            } finally {
                _isLoading.value = false
                isProcessing = false
            }
        }
    }

    // Извлечение FnsReceipt из разных форматов (прямой ФНС или обёртка proverkacheka)
    private fun extractFnsReceipt(body: Any?): FnsReceipt? {
        if (body == null) return null
        return try {
            when (body) {
                is FnsReceipt -> body
                is com.emix.financetracker.data.network.ReceiptResponse -> {
                    body.data?.json?.let { json ->
                        FnsReceipt(
                            items = json.items,
                            operationType = null,
                            totalSum = null,
                            dateTime = null,
                            user = null
                        )
                    }
                }
                else -> null
            }
        } catch (e: Exception) {
            Log.e("ReceiptViewModel", "Error extracting FnsReceipt", e)
            null
        }
    }

    // Обновление одной позиции
    fun updateItem(id: String, updated: ReceiptUiItem) {
        _items.value = _items.value.map { if (it.id == id) validateItem(updated) else it }
    }

    // Валидация полей
    private fun validateItem(item: ReceiptUiItem): ReceiptUiItem {
        val nameError = if (item.name.isBlank()) "Название обязательно" else ""
        val quantityError = when {
            item.quantity.isBlank() -> "Введите количество"
            item.quantity.toDoubleOrNull() == null -> "Не число"
            item.quantity.toDouble() <= 0 -> "Количество > 0"
            else -> ""
        }
        val priceError = when {
            item.pricePerUnit.isBlank() -> "Введите цену"
            item.pricePerUnit.toDoubleOrNull() == null -> "Не число"
            item.pricePerUnit.toDouble() <= 0 -> "Цена > 0"
            else -> ""
        }
        return item.copy(
            nameError = nameError,
            quantityError = quantityError,
            priceError = priceError
        )
    }

    fun toggleCheck(id: String) {
        _items.value = _items.value.map {
            if (it.id == id) it.copy(isChecked = !it.isChecked) else it
        }
    }

    fun confirm(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isConfirming.value = true
            val checkedItems = _items.value.filter { it.isChecked }
            val hasErrors = checkedItems.any { !it.isValid() }
            if (hasErrors) {
                _errorMessage.value = "Заполните все поля корректно"
                _isConfirming.value = false
                return@launch
            }

            val budget = budgetRepository.getActiveBudgetOnce()
            if (budget == null) {
                _errorMessage.value = "Нет активного бюджета"
                _isConfirming.value = false
                return@launch
            }

            val receiptItems = checkedItems.mapNotNull { it.toReceiptItem() }
            if (receiptItems.isEmpty()) {
                _errorMessage.value = "Нет валидных позиций для добавления"
                _isConfirming.value = false
                return@launch
            }

            try {
                aggregationUseCase.processItems(budget.id, receiptItems)
                    .onSuccess { 
                        _isConfirming.value = false
                        onSuccess() 
                    }
                    .onFailure { 
                        _errorMessage.value = "Ошибка записи: ${it.message}"
                        _isConfirming.value = false 
                    }
            } catch (e: Exception) {
                Log.e("ReceiptViewModel", "Confirm error", e)
                _errorMessage.value = "Ошибка записи: ${e.message}"
                _isConfirming.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // ========== Конвертация единиц измерения ==========
    private fun mapUnitCodeToString(code: Int?, textUnit: String?): String {
        if (code == null) {
            return if (!textUnit.isNullOrBlank()) textUnit else "шт"
        }
        return when (code) {
            0 -> "шт"
            10 -> "г"
            11 -> "кг"
            12 -> "т"
            20 -> "см"
            21 -> "дм"
            22 -> "м"
            30 -> "кв. см"
            31 -> "кв. дм"
            32 -> "кв. м"
            40 -> "мл"
            41 -> "л"
            42 -> "куб. м"
            255 -> if (!textUnit.isNullOrBlank()) textUnit else "уп"
            else -> if (!textUnit.isNullOrBlank()) textUnit else "шт"
        }
    }

    private fun FnsReceiptItem.toUiItem(): ReceiptUiItem {
        // Все суммы в копейках
        val pricePerUnitKop = price
        val totalKop = sum
        val quantityVal = quantity

        val pricePerUnitRub = pricePerUnitKop / 100.0
        val totalRub = totalKop / 100.0

        val unit = mapUnitCodeToString(itemsQuantityMeasure, measurementUnit)

        return ReceiptUiItem(
            name = name,
            quantity = quantityVal.toString(),
            unit = unit,
            pricePerUnit = String.format("%.2f", pricePerUnitRub).replace(',', '.'),
            isChecked = true
        )
    }

    private fun ReceiptUiItem.toReceiptItem(): ReceiptItem? {
        val qty = quantity.toDoubleOrNull() ?: return null
        val price = pricePerUnit.toDoubleOrNull() ?: return null
        if (name.isBlank()) return null
        val total = qty * price
        val roundedTotal = (total * 100.0).roundToInt() / 100.0
        val normalizedUnit = unit.replace(".", "").trim()
        return ReceiptItem(
            name = name,
            quantity = qty,
            unit = normalizedUnit.ifEmpty { "шт" },
            pricePerUnit = price,
            totalAmount = roundedTotal
        )
    }
}