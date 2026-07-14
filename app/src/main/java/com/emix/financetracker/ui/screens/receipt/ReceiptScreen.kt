package com.emix.financetracker.ui.screens.receipt

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScreen(
    source: String,
    qrData: String?,
    onConfirmed: () -> Unit,
    onBack: () -> Unit,
    viewModel: ReceiptViewModel = hiltViewModel()
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isConfirming by viewModel.isConfirming.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    LaunchedEffect(source, qrData) {
        when (source) {
            "scan" -> {
                if (!qrData.isNullOrEmpty()) {
                    viewModel.loadFromQr(qrData)
                } else {
                    viewModel.initManual()
                }
            }
            else -> viewModel.initManual()
        }
    }

    // Проверяем, есть ли хоть одна валидная отмеченная позиция
    val hasValidCheckedItems by remember(items) {
        derivedStateOf {
            items.filter { it.isChecked }.any { it.isValid() }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Проверка чека") },
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
            if (isLoading || isConfirming) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items, key = { it.id }) { item ->
                            ReceiptItemRow(
                                item = item,
                                onItemChange = { updated -> viewModel.updateItem(item.id, updated) },
                                onToggleCheck = { viewModel.toggleCheck(item.id) },
                                onDelete = { viewModel.removeItem(item.id) }
                            )
                        }
                        item {
                            Button(
                                onClick = { viewModel.addEmptyItem() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Добавить")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Добавить позицию")
                            }
                        }
                    }

                    Button(
                        onClick = { viewModel.confirm(onConfirmed) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        enabled = hasValidCheckedItems
                    ) {
                        Text("Подтвердить и внести")
                    }
                }
            }

            errorMessage?.let { message ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("OK")
                        }
                    }
                ) {
                    Text(message)
                }
            }
        }
    }
}

@Composable
fun ReceiptItemRow(
    item: ReceiptUiItem,
    onItemChange: (ReceiptUiItem) -> Unit,
    onToggleCheck: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = item.isChecked,
                    onCheckedChange = { onToggleCheck() }
                )
                OutlinedTextField(
                    value = item.name,
                    onValueChange = { onItemChange(item.copy(name = it)) },
                    label = { Text("Наименование") },
                    isError = item.nameError.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить")
                }
            }
            if (item.nameError.isNotEmpty()) {
                Text(
                    text = item.nameError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 56.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = item.quantity,
                    onValueChange = { onItemChange(item.copy(quantity = it)) },
                    label = { Text("Кол-во") },
                    isError = item.quantityError.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = item.unit,
                    onValueChange = { onItemChange(item.copy(unit = it)) },
                    label = { Text("Ед. изм.") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = item.pricePerUnit,
                    onValueChange = { onItemChange(item.copy(pricePerUnit = it)) },
                    label = { Text("Цена за ед.") },
                    isError = item.priceError.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                )
            }
            if (item.quantityError.isNotEmpty()) {
                Text(
                    text = item.quantityError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (item.priceError.isNotEmpty()) {
                Text(
                    text = item.priceError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (item.aggregationHint.isNotBlank()) {
                Text(
                    text = item.aggregationHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}