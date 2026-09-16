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

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactionsFlow(): Flow<List<Transaction>>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE type = 'DEBIT'")
    fun getTotalDebitSpend(): Flow<Double>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'DEBIT' AND timestamp >= :startOfMonth")
    fun getMonthlyTotalDebitFlow(startOfMonth: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'DEBIT' AND timestamp >= :startOfDay")
    fun getTodayTotalDebitFlow(startOfDay: Long): Flow<Double?>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE type = 'DEBIT' AND timestamp >= :startOfDayTimestamp")
    fun getTodayDebitSpend(startOfDayTimestamp: Long): Flow<Double>

    @Query("""
        SELECT * FROM transactions 
        WHERE type = :type 
          AND ABS(amount - :amount) < 0.01 
          AND timestamp >= :windowStartTime 
        LIMIT 1
    """)
    suspend fun findDuplicateCandidate(
        type: String, 
        amount: Double, 
        windowStartTime: Long
    ): Transaction?

    @Query("SELECT * FROM transactions WHERE timestamp >= :sinceTime ORDER BY timestamp DESC")
    suspend fun getRecentTransactions(sinceTime: Long): List<Transaction>

    @Query("SELECT category, SUM(amount) as total FROM transactions WHERE type = 'DEBIT' GROUP BY category ORDER BY total DESC")
    fun getCategoryWiseSpend(): Flow<List<CategorySpend>>

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM transactions")
    suspend fun clearAll()
}
