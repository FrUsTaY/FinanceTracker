package com.emix.financetracker.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.emix.financetracker.ui.theme.*

@Composable
fun OnboardingScreen(
    onBudgetCreated: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    var periodName by remember { mutableStateOf(viewModel.defaultPeriodName) }
    var budgetLimit by remember { mutableStateOf("") }
    var periodError by remember { mutableStateOf("") }
    var limitError by remember { mutableStateOf("") }

    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val success by viewModel.success.collectAsStateWithLifecycle()

    val isFormValid = periodError.isEmpty() && limitError.isEmpty() && budgetLimit.isNotEmpty()

    // Обработка успешного создания бюджета
    LaunchedEffect(success) {
        if (success) {
            onBudgetCreated()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Добро пожаловать\nв Трекер финансов",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Создайте первый период, чтобы начать отслеживание расходов",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = periodName,
            onValueChange = { newValue ->
                periodName = newValue
                validatePeriodName(newValue).let { periodError = it }
            },
            label = { Text("Название периода") },
            isError = periodError.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        )

        if (periodError.isNotEmpty()) {
            Text(text = periodError, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(4.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = budgetLimit,
            onValueChange = { newValue ->
                budgetLimit = newValue
                validateBudgetLimit(newValue).let { limitError = it }
            },
            label = { Text("Лимит бюджета, ₽") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = limitError.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        )

        if (limitError.isNotEmpty()) {
            Text(text = limitError, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(4.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))

        val scope = rememberCoroutineScope()

        Button(
            onClick = {
                if (isFormValid) {
                    val limit = budgetLimit.toDoubleOrNull() ?: 0.0
                    scope.launch {
                        viewModel.createBudget(periodName, limit).onSuccess {
                            viewModel.setSuccess()
                        }.onFailure { e ->
                            viewModel.setErrorMessage("Ошибка: ${e.message}")
                        }
                    }
                }
            },
            enabled = isFormValid && !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Создание...")
            } else {
                Text("Начать отслеживание")
            }
        }

        errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = message, color = MaterialTheme.colorScheme.error)
        }
    }
}

private fun validatePeriodName(value: String): String {
    if (value.isEmpty()) return "Поле не может быть пустым"
    val pattern = """^\d{2}-\d{4}$""".toRegex()
    if (!pattern.matches(value)) return "Формат должен быть MM-YYYY"
    val month = value.substring(0, 2).toIntOrNull()
    val year = value.substring(3, 7).toIntOrNull()
    if (month == null || month < 1 || month > 12) return "Месяц должен быть от 01 до 12"
    if (year == null) return "Неверный формат года"
    return ""
}

private fun validateBudgetLimit(value: String): String {
    if (value.isEmpty()) return "Поле не может быть пустым"
    val limit = value.toDoubleOrNull()
    if (limit == null || limit <= 0) return "Значение должно быть больше 0"
    return ""
}
