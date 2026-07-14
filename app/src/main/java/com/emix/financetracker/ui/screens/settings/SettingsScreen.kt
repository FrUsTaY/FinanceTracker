package com.emix.financetracker.ui.screens.settings

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emix.financetracker.BuildConfig
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val activeBudget by viewModel.activeBudget.collectAsStateWithLifecycle()
    val allBudgets by viewModel.allBudgets.collectAsStateWithLifecycle()
    val operationStatus by viewModel.operationStatus.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val startDay by viewModel.startDay.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var currentLimit by remember(activeBudget) { mutableStateOf(activeBudget?.budgetLimit?.toString() ?: "") }
    var defaultLimit by remember(activeBudget) { mutableStateOf(activeBudget?.defaultLimit?.toString() ?: "") }
    var apiToken by remember { mutableStateOf(viewModel.getApiToken()) }
    var showToken by remember { mutableStateOf(false) }
    var showArchiveDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showNewMonthDialog by remember { mutableStateOf(false) }
    var selectedArchiveBudget by remember { mutableStateOf<com.emix.financetracker.data.db.entity.BudgetEntity?>(null) }
    var showArchiveDetailDialog by remember { mutableStateOf(false) }
    var currentStartDay by remember(startDay) { mutableStateOf(startDay) }
    var selectedImportUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { selectedImportUri = it }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Секция: Бюджет и лимиты
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Бюджет и лимиты", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = currentLimit,
                                onValueChange = { currentLimit = it },
                                label = { Text("Лимит текущего месяца, ₽") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                            Button(
                                onClick = {
                                    val limit = currentLimit.toDoubleOrNull()
                                    if (limit != null && limit > 0) {
                                        viewModel.updateCurrentLimit(limit)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Сохранить лимит")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = defaultLimit,
                                onValueChange = { defaultLimit = it },
                                label = { Text("Лимит по умолчанию, ₽") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                            Button(
                                onClick = {
                                    val limit = defaultLimit.toDoubleOrNull()
                                    if (limit != null && limit > 0) {
                                        viewModel.updateDefaultLimit(limit)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Сохранить дефолтный лимит")
                            }
                        }
                    }
                }

                // Секция: Настройка дня начала периода
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("День начала периода", style = MaterialTheme.typography.titleMedium)
                            Text("С этого числа начинается новый отчётный период (например, зарплата)", style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("День:", style = MaterialTheme.typography.bodyLarge)
                                Slider(
                                    value = currentStartDay.toFloat(),
                                    onValueChange = { currentStartDay = it.toInt() },
                                    valueRange = 1f..31f,
                                    steps = 30,
                                    modifier = Modifier.weight(1f)
                                )
                                Text("$currentStartDay", style = MaterialTheme.typography.bodyLarge)
                            }
                            Button(
                                onClick = {
                                    if (currentStartDay in 1..31) {
                                        viewModel.updateStartDay(currentStartDay)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Сохранить день начала")
                            }
                        }
                    }
                }

                // Секция: Управление периодом
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Управление периодом", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { showNewMonthDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Начать новый месяц")
                            }
                            Button(
                                onClick = { showArchiveDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Архив месяцев")
                            }
                        }
                    }
                }

                // Секция: Данные и резервное копирование (JSON)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Данные и резервное копирование", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.exportDatabaseToJson() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Экспорт данных (JSON)")
                            }
                            Button(
                                onClick = { importJsonLauncher.launch("application/json") },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Импорт данных (JSON)")
                            }

                            // Диалог подтверждения импорта
                            if (selectedImportUri != null) {
                                AlertDialog(
                                    onDismissRequest = { selectedImportUri = null },
                                    title = { Text("Импорт данных") },
                                    text = { Text("Все текущие данные будут удалены. Продолжить?") },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                val uri = selectedImportUri!!
                                                selectedImportUri = null
                                                scope.launch {
                                                    viewModel.importDatabaseFromJson(uri)
                                                }
                                            }
                                        ) {
                                            Text("Да", color = MaterialTheme.colorScheme.primary)
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { selectedImportUri = null }) {
                                            Text("Отмена")
                                        }
                                    }
                                )
                            }
                            Button(
                                onClick = { showClearDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Очистить всю историю")
                            }
                        }
                    }
                }

                // Секция: Интеграция
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Интеграция", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = apiToken,
                                onValueChange = { apiToken = it },
                                label = { Text("API-токен ФНС") },
                                visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row {
                                TextButton(onClick = { showToken = !showToken }) {
                                    Text(if (showToken) "Скрыть" else "Показать")
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Button(onClick = { viewModel.saveApiToken(apiToken) }) {
                                    Text("Сохранить токен")
                                }
                            }
                        }
                    }
                }

                // Секция: О приложении
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("О приложении", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Версия: ${BuildConfig.VERSION_NAME}")
                            Text("Трекер финансов — локальный учёт расходов")
                        }
                    }
                }
            }

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            operationStatus?.let { message ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearOperationStatus() }) {
                            Text("OK")
                        }
                    }
                ) {
                    Text(message)
                }
            }
        }
    }

    if (showNewMonthDialog) {
        AlertDialog(
            onDismissRequest = { showNewMonthDialog = false },
            title = { Text("Новый месяц") },
            text = { Text("Текущий период будет завершён, а новый начнётся с лимитом по умолчанию и тем же днём начала. Продолжить?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.startNewMonth()
                    showNewMonthDialog = false
                }) {
                    Text("Да")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewMonthDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Очистка истории") },
            text = { Text("Все данные будут безвозвратно удалены. Продолжить?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllData()
                    showClearDialog = false
                }) {
                    Text("Очистить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showArchiveDialog) {
        val archivedBudgets = allBudgets.filter { it.isActive == 0 }
        AlertDialog(
            onDismissRequest = { showArchiveDialog = false },
            title = { Text("Архив месяцев") },
            text = {
                if (archivedBudgets.isEmpty()) {
                    Text("Нет завершённых периодов")
                } else {
                    Column {
                        archivedBudgets.forEach { budget ->
                            TextButton(onClick = {
                                selectedArchiveBudget = budget
                                showArchiveDialog = false
                                showArchiveDetailDialog = true
                            }) {
                                Text("${budget.monthYear} — ${budget.budgetLimit} ₽ (день начала: ${budget.startDay})")
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showArchiveDialog = false }) {
                    Text("Закрыть")
                }
            }
        )
    }

    if (showArchiveDetailDialog && selectedArchiveBudget != null) {
        AlertDialog(
            onDismissRequest = {
                showArchiveDetailDialog = false
                selectedArchiveBudget = null
            },
            title = { Text("Период: ${selectedArchiveBudget!!.monthYear}") },
            text = { Text("Лимит: ${selectedArchiveBudget!!.budgetLimit} ₽\nДень начала: ${selectedArchiveBudget!!.startDay}\n(Просмотр товаров временно недоступен)") },
            confirmButton = {
                TextButton(onClick = {
                    showArchiveDetailDialog = false
                    selectedArchiveBudget = null
                }) {
                    Text("OK")
                }
            }
        )
    }
}