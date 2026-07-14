package com.emix.financetracker.ui.screens.receipt

import java.util.UUID

data class ReceiptUiItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val quantity: String = "",
    val unit: String = "шт",
    val pricePerUnit: String = "",
    val isChecked: Boolean = true,
    val aggregationHint: String = "",
    val nameError: String = "",
    val quantityError: String = "",
    val priceError: String = ""
) {
    fun isValid(): Boolean =
        nameError.isEmpty() && quantityError.isEmpty() && priceError.isEmpty() && name.isNotBlank()
}