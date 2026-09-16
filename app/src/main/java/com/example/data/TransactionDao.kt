package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class CategorySpend(
    val category: String,
    val total: Double
)

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE type = 'DEBIT'")
    fun getTotalDebitSpend(): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE type = 'DEBIT' AND timestamp >= :startOfDayTimestamp")
    fun getTodayDebitSpend(startOfDayTimestamp: Long): Flow<Double>

    @Query("SELECT category, SUM(amount) as total FROM transactions WHERE type = 'DEBIT' GROUP BY category ORDER BY total DESC")
    fun getCategoryWiseSpend(): Flow<List<CategorySpend>>

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM transactions")
    suspend fun clearAll()
}
