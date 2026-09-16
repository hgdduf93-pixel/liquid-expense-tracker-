package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.CategorySpend
import com.example.data.Transaction
import com.example.data.TransactionDao
import com.example.util.NotificationParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val isRoomAvailable: Boolean
    private val dao: TransactionDao?

    // Resilient local in-memory fallback state for smooth preview sandbox & offline resilience
    private val _inMemoryTransactions = MutableStateFlow<List<Transaction>>(
        listOf(
            Transaction(
                id = 1,
                amount = 450.0,
                type = "DEBIT",
                merchantName = "Swiggy Food",
                category = "Food",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 25,
                rawMessage = "Paid Rs. 450 to Swiggy via UPI"
            ),
            Transaction(
                id = 2,
                amount = 220.0,
                type = "DEBIT",
                merchantName = "Uber Technologies",
                category = "Travel",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 2,
                rawMessage = "Debited Rs. 220 for Uber Ride"
            ),
            Transaction(
                id = 3,
                amount = 1899.0,
                type = "DEBIT",
                merchantName = "Amazon India",
                category = "Shopping",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 6,
                rawMessage = "Spent Rs. 1,899 at Amazon"
            ),
            Transaction(
                id = 4,
                amount = 25000.0,
                type = "CREDIT",
                merchantName = "Salary Credited",
                category = "Income",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 24,
                rawMessage = "Account credited with Rs. 25,000"
            )
        )
    )

    init {
        var d: TransactionDao? = null
        var available = false
        try {
            val db = AppDatabase.getInstance(application)
            d = db.transactionDao()
            available = true
        } catch (t: Throwable) {
            Log.w("ExpenseViewModel", "Room Database unavailable, using in-memory state: ${t.message}")
            available = false
            d = null
        }
        dao = d
        isRoomAvailable = available
    }

    private fun getStartOfDayTimestamp(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    val allTransactions: StateFlow<List<Transaction>> = if (isRoomAvailable && dao != null) {
        dao.getAllTransactions()
            .catch { emit(_inMemoryTransactions.value) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = _inMemoryTransactions.value
            )
    } else {
        _inMemoryTransactions.asStateFlow()
    }

    val totalDebits: StateFlow<Double> = allTransactions
        .map { list -> list.filter { it.type == "DEBIT" }.sumOf { it.amount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalCredits: StateFlow<Double> = allTransactions
        .map { list -> list.filter { it.type == "CREDIT" }.sumOf { it.amount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val netBalance: StateFlow<Double> = combine(totalCredits, totalDebits) { credits, debits ->
        credits - debits
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val todayDebits: StateFlow<Double> = allTransactions
        .map { list ->
            val startOfDay = getStartOfDayTimestamp()
            list.filter { it.type == "DEBIT" && it.timestamp >= startOfDay }.sumOf { it.amount }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val todayCredits: StateFlow<Double> = allTransactions
        .map { list ->
            val startOfDay = getStartOfDayTimestamp()
            list.filter { it.type == "CREDIT" && it.timestamp >= startOfDay }.sumOf { it.amount }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val todayNetSpent: StateFlow<Double> = combine(todayDebits, todayCredits) { debits, credits ->
        debits - credits
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalSpent: StateFlow<Double> = totalDebits

    val todaySpent: StateFlow<Double> = todayDebits

    val categoryBreakdown: StateFlow<List<CategorySpend>> = allTransactions
        .map { list ->
            list.filter { it.type == "DEBIT" }
                .groupBy { it.category }
                .map { (category, items) -> CategorySpend(category, items.sumOf { it.amount }) }
                .sortedByDescending { it.total }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private fun getStartOfMonthTimestamp(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    private val prefs = application.getSharedPreferences("mudrix_prefs", android.content.Context.MODE_PRIVATE)
    private val _monthlyBudgetState = MutableStateFlow(prefs.getFloat("monthly_budget_target", 0f).toDouble())
    val monthlyBudget: StateFlow<Double> = _monthlyBudgetState.asStateFlow()

    fun setMonthlyBudget(amount: Double) {
        _monthlyBudgetState.value = amount
        prefs.edit().putFloat("monthly_budget_target", amount.toFloat()).apply()
    }

    val totalSpentThisMonth: StateFlow<Double> = allTransactions
        .map { list ->
            val startOfMonth = getStartOfMonthTimestamp()
            list.filter { it.type == "DEBIT" && it.timestamp >= startOfMonth }.sumOf { it.amount }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = _inMemoryTransactions.value.filter { it.type == "DEBIT" && it.timestamp >= getStartOfMonthTimestamp() }.sumOf { it.amount }
        )

    fun insertTransaction(transaction: Transaction) {
        // Always update in-memory state so UI updates instantaneously
        val currentList = _inMemoryTransactions.value
        _inMemoryTransactions.value = listOf(transaction) + currentList

        // Persist to Room if available
        if (isRoomAvailable && dao != null) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    dao.insert(transaction)
                } catch (e: Throwable) {
                    Log.e("ExpenseViewModel", "Failed to insert into Room", e)
                }
            }
        }
    }

    fun deleteTransaction(id: Int) {
        _inMemoryTransactions.value = _inMemoryTransactions.value.filter { it.id != id }

        if (isRoomAvailable && dao != null) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    dao.deleteById(id)
                } catch (e: Throwable) {
                    Log.e("ExpenseViewModel", "Failed to delete from Room", e)
                }
            }
        }
    }

    fun clearAllTransactions() {
        _inMemoryTransactions.value = emptyList()

        if (isRoomAvailable && dao != null) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    dao.clearAll()
                } catch (e: Throwable) {
                    Log.e("ExpenseViewModel", "Failed to clear Room", e)
                }
            }
        }
    }

    /**
     * Helper to simulate a real notification transaction for easy testing on emulator or physical device.
     */
    fun simulateTestTransaction(title: String, body: String) {
        val parsed = NotificationParser.parse(title, body) ?: Transaction(
            amount = 350.0,
            type = "DEBIT",
            merchantName = title,
            category = "Food",
            timestamp = System.currentTimeMillis(),
            rawMessage = "$title $body"
        )
        insertTransaction(parsed)
    }
}
