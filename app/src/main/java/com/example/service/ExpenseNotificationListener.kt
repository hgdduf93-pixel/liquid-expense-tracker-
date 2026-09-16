package com.example.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.data.AppDatabase
import com.example.util.NotificationParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ExpenseNotificationListener : NotificationListenerService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    companion object {
        private const val TAG = "ExpenseListener"

        // Known Indian UPI and Banking Apps
        private val TARGET_PACKAGES = setOf(
            "com.phonepe.app",
            "net.one97.paytm",
            "com.google.android.apps.nbu.paisa.user",
            "in.org.npci.upiapp",
            "in.amazon.mShop.android.shopping",
            "com.dreamplug.androidapp",
            "com.naviapp",
            "tech.super",
            "money.jupiter",
            "co.fi.money",
            "com.mobikwik_new",
            "com.freecharge.android",
            "indwin.c3.shareKaro",
            "com.fampay.in",
            "com.bharatpe.app",
            "com.bharatpe.merchant",
            "com.whatsapp",
            // SMS Apps
            "com.google.android.apps.messaging",
            "com.samsung.android.messaging",
            "com.android.mms",
            "com.android.messaging"
        )

        private val BANK_KEYWORDS = listOf(
            "debited", "credited", "spent", "paid", "sent", "received",
            "inr", "rs.", "rs ", "₹", "upi", "vpa", "a/c", "account",
            "hdfc", "sbi", "icici", "axis", "kotak", "pnb", "bob", "indusind", "yes bank"
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null || sbn.isOngoing) return

        try {
            val packageName = sbn.packageName ?: ""
            
            // Step A: Extract full text with deliberate spaces to prevent boundary collisions
            val extras = sbn.notification?.extras ?: return
            val title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString() ?: ""
            val text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString() ?: ""
            val bigText = extras.getCharSequence(android.app.Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
            val subText = extras.getCharSequence(android.app.Notification.EXTRA_SUB_TEXT)?.toString() ?: ""

            val fullText = "$title$text $bigText$subText".replace("\n", " ").replace("\\s+".toRegex(), " ").trim()
            val lower = fullText.lowercase()

            var finalType: String? = null
            var finalAmount: Double? = null
            var finalMerchant: String = "UPI Payment"

            // STEP 1: STRICT DEBIT EVALUATION (HIGHEST PRIORITY - TERMINATING CHECK)
            val isDebitAlert = lower.contains("debited") ||
                               lower.contains("paid to") ||
                               lower.contains("sent to") ||
                               lower.contains("spent") ||
                               lower.contains("deducted") ||
                               lower.contains("payment to") ||
                               (lower.contains("paid") && !lower.contains("paid you"))

            if (isDebitAlert) {
                finalType = "DEBIT"

                // 1A. Extract Amount (handles Rs., INR, ₹, with optional decimals)
                val amountRegex = """(?:rs\.?|inr|₹)\s*([0-9,]+(?:\.[0-9]{1,2})?)""".toRegex(RegexOption.IGNORE_CASE)
                amountRegex.find(fullText)?.let {
                    finalAmount = it.groupValues[1].replace(",", "").toDoubleOrNull()
                }

                // 1B. Extract Real Counterparty (skip Bank SMS headers like JM-BOIIND-S)
                val recipientRegex = """(?:credited\s+to|paid\s+to|sent\s+to|to\s+vpa|to)\s+([A-Za-z0-9\s._'-]+?)(?:\s+via|\s+on|\s+ref|\s+upi|\.|\,|$)""".toRegex(RegexOption.IGNORE_CASE)
                recipientRegex.find(fullText)?.let {
                    val extracted = it.groupValues[1].trim()
                    if (extracted.length in 2..35 && !extracted.contains("account", ignoreCase = true) && !extracted.contains("a/c", ignoreCase = true)) {
                        finalMerchant = extracted
                    }
                }
                
                // If merchant wasn't found in text and title isn't a bank code, use title
                if (finalMerchant == "UPI Payment" && title.isNotBlank() && !title.matches("""^[A-Z]{2}-[A-Z0-9]+.*""".toRegex())) {
                    finalMerchant = title.trim()
                }
            } 
            // STEP 2: CREDIT EVALUATION (STRICTLY MUTUALLY EXCLUSIVE - ONLY RUNS IF NOT DEBIT)
            else {
                val isCreditAlert = lower.contains("credited to your") ||
                                    lower.contains("credited to a/c") ||
                                    lower.contains("credited to acct") ||
                                    lower.contains("deposited in") ||
                                    lower.contains("paid you") ||
                                    lower.contains("received ₹") ||
                                    lower.contains("received rs") ||
                                    (lower.contains("received") && lower.contains("from"))

                if (isCreditAlert) {
                    finalType = "CREDIT"

                    // 2A. Extract Credit Amount
                    val amountRegex = """(?:rs\.?|inr|₹)\s*([0-9,]+(?:\.[0-9]{1,2})?)""".toRegex(RegexOption.IGNORE_CASE)
                    amountRegex.find(fullText)?.let {
                        finalAmount = it.groupValues[1].replace(",", "").toDoubleOrNull()
                    }

                    // 2B. Extract Sender Name
                    val senderRegex = """(?:from|paid\s+you)\s+([A-Za-z0-9\s._'-]+?)(?:\s+via|\s+on|\s+ref|\.|\,|$)""".toRegex(RegexOption.IGNORE_CASE)
                    senderRegex.find(fullText)?.let {
                        val extracted = it.groupValues[1].trim()
                        if (extracted.length in 2..35) {
                            finalMerchant = extracted
                        }
                    }
                    
                    if (finalMerchant == "UPI Payment" && title.isNotBlank()) {
                        finalMerchant = title.replace("Tap to view", "").trim()
                    }
                }
            }

            val transaction = if (finalType != null && finalAmount != null) {
                com.example.data.Transaction(
                    amount = finalAmount!!,
                    type = finalType!!,
                    merchantName = finalMerchant,
                    category = "Others",
                    timestamp = System.currentTimeMillis(),
                    rawMessage = fullText
                )
            } else null
            
            if (transaction != null) {
                Log.d(TAG, "Parsed Transaction: Amount=${transaction.amount}, Merchant=${transaction.merchantName}, Category=${transaction.category}, Type=${transaction.type}")
                scope.launch {
                    val db = AppDatabase.getInstance(applicationContext)
                    val dao = db.transactionDao()
                    val windowStartTime = System.currentTimeMillis() - (15 * 60 * 1000L)
                    val existingMatch = dao.findDuplicateCandidate(transaction.type, transaction.amount, windowStartTime)

                    if (existingMatch != null) {
                        val isIncomingRicher = (transaction.merchantName != "Merchant Payment" && transaction.merchantName != "Received Payment") &&
                                (existingMatch.merchantName == "Merchant Payment" || existingMatch.merchantName == "Received Payment")
                        if (isIncomingRicher) {
                            val updated = existingMatch.copy(
                                merchantName = transaction.merchantName,
                                category = transaction.category,
                                rawMessage = "${existingMatch.rawMessage} | ${transaction.rawMessage}"
                            )
                            dao.insert(updated)
                            Log.d(TAG, "Updated existing transaction with richer metadata (Scenario B): ID=${updated.id}")
                        } else {
                            val updated = existingMatch.copy(
                                rawMessage = "${existingMatch.rawMessage} | ${transaction.rawMessage}"
                            )
                            dao.insert(updated)
                            Log.d(TAG, "Duplicate transaction detected (Scenario A) within 15 mins (Amount: ${transaction.amount}). Dropped duplicate and enriched rawMessage.")
                        }
                    } else {
                        dao.insert(transaction)
                        Log.d(TAG, "Successfully inserted new transaction into Room Database")
                    }
                }
            }
            // End of replaced logic

        } catch (e: Exception) {
            Log.e(TAG, "Error handling notification", e)
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            requestRebind(android.content.ComponentName(this, ExpenseNotificationListener::class.java))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
