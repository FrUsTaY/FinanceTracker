package com.emix.financetracker.ui.screens.settings

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emix.financetracker.BuildConfig
import com.emix.financetracker.data.db.AppDatabase
import com.emix.financetracker.data.db.dao.PurchaseHistoryDao
import com.emix.financetracker.data.db.entity.BudgetEntity
import com.emix.financetracker.data.db.entity.ProductsMonthEntity
import com.emix.financetracker.data.db.entity.PurchaseHistoryEntity
import com.emix.financetracker.data.network.TokenManager
import com.emix.financetracker.data.repository.BudgetRepository
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val purchaseHistoryDao: PurchaseHistoryDao,
    private val productsMonthDao: com.emix.financetracker.data.db.dao.ProductsMonthDao,
    private val tokenManager: TokenManager,
    private val appDatabase: AppDatabase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val activeBudget: StateFlow<BudgetEntity?> = budgetRepository.getActiveBudget()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allBudgets: StateFlow<List<BudgetEntity>> = budgetRepository.getAllBudgets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _operationStatus = MutableStateFlow<String?>(null)
    val operationStatus: StateFlow<String?> = _operationStatus.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val startDay: StateFlow<Int> = activeBudget.map { it?.startDay ?: 1 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    fun updateCurrentLimit(newLimit: Double) {
        viewModelScope.launch {
            activeBudget.value?.let { budget ->
                budgetRepository.updateLimit(budget.id, newLimit)
                _operationStatus.value = "Лимит обновлён"
            }
        }
    }

    fun updateDefaultLimit(newDefault: Double) {
        viewModelScope.launch {
            activeBudget.value?.let { budget ->
                budgetRepository.updateDefaultLimit(budget.id, newDefault)
                _operationStatus.value = "Лимит по умолчанию обновлён"
            }
        }
    }

    fun updateStartDay(newStartDay: Int) {
        viewModelScope.launch {
            activeBudget.value?.let { budget ->
                budgetRepository.updateStartDay(budget.id, newStartDay)
                _operationStatus.value = "День начала периода обновлён"
            }
        }
    }

    fun startNewMonth() {
        viewModelScope.launch {
            val currentBudget = activeBudget.value
            val defaultLimit = currentBudget?.defaultLimit ?: currentBudget?.budgetLimit ?: 0.0
            val startDay = currentBudget?.startDay ?: 1
            budgetRepository.deactivateCurrentAndCreateNew(defaultLimit, startDay)
            _operationStatus.value = "Новый месяц начат"
        }
    }

    fun saveApiToken(token: String) {
        tokenManager.saveToken(token)
        _operationStatus.value = "Токен сохранён"
    }

    fun getApiToken(): String = tokenManager.getToken() ?: ""

    // ==================== JSON EXPORT / IMPORT ====================

    data class ExportData(
        val version: Int = 1,
        val budgets: List<BudgetEntity>,
        val productsMonth: List<ProductsMonthEntity>,
        val purchaseHistory: List<PurchaseHistoryEntity>
    )

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    fun exportDatabaseToJson() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val budgets = getAllBudgetsInternal()
                val products = getAllProductsInternal()
                val history = getAllPurchaseHistoryInternal()
                val exportData = ExportData(
                    budgets = budgets,
                    productsMonth = products,
                    purchaseHistory = history
                )
                val json = gson.toJson(exportData)
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "finance_tracker_backup_$timestamp.json"
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val destFile = File(downloadsDir, fileName)
                destFile.writeText(json)
                _operationStatus.value = "Экспорт JSON завершён: $fileName (папка Download)"
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Export JSON error", e)
                _operationStatus.value = "Ошибка экспорта: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun importDatabaseFromJson(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: throw Exception("Не удалось открыть файл")
                val reader = InputStreamReader(inputStream)
                val exportData = gson.fromJson<ExportData>(reader, ExportData::class.java)
                    ?: run {
                        _operationStatus.value = "Ошибка: пустые данные в файле"
                        return@launch
                    }
                reader.close()
                inputStream.close()

                if (exportData.version != 1) {
                    _operationStatus.value = "Неизвестная версия формата: ${exportData.version}"
                    return@launch
                }

                // Валидация обязательных полей
                // (поля не nullable, поэтому null-проверка не нужна)

                // Очищаем все таблицы
                purchaseHistoryDao.deleteAll()
                productsMonthDao.deleteAll()
                // Удаляем все бюджеты (нельзя просто удалить – нарушится связь; придётся удалять через Dao)
                val budgetsToRemove = allBudgets.value.toList()
                budgetsToRemove.forEach { budget ->
                    appDatabase.budgetDao().delete(budget)
                }

                // Вставляем бюджеты
                exportData.budgets.forEach { budget ->
                    appDatabase.budgetDao().insert(budget)
                }
                // Вставляем товары
                exportData.productsMonth.forEach { product ->
                    appDatabase.productsMonthDao().insert(product)
                }
                // Вставляем историю
                exportData.purchaseHistory.forEach { history ->
                    appDatabase.purchaseHistoryDao().insert(history)
                }

                _operationStatus.value = "Импорт JSON завершён. Перезапустите приложение."
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Import JSON error", e)
                _operationStatus.value = "Ошибка импорта: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Вспомогательные методы для получения всех данных
    private suspend fun getAllBudgetsInternal(): List<BudgetEntity> {
        return appDatabase.budgetDao().getAllBudgets().first()
    }

    private suspend fun getAllProductsInternal(): List<ProductsMonthEntity> {
        // Нет прямого метода для всех продуктов – сделаем запрос
        return appDatabase.productsMonthDao().getAllProducts()
    }

    private suspend fun getAllPurchaseHistoryInternal(): List<PurchaseHistoryEntity> {
        return appDatabase.purchaseHistoryDao().getAllHistory()
    }

    fun clearAllData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                purchaseHistoryDao.deleteAll()
                productsMonthDao.deleteAll()
                // Удаляем все бюджеты, кроме активного? Лучше удалить все
                val budgetsToRemove = allBudgets.value.toList()
                budgetsToRemove.forEach { budget ->
                    appDatabase.budgetDao().delete(budget)
                }
                _operationStatus.value = "Все данные очищены"
            } catch (e: Exception) {
                _operationStatus.value = "Ошибка очистки: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearOperationStatus() {
        _operationStatus.value = null
    }
}