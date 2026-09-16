package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class CategorySpend(
    val category: String,
    val total: Double
)

@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(transaction: Transaction)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'DEBIT' AND strftime('%m', timestamp / 1000, 'unixepoch') = :month AND strftime('%Y', timestamp / 1000, 'unixepoch') = :year")
    fun getTotalSpendOfMonth(month: String, year: String): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'DEBIT' AND date(timestamp / 1000, 'unixepoch') = date('now')")
    fun getTodaySpend(): Flow<Double?>
    
    @Query("SELECT category, SUM(amount) as total FROM transactions WHERE type = 'DEBIT' GROUP BY category")
    fun getCategoryWiseSpend(): Flow<List<CategorySpend>>
}
