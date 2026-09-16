package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double = 0.0,
    val type: String = "DEBIT", // DEBIT or CREDIT
    val merchantName: String = "Others",
    val category: String = "Others",
    val timestamp: Long = System.currentTimeMillis(),
    val rawMessage: String = ""
)
