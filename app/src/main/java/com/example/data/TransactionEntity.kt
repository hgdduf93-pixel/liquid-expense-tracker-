package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val type: String, // DEBIT or CREDIT
    val merchantName: String,
    val category: String,
    val timestamp: Long,
    val rawMessage: String
)
