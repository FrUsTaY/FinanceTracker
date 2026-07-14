package com.emix.financetracker.data.network

// Модели для ответа API proverkacheka.com (или прямого ответа ФНС)
data class ReceiptItem(
    val name: String,
    val quantity: Double,
    val pricePerUnit: Double,   // в рублях (после конвертации)
    val totalAmount: Double,    // в рублях
    val unit: String            // текстовая единица измерения
)

// Ответ от API proverkacheka.com (с обёрткой)
data class ReceiptResponse(
    val code: Int,           // 1 - успех, 0 - ошибка, 2 - ожидание
    val data: ReceiptData? = null,
    val message: String? = null
)

data class ReceiptData(
    val json: ReceiptJson? = null
)

// Прямой ответ от ФНС (без обёртки) – используем если API вернул такой формат
data class FnsReceipt(
    val items: List<FnsReceiptItem>? = null,
    val operationType: Int? = null,      // 1 – приход, 2 – возврат прихода и т.д.
    val totalSum: Int? = null,           // в копейках
    val dateTime: String? = null,
    val user: String? = null
)

data class FnsReceiptItem(
    val name: String,
    val price: Int,           // цена за единицу в КОПЕЙКАХ
    val quantity: Double,
    val sum: Int,             // общая сумма позиции в КОПЕЙКАХ
    val itemsQuantityMeasure: Int? = null,   // код единицы измерения (тег 2108)
    val measurementUnit: String? = null,     // текстовое название (может отсутствовать)
    val productType: Int? = null,
    val nds: Int? = null
)

// Структура для обёртки proverkacheka.com
data class ReceiptJson(
    val items: List<FnsReceiptItem>? = null   // внутри data.json лежит такой же список, как у ФНС
)