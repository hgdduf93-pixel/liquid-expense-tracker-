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
        
        // Categorization using regex for better pattern matching
        val category = when {
            Regex("Zomato|Swiggy|Domino|KFC|McD", RegexOption.IGNORE_CASE).containsMatchIn(message) -> "Food"
            Regex("Uber|Ola|Rapido|Metro|Train", RegexOption.IGNORE_CASE).containsMatchIn(message) -> "Travel"
            Regex("Amazon|Flipkart|Myntra|Nykaa", RegexOption.IGNORE_CASE).containsMatchIn(message) -> "Shopping"
            Regex("Netflix|Prime|Spotify|YouTube|Hotstar", RegexOption.IGNORE_CASE).containsMatchIn(message) -> "Entertainment"
            Regex("Recharge|Electricity|Water|Gas|Broadband", RegexOption.IGNORE_CASE).containsMatchIn(message) -> "Bills"
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
