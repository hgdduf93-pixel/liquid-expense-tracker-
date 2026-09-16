package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.Transaction
import kotlinx.coroutines.flow.Flow

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {
    private val db = Room.databaseBuilder(application, AppDatabase::class.java, "funds-db").build()
    private val dao = db.transactionDao()

    val allTransactions: Flow<List<Transaction>> = dao.getAllTransactions()
    
    // Simplification: In a real app, use Hilt/DI.
}
