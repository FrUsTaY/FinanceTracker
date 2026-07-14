package com.emix.financetracker.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "PurchaseHistory",
    foreignKeys = [
        ForeignKey(
            entity = BudgetEntity::class,
            parentColumns = ["id"],
            childColumns = ["budgetId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("budgetId"),
        Index("canonicalName")
    ]
)
data class PurchaseHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") var id: Int = 0,
    @ColumnInfo(name = "budgetId") val budgetId: Int?,
    @ColumnInfo(name = "productName") val productName: String,
    @ColumnInfo(name = "canonicalName") val canonicalName: String,
    @ColumnInfo(name = "unit") val unit: String = "шт",
    @ColumnInfo(name = "purchaseDate") val purchaseDate: String,
    @ColumnInfo(name = "pricePerUnit") val pricePerUnit: Double
)
