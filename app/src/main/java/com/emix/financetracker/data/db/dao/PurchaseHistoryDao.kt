package com.emix.financetracker.data.db.dao

import androidx.room.*
import com.emix.financetracker.data.db.entity.PurchaseHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseHistoryDao {
    @Insert
    suspend fun insert(history: PurchaseHistoryEntity): Long

    @Query("SELECT * FROM PurchaseHistory WHERE canonicalName = :canonicalName AND unit = :unit ORDER BY purchaseDate ASC")
    fun getPriceHistory(canonicalName: String, unit: String): Flow<List<PurchaseHistoryEntity>>

    @Query("DELETE FROM PurchaseHistory")
    suspend fun deleteAll()

    @Query("SELECT * FROM PurchaseHistory")
    suspend fun getAllHistory(): List<PurchaseHistoryEntity>
}
