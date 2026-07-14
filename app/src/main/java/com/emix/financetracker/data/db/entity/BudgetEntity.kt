package com.emix.financetracker.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "Budgets",
    indices = [Index(value = ["monthYear"], unique = true)]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Int = 0,
    @ColumnInfo(name = "monthYear") val monthYear: String,
    @ColumnInfo(name = "budgetLimit") val budgetLimit: Double,
    @ColumnInfo(name = "isActive") val isActive: Int,
    @ColumnInfo(name = "defaultLimit") val defaultLimit: Double? = null,
    @ColumnInfo(name = "startDay", defaultValue = "1") val startDay: Int = 1  // день начала периода (1-31)
)