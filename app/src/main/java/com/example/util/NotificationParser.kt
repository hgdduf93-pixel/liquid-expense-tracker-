package com.example.util

import com.example.data.Transaction
import java.util.Locale
import java.util.regex.Pattern

object NotificationParser {
    private val promoBlacklist = listOf(
        "upto", "up to", "win", "refer", "cashback won", "reward", "scratch card", "bonus", "coupon", "voucher", "pre-approved", "apply now", "flat off", "deal of the day",
        "otp", "verification code", "one time password", "secret code", "statement", "bill due", "due date", "overdue", "reminder", "kyc"
    )

    private val bankingAnchors = listOf(
        Regex("""(?i)(?:a/c|acct|account|card|wallet)[\s\S]*?(?:ending|no\.?|xx|\*{2,})[0-9]{3,4}"""),
        Regex("""(?i)(?:upi\s*ref|rrn|txn\s*(?:id|no)|ref\s*no|utr|imps|order\s*id)[\s:]*[a-zA-Z0-9]{6,22}"""),
        Regex("""(?i)(?:avl\s*bal|available\s*balance|bal\s*inr|current\s*bal)[\s:]*(?:rs\.?|inr|₹)?\s*[0-9,]+""")
    )

    private val balanceInquiryKeywords = listOf("available balance", "avl bal", "inquiry")
    private val debitCreditMarkers = listOf("debited", "credited", "spent", "paid", "sent", "received")

    private data class ParseRule(val regex: Regex, val isCredit: Boolean, val amountGroup: Int, val merchantGroup: Int)

    private val creditRules = listOf(
        ParseRule(Regex("""(?i)received\s+(?:₹|rs\.?|inr)\s*([0-9,]+(?:\.[0-9]{1,2})?)\s+from\s+([A-Za-z0-9\s._'-]+)"""), true, 1, 2),
        ParseRule(Regex("""(?i)payment\s+of\s+(?:₹|rs\.?|inr)\s*([0-9,]+(?:\.[0-9]{1,2})?)\s+received\s+from\s+([A-Za-z0-9\s._'-]+)"""), true, 1, 2),
        ParseRule(Regex("""(?i)(?:₹|rs\.?|inr)\s*([0-9,]+(?:\.[0-9]{1,2})?)\s+received\s+from\s+([A-Za-z0-9\s._'-]+)"""), true, 1, 2),
        ParseRule(Regex("""(?i)you\s+have\s+received\s+(?:₹|rs\.?|inr)\s*([0-9,]+(?:\.[0-9]{1,2})?)\s+from\s+([A-Za-z0-9\s._'-]+)"""), true, 1, 2),
        ParseRule(Regex("""(?i)^([A-Za-z0-9\s._'-]+?)\s+paid\s+you\s+(?:₹|rs\.?|inr)\s*([0-9,]+(?:\.[0-9]{1,2})?)"""), true, 2, 1),
        ParseRule(Regex("""(?i)(?:₹|rs\.?|inr)\s*([0-9,]+(?:\.[0-9]{1,2})?)\s+credited\s+to\s+your\s+(?:account|wallet|vpa)"""), true, 1, -1),
        ParseRule(Regex("""(?i)money\s+transferred\s+to\s+your\s+account[\s\S]*?(?:₹|rs\.?|inr)\s*([0-9,]+(?:\.[0-9]{1,2})?)"""), true, 1, -1),
        ParseRule(Regex("""(?i)(?:paid\s+you|paid\s+to\s+you|received\s+from|credited\s+to|added\s+to|deposited|refund\s+of)\s*(?:rs\.?|inr|₹)?\s*([0-9]{1,3}(?:,[0-9]{2,3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)"""), true, 1, -1)
    )

    private val debitRules = listOf(
        ParseRule(Regex("""(?i)paid\s+(?:₹|rs\.?|inr)\s*([0-9,]+(?:\.[0-9]{1,2})?)\s+to\s+([A-Za-z0-9\s._'-]+)"""), false, 1, 2),
        ParseRule(Regex("""(?i)payment\s+of\s+(?:₹|rs\.?|inr)\s*([0-9,]+(?:\.[0-9]{1,2})?)\s+to\s+([A-Za-z0-9\s._'-]+)\s+successful"""), false, 1, 2),
        ParseRule(Regex("""(?i)(?:₹|rs\.?|inr)\s*([0-9,]+(?:\.[0-9]{1,2})?)\s+paid\s+to\s+([A-Za-z0-9\s._'-]+)"""), false, 1, 2),
        ParseRule(Regex("""(?i)sent\s+(?:₹|rs\.?|inr)\s*([0-9,]+(?:\.[0-9]{1,2})?)\s+to\s+([A-Za-z0-9\s._'-]+)"""), false, 1, 2),
        ParseRule(Regex("""(?i)transfer\s+of\s+(?:₹|rs\.?|inr)\s*([0-9,]+(?:\.[0-9]{1,2})?)\s+to\s+([A-Za-z0-9\s._'-]+)\s+successful"""), false, 1, 2),
        ParseRule(Regex("""(?i)(?:paid\s+to|sent\s+to|debited\s+from|transferred\s+to|spent\s+on|spent\s+at)\s*(?:rs\.?|inr|₹)?\s*([0-9]{1,3}(?:,[0-9]{2,3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)"""), false, 1, -1)
    )

    private val amountRegex = Regex(
        """(?i)(?:(?:rs\.?|inr|₹)\s*|debited\s+(?:by|with)?\s*(?:rs\.?|inr|₹)?\s*|paid\s+(?:rs\.?|inr|₹)?\s*|spent\s+(?:rs\.?|inr|₹)?\s*|sent\s+(?:rs\.?|inr|₹)?\s*|credited\s+(?:with|by)?\s*(?:rs\.?|inr|₹)?\s*)([0-9]{1,3}(?:,[0-9]{2,3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)"""
    )

    private val merchantRegex = Regex(
        """(?i)(?:paid\s+to|sent\s+to|transfer\s+to|transferred\s+to|debited\s+for|to\s+vpa|to|at)\s+([A-Za-z0-9\s.&'-]{2,35}?)(?:\s+(?:on|ref|txn|via|using|upi|bal|avbl|a/c|ending|\.|\,)|$)"""
    )

    private val senderRegex = Regex(
        """(?i)(?:paid\s+you\s+(?:by|from)?|received\s+from|credited\s+from|from)\s+([A-Za-z0-9\s.&'-]{2,30}?)(?:\s+(?:on|ref|txn|via|using|upi|bal|avbl|a/c|ending|\.|\,)|$)"""
    )

    fun parse(packageName: String, title: String, text: String): Transaction? {
        val rawCombined = "$title $text".trim()
        if (rawCombined.isEmpty()) return null
        val lowerMessage = rawCombined.lowercase(Locale.ROOT)

        // Tier 1: Sender Authenticity (SMS & App Guard)
        val isSmsApp = packageName.contains("sms", ignoreCase = true) || packageName.contains("message", ignoreCase = true) || packageName.contains("mms", ignoreCase = true)
        val isChatApp = packageName.contains("whatsapp", ignoreCase = true) || packageName.contains("telegram", ignoreCase = true) || packageName.contains("instagram", ignoreCase = true)

        if (isSmsApp) {
            // Reject if title is a 10-12 digit mobile number or personal contact name
            if (Regex("""^\+?[0-9]{10,12}$""").matches(title.trim())) {
                return null
            }
        }

        if (isChatApp) {
            // Drop chat app notifications unless they have banking anchors or official markers
            val hasAnchor = bankingAnchors.any { it.containsMatchIn(rawCombined) }
            if (!hasAnchor) return null
        }

        // Tier 3: Promotional & Phishing Exclusion
        if (promoBlacklist.any { lowerMessage.contains(it) }) {
            return null
        }

        if (balanceInquiryKeywords.any { lowerMessage.contains(it) }) {
            val hasMarker = debitCreditMarkers.any { lowerMessage.contains(it) }
            if (!hasMarker) return null
        }

        // Tier 2: Mandatory "Banking Anchor" Requirement (Dual-Lock)
        val hasValidAnchor = bankingAnchors.any { it.containsMatchIn(rawCombined) }
        if (!hasValidAnchor) {
            return null
        }

        var amount: Double? = null
        var type = "DEBIT"
        var rawMerchant = ""

        // Step A — Check credit rules
        for (rule in creditRules) {
            val match = rule.regex.find(rawCombined)
            if (match != null) {
                val amountStr = match.groups[rule.amountGroup]?.value?.replace(",", "")?.trim()
                val parsedAmt = amountStr?.toDoubleOrNull()
                if (parsedAmt != null && parsedAmt > 0.0) {
                    amount = parsedAmt
                    type = "CREDIT"
                    if (rule.merchantGroup != -1) {
                        rawMerchant = match.groups[rule.merchantGroup]?.value?.trim() ?: ""
                    }
                    break
                }
            }
        }

        // Step B — Check debit rules if amount is null
        if (amount == null) {
            for (rule in debitRules) {
                val match = rule.regex.find(rawCombined)
                if (match != null) {
                    val amountStr = match.groups[rule.amountGroup]?.value?.replace(",", "")?.trim()
                    val parsedAmt = amountStr?.toDoubleOrNull()
                    if (parsedAmt != null && parsedAmt > 0.0) {
                        amount = parsedAmt
                        type = "DEBIT"
                        if (rule.merchantGroup != -1) {
                            rawMerchant = match.groups[rule.merchantGroup]?.value?.trim() ?: ""
                        }
                        break
                    }
                }
            }
        }

        if (amount == null) {
            val match = amountRegex.find(rawCombined) ?: return null
            val amountStr = match.groups[1]?.value?.replace(",", "")?.trim() ?: return null
            amount = amountStr.toDoubleOrNull() ?: return null
            if (amount <= 0.0) return null

            val isCreditFallback = Regex("""(?i)\b(?:credited|received|deposited|refund|cashback|paid\s+you)\b""").containsMatchIn(rawCombined)
            type = if (isCreditFallback) "CREDIT" else "DEBIT"
        }

        if (amount <= 0.0) return null

        if (rawMerchant.isEmpty()) {
            if (type == "CREDIT") {
                val senderMatch = senderRegex.find(rawCombined)
                rawMerchant = senderMatch?.groups?.get(1)?.value?.trim() ?: ""
            } else {
                val merchantMatch = merchantRegex.find(rawCombined)
                rawMerchant = merchantMatch?.groups?.get(1)?.value?.trim() ?: ""
            }
        }

        val wordsToClean = listOf("your", "account", "a/c", "vpa", "bank", "the", "ref", "upi", "is", "has", "been")
        if (wordsToClean.any { rawMerchant.equals(it, ignoreCase = true) } || rawMerchant.length < 2) {
            rawMerchant = ""
        }

        val appNameLabel = when {
            packageName.contains("phonepe", true) -> "PhonePe Transfer"
            packageName.contains("paytm", true) -> "Paytm Transfer"
            packageName.contains("google", true) -> "GPay Transfer"
            packageName.contains("amazon", true) -> "Amazon Pay Transfer"
            packageName.contains("cred", true) -> "CRED Payment"
            packageName.contains("bhim", true) -> "BHIM UPI Transfer"
            else -> if (type == "CREDIT") "Received Payment" else "Merchant Payment"
        }

        val merchantName = when {
            rawMerchant.isNotEmpty() -> cleanMerchantName(rawMerchant)
            title.isNotEmpty() && !title.contains("message", ignoreCase = true) && !title.contains("notification", ignoreCase = true) -> cleanMerchantName(title)
            else -> appNameLabel
        }

        val category = autoCategorize(merchantName, rawCombined)

        return Transaction(
            amount = amount,
            type = type,
            merchantName = merchantName,
            category = category,
            timestamp = System.currentTimeMillis(),
            rawMessage = rawCombined
        )
    }

    // Overload for backward compatibility
    fun parse(title: String, text: String): Transaction? {
        return parse("", title, text)
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
            Regex("""\b(salary|dividend|interest|cashback|refund|credited by client|paid you|received from)\b""").containsMatchIn(target) -> "Income"
            Regex("""\b(swiggy|zomato|domino|mcdonald|kfc|starbucks|burger|pizza|cafe|restaurant|dining|blinkit|zepto|instamart|bigbasket|dunzo|bakery|food)\b""").containsMatchIn(target) -> "Food"
            Regex("""\b(uber|ola|rapido|metro|irctc|train|rail|flight|indigo|air india|makemytrip|shell|petrol|fuel|hpcl|bpcl|iocl|fastag|cab|taxi|toll)\b""").containsMatchIn(target) -> "Travel"
            Regex("""\b(electricity|water|gas|recharge|jio|airtel|vi|broadband|bescom|tata play|dth|rent|maintenance|bill|postpaid|prepaid|utility)\b""").containsMatchIn(target) -> "Bills"
            Regex("""\b(amazon|flipkart|myntra|nykaa|ajio|zara|h&m|croma|reliance|apple|store|mall|retail|mart|cloth|supermarket)\b""").containsMatchIn(target) -> "Shopping"
            Regex("""\b(netflix|spotify|prime|youtube|hotstar|sonyliv|zee5|bookmyshow|cinema|pvr|inox|movie|game|steam)\b""").containsMatchIn(target) -> "Entertainment"
            Regex("""\b(apollo|1mg|pharmeasy|medplus|hospital|clinic|doctor|pharmacy|medical|dentist)\b""").containsMatchIn(target) -> "Health"
            else -> "Others"
        }
    }
}
