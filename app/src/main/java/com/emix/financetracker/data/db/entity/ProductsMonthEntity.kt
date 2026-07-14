package com.emix.financetracker.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ProductsMonth",
    foreignKeys = [
        ForeignKey(
            entity = BudgetEntity::class,
            parentColumns = ["id"],
            childColumns = ["budgetId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("budgetId"),
        Index("canonicalName"),
        Index("mergedIntoId")
    ]
)
data class ProductsMonthEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") var id: Int = 0,
    @ColumnInfo(name = "budgetId") val budgetId: Int,
    @ColumnInfo(name = "productName") val productName: String,
    @ColumnInfo(name = "canonicalName") val canonicalName: String,
    @ColumnInfo(name = "unit") val unit: String = "шт",
    @ColumnInfo(name = "totalQuantity") val totalQuantity: Double = 0.0,
    @ColumnInfo(name = "totalAmount") val totalAmount: Double = 0.0,
    @ColumnInfo(name = "mergedIntoId") val mergedIntoId: Int? = null
)
