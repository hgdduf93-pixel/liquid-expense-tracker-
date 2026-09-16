package com.example.util

import com.example.data.Transaction
import java.util.Locale
import java.util.regex.Pattern

object NotificationParser {
    private val blacklistedKeywords = listOf(
        "otp", "verification code", "one time password", "secret code",
        "pre-approved", "apply now", "congratulations", "cashback won", "reward", "offer",
        "due date", "bill generated", "minimum due", "statement for"
    )

    private val balanceInquiryKeywords = listOf("available balance", "avl bal", "inquiry")
    private val debitCreditMarkers = listOf("debited", "credited", "spent", "paid", "sent", "received")

    // Regex to capture amounts like "Rs. 250", "Rs 1,250.50", "INR 400", "₹500", "debited by 300"
    private val amountRegex = Regex(
        """(?i)(?:(?:rs\.?|inr|₹)\s*|debited\s+(?:by|with)?\s*(?:rs\.?|inr|₹)?\s*|paid\s+(?:rs\.?|inr|₹)?\s*|spent\s+(?:rs\.?|inr|₹)?\s*|sent\s+(?:rs\.?|inr|₹)?\s*|credited\s+(?:with|by)?\s*(?:rs\.?|inr|₹)?\s*)([0-9]{1,3}(?:,[0-9]{2,3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)"""
    )

    // Regex to extract merchant name or receiver name
    private val merchantRegex = Regex(
        """(?i)(?:paid\s+to|sent\s+to|transfer\s+to|transferred\s+to|debited\s+for|to\s+vpa|to|at)\s+([A-Za-z0-9\s.&'-]{2,35}?)(?:\s+(?:on|ref|txn|via|using|upi|bal|avbl|a/c|ending|\.|\,)|$)"""
    )

    fun parse(title: String, text: String): Transaction? {
        val rawCombined = "$title $text".trim()
        if (rawCombined.isEmpty()) return null
        val lowerMessage = rawCombined.lowercase(Locale.ROOT)
        val combinedMessage = rawCombined

        // Strict Anti-Spam & OTP Filter
        if (blacklistedKeywords.any { lowerMessage.contains(it) }) {
            return null
        }
        if (balanceInquiryKeywords.any { lowerMessage.contains(it) }) {
            val hasMarker = debitCreditMarkers.any { lowerMessage.contains(it) }
            if (!hasMarker) return null
        }

        // Look for amount
        val match = amountRegex.find(rawCombined) ?: return null
        val amountStr = match.groups[1]?.value?.replace(",", "")?.trim() ?: return null
        val amount = amountStr.toDoubleOrNull() ?: return null
        if (amount <= 0.0) return null

        // Determine transaction type
        val isCredit = Regex("""(?i)\b(?:credited|received|deposited|refund|cashback)\b""").containsMatchIn(combinedMessage)
        val isDebit = Regex("""(?i)\b(?:debited|paid|sent|spent|transferred|withdrawn|purchase)\b""").containsMatchIn(combinedMessage)

        val type = when {
            isDebit -> "DEBIT"
            isCredit -> "CREDIT"
            else -> "DEBIT" // default assumption for payments
        }

        // Extract merchant / recipient
        val merchantMatch = merchantRegex.find(combinedMessage)
        var rawMerchant = merchantMatch?.groups?.get(1)?.value?.trim() ?: ""

        // Clean up common noise from merchant name
        val wordsToClean = listOf("your", "account", "a/c", "vpa", "bank", "the", "ref", "upi")
        if (wordsToClean.any { rawMerchant.equals(it, ignoreCase = true) } || rawMerchant.length < 2) {
            rawMerchant = ""
        }

        val merchantName = when {
            rawMerchant.isNotEmpty() -> cleanMerchantName(rawMerchant)
            title.isNotEmpty() && !title.contains("message", ignoreCase = true) && !title.contains("notification", ignoreCase = true) -> cleanMerchantName(title)
            else -> "Merchant Payment"
        }

        // Auto-categorize
        val category = autoCategorize(merchantName, combinedMessage)

        return Transaction(
            amount = amount,
            type = type,
            merchantName = merchantName,
            category = category,
            timestamp = System.currentTimeMillis(),
            rawMessage = combinedMessage
        )
    }

    private fun cleanMerchantName(raw: String): String {
        return raw.split(" ")
            .filter { it.isNotBlank() }
            .take(4)
            .joinToString(" ") { word ->
                word.lowercase(Locale.ROOT).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            }
    }

    private fun autoCategorize(merchant: String, message: String): String {
        val target = "$merchant $message".lowercase(Locale.ROOT)

        return when {
            // Food & Groceries
            Regex("""\b(swiggy|zomato|domino|mcdonald|kfc|starbucks|burger|pizza|cafe|restaurant|dining|blinkit|zepto|instamart|bigbasket|dunzo|bakery|food)\b""").containsMatchIn(target) -> "Food"

            // Travel & Transport
            Regex("""\b(uber|ola|rapido|metro|irctc|train|rail|flight|indigo|air india|makemytrip|shell|petrol|fuel|hpcl|bpcl|iocl|fastag|cab|taxi|toll)\b""").containsMatchIn(target) -> "Travel"

            // Bills & Utilities
            Regex("""\b(electricity|water|gas|recharge|jio|airtel|vi|broadband|bescom|tata play|dth|rent|maintenance|bill|postpaid|prepaid|utility)\b""").containsMatchIn(target) -> "Bills"

            // Shopping & Retail
            Regex("""\b(amazon|flipkart|myntra|nykaa|ajio|zara|h&m|croma|reliance|apple|store|mall|retail|mart|cloth|supermarket)\b""").containsMatchIn(target) -> "Shopping"

            // Entertainment
            Regex("""\b(netflix|spotify|prime|youtube|hotstar|sonyliv|zee5|bookmyshow|cinema|pvr|inox|movie|game|steam)\b""").containsMatchIn(target) -> "Entertainment"

            // Health & Pharmacy
            Regex("""\b(apollo|1mg|pharmeasy|medplus|hospital|clinic|doctor|pharmacy|medical|dentist)\b""").containsMatchIn(target) -> "Health"

            // Income / Salary
            Regex("""\b(salary|dividend|interest|cashback|refund|credited by client)\b""").containsMatchIn(target) -> "Income"

            else -> "Others"
        }
    }
}
