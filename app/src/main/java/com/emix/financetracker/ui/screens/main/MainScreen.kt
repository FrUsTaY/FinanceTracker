package com.emix.financetracker.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.emix.financetracker.data.db.entity.ProductsMonthEntity
import com.emix.financetracker.ui.theme.*
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onScanQr: () -> Unit,
    onAddManual: () -> Unit,
    onProductClick: (canonicalName: String, unit: String) -> Unit,
    onSettingsClick: () -> Unit,
    onAddPurchaseClick: () -> Unit = onAddManual,
    viewModel: MainViewModel = hiltViewModel()
) {
    val budgetState by viewModel.budgetState.collectAsStateWithLifecycle()
    val sortedProducts by viewModel.sortedProducts.collectAsStateWithLifecycle()
    val mergeSourceId by viewModel.mergeSourceId.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var productToDelete by remember { mutableStateOf<ProductsMonthEntity?>(null) }
    var showMergeDialog by remember { mutableStateOf<Pair<ProductsMonthEntity, ProductsMonthEntity>?>(null) }

    Scaffold(
        topBar = {
            MainTopAppBar(
                onSettingsClick = onSettingsClick,
                budgetState = budgetState,
                isMergeMode = mergeSourceId != null,
                onCancelMerge = { viewModel.cancelMerge() }
            )
        },
        floatingActionButton = {
            if (mergeSourceId == null) {
                MainFloatingActions(onScanQr = onScanQr, onAddManual = onAddManual)
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Black)
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            val currentBudgetState = budgetState
            if (currentBudgetState == null) {
                CircularProgressIndicator(color = AccentGreen)
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Banner-reminder
                    if (shouldShowBanner(currentBudgetState.periodName, currentBudgetState.startDay)) {
                        ReminderBanner(onClick = onSettingsClick)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Budget section
                    BudgetSection(budgetState = currentBudgetState)

                    Spacer(modifier = Modifier.height(24.dp))

                    // Table header
                    TableHeader(viewModel.sortOrder) { order ->
                        viewModel.setSortOrder(order)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Product list with swipe to delete and merge
                    if (sortedProducts.isEmpty()) {
                        EmptyState(modifier = Modifier.weight(1f), onAddPurchaseClick = onAddPurchaseClick)
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            items(sortedProducts, key = { it.id }) { product ->
                                val isSelectedAsDonor = mergeSourceId == product.id
                                val isHighlighted = mergeSourceId != null && !isSelectedAsDonor
                                SwipeToDeleteItem(
                                    product = product,
                                    onProductClick = {
                                        val encodedCn = try {
                                            java.net.URLEncoder.encode(product.canonicalName, "UTF-8")
                                        } catch (e: Exception) {
                                            product.canonicalName
                                        }
                                        val encodedUnit = try {
                                            java.net.URLEncoder.encode(product.unit, "UTF-8")
                                        } catch (e: Exception) {
                                            product.unit
                                        }
                                        onProductClick(encodedCn, encodedUnit)
                                    },
                                    onDelete = { productToDelete = product },
                                    onLongClick = { viewModel.startMerge(product.id) },
                                    isMergeMode = mergeSourceId != null,
                                    isSelectedAsDonor = isSelectedAsDonor,
                                    isHighlighted = isHighlighted,
                                    onTargetClick = { target ->
                                        val donorId = mergeSourceId ?: return@SwipeToDeleteItem
                                        val donor = sortedProducts.find { it.id == donorId } ?: return@SwipeToDeleteItem
                                        showMergeDialog = Pair(donor, target)
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // Диалог подтверждения удаления
    if (productToDelete != null) {
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("Удаление") },
            text = { Text("Удалить \"${productToDelete!!.productName}\"? Бюджет будет пересчитан.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            viewModel.deleteProduct(productToDelete!!)
                            productToDelete = null
                        }
                    }
                ) {
                    Text("Удалить", color = AccentRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Диалог подтверждения склейки
    if (showMergeDialog != null) {
        val (donor, target) = showMergeDialog!!
        AlertDialog(
            onDismissRequest = { showMergeDialog = null },
            title = { Text("Объединение товаров") },
            text = { Text("Объединить \"${donor.productName}\" с \"${target.productName}\"?\nКоличество и сумма будут суммированы.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            viewModel.mergeProducts(donor.id, target.id)
                            showMergeDialog = null
                        }
                    }
                ) {
                    Text("Объединить", color = AccentGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMergeDialog = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTopAppBar(
    onSettingsClick: () -> Unit,
    budgetState: BudgetUiState?,
    isMergeMode: Boolean,
    onCancelMerge: () -> Unit
) {
    TopAppBar(
        title = { Text(text = if (isMergeMode) "Выберите целевой товар" else (budgetState?.periodName ?: ""), maxLines = 1) },
        navigationIcon = {},
        actions = {
            if (isMergeMode) {
                IconButton(onClick = onCancelMerge) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Отмена склейки",
                        tint = OnSurface
                    )
                }
            } else {
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Настройки",
                        tint = OnSurface
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Black,
            titleContentColor = OnSurface
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteItem(
    product: ProductsMonthEntity,
    onProductClick: () -> Unit,
    onDelete: () -> Unit,
    onLongClick: () -> Unit,
    isMergeMode: Boolean,
    isSelectedAsDonor: Boolean,
    isHighlighted: Boolean,
    onTargetClick: (ProductsMonthEntity) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удаление") },
            text = { Text("Удалить \"${product.productName}\"? Бюджет будет пересчитан.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Удалить", color = AccentRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.EndToStart -> {
                    showDeleteDialog = true
                    false
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AccentRed)
                    .padding(end = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Удалить",
                    tint = Color.White
                )
            }
        }
    ) {
        val backgroundColor = when {
            isSelectedAsDonor -> AccentGreen.copy(alpha = 0.3f)
            isHighlighted -> Color.Transparent
            else -> DarkSurface
        }
        ProductRow(
            product = product,
            onClick = {
                if (isMergeMode && !isSelectedAsDonor) {
                    onTargetClick(product)
                } else {
                    onProductClick()
                }
            },
            onLongClick = onLongClick,
            backgroundColor = backgroundColor
        )
    }
}

@Composable
private fun ReminderBanner(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AccentYellow)
            .padding(16.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Вы ещё не начали новый месяц. Перейти в настройки?",
            color = Black,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun BudgetSection(budgetState: BudgetUiState) {
    val progressColor = when {
        budgetState.progress < 0.8f -> AccentGreen
        budgetState.progress <= 1.0f -> AccentYellow
        else -> AccentRed
    }

    val remainingColor = when {
        budgetState.progress > 1.0f -> AccentRed
        budgetState.progress > 0.9f -> AccentYellow
        else -> OnSurface
    }

    Text(
        text = "Осталось: ${budgetState.remaining} ₽ из ${budgetState.budgetLimit} ₽",
        style = MaterialTheme.typography.titleMedium,
        color = remainingColor
    )

    Spacer(modifier = Modifier.height(8.dp))

    LinearProgressIndicator(
        progress = { budgetState.progress },
        color = progressColor,
        trackColor = DarkSurfaceVariant,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun TableHeader(
    sortOrder: StateFlow<SortOrder>,
    onSortOrderChange: (SortOrder) -> Unit
) {
    val currentOrder by sortOrder.collectAsStateWithLifecycle()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp) // тот же отступ, что и у строк
    ) {
        SortableHeader(
            text = "Наименование",
            currentOrder = currentOrder,
            sortValue = SortOrder.NAME_ASC,
            sortValueDesc = SortOrder.NAME_DESC,
            onClick = onSortOrderChange
        )

        SortableHeader(
            text = "Кол-во",
            currentOrder = currentOrder,
            sortValue = SortOrder.QTY_ASC,
            sortValueDesc = SortOrder.QTY_DESC,
            onClick = onSortOrderChange
        )

        SortableHeader(
            text = "Сумма",
            currentOrder = currentOrder,
            sortValue = SortOrder.AMOUNT_ASC,
            sortValueDesc = SortOrder.AMOUNT_DESC,
            onClick = onSortOrderChange
        )
    }
}

@Composable
private fun RowScope.SortableHeader(
    text: String,
    currentOrder: SortOrder,
    sortValue: SortOrder,
    sortValueDesc: SortOrder,
    onClick: (SortOrder) -> Unit
) {
    val isActive = currentOrder == sortValue || currentOrder == sortValueDesc
    val isAscending = currentOrder == sortValue

    // Выравнивание в зависимости от столбца
    val textAlign = when (text) {
        "Наименование" -> TextAlign.Start
        "Кол-во" -> TextAlign.Center
        "Сумма" -> TextAlign.End
        else -> TextAlign.Start
    }

    val arrangement = when (text) {
        "Наименование" -> Arrangement.Start
        "Кол-во" -> Arrangement.Center
        "Сумма" -> Arrangement.End
        else -> Arrangement.Start
    }

    Row(
        modifier = Modifier
            .weight(1f)
            .clickable { onClick(sortValue) },
        horizontalArrangement = arrangement,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = if (isActive) OnSurface else OnSurfaceSecondary,
            modifier = Modifier.weight(1f),
            textAlign = textAlign
        )

        if (isActive) {
            Text(
                text = if (isAscending) "↑" else "↓",
                modifier = Modifier.size(16.dp),
                color = OnSurface
            )
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier, onAddPurchaseClick: () -> Unit) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            tint = OnSurfaceSecondary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Добавьте первую покупку",
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceSecondary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onAddPurchaseClick) {
            Icon(Icons.Default.Add, contentDescription = "Добавить")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Добавить первую покупку")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainFloatingActions(onScanQr: () -> Unit, onAddManual: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        FloatingActionButton(
            onClick = onScanQr,
            containerColor = AccentGreen,
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "QR",
                color = Color(0xFF0D0D0D),
                fontWeight = FontWeight.Bold
            )
        }
        FloatingActionButton(
            onClick = onAddManual,
            containerColor = AccentGreen,
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "+",
                color = Color(0xFF0D0D0D),
                fontSize = 24.sp
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductRow(
    product: ProductsMonthEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    backgroundColor: Color = DarkSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(backgroundColor)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Наименование – прижато к левому краю
        Text(
            text = product.productName,
            modifier = Modifier.weight(1f),
            softWrap = true,
            overflow = TextOverflow.Visible,
            textAlign = TextAlign.Start
        )

        // Количество – выровнено по центру
        Text(
            text = "${product.totalQuantity} ${product.unit}",
            modifier = Modifier.weight(0.4f),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Сумма – прижата к правому краю
        Text(
            text = String.format("%.2f ₽", product.totalAmount),
            modifier = Modifier.weight(0.4f),
            textAlign = TextAlign.End,
            maxLines = 1
        )
    }
}

private fun shouldShowBanner(periodName: String, startDay: Int): Boolean {
    return try {
        val parts = periodName.split("-")
        if (parts.size != 2) return false
        val periodMonth = parts[0].toInt()
        val periodYear = parts[1].toInt()

        // Вычисляем дату окончания периода: последний день периода (день перед следующим startDay)
        val endCalendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, periodYear)
            set(Calendar.MONTH, periodMonth - 1)
            val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
            val actualStartDay = startDay.coerceIn(1, maxDay)
            set(Calendar.DAY_OF_MONTH, actualStartDay)
            add(Calendar.MONTH, 1)
            add(Calendar.DAY_OF_MONTH, -1)
        }
        val today = Calendar.getInstance()
        today.timeInMillis > endCalendar.timeInMillis
    } catch (e: Exception) {
        false
    }
}