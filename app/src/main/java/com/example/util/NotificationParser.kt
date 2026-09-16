package com.example.util

import com.example.data.Transaction

object NotificationParser {
    // Regex for amount, very basic for now
    private val amountRegex = Regex("(?i)(?:Rs\\.?|INR)\\s?([0-9,]+(?:\\.[0-9]{2})?)|(?:debited\\s+by\\s+([0-9,]+))")

    fun parse(title: String, text: String): Transaction? {
        val message = "$title $text"
        val amountMatch = amountRegex.find(message) ?: return null
        
        val amount = (amountMatch.groups[1]?.value ?: amountMatch.groups[2]?.value)
            ?.replace(",", "")?.toDoubleOrNull() ?: return null
            
        val type = if (message.contains("debited", ignoreCase = true) || message.contains("paid", ignoreCase = true) || message.contains("sent", ignoreCase = true)) "DEBIT" else "CREDIT"
        
        // Simple categorization
        val category = when {
            message.contains("Zomato", ignoreCase = true) || message.contains("Swiggy", ignoreCase = true) -> "Food"
            message.contains("Uber", ignoreCase = true) || message.contains("Ola", ignoreCase = true) -> "Travel"
            message.contains("Amazon", ignoreCase = true) || message.contains("Flipkart", ignoreCase = true) -> "Shopping"
            message.contains("Recharge", ignoreCase = true) || message.contains("Electricity", ignoreCase = true) -> "Bills"
            else -> "Others"
        }

        return Transaction(
            amount = amount,
            type = type,
            merchantName = title,
            category = category,
            timestamp = System.currentTimeMillis(),
            rawMessage = message
        )
    }
}
