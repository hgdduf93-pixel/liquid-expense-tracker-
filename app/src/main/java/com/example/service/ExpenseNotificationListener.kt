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
            "com.amazon.mShop.android.shopping",
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
            val extras = sbn.notification?.extras ?: return

            val title = extras.getCharSequence("android.title")?.toString() ?: ""
            val text = extras.getCharSequence("android.text")?.toString() ?: ""
            val bigText = extras.getCharSequence("android.bigText")?.toString() ?: ""
            val messageContent = if (bigText.isNotBlank()) bigText else text

            val fullText = "$title $messageContent".lowercase()

            val isTargetPackage = packageName in TARGET_PACKAGES ||
                    packageName.contains("sms", ignoreCase = true) ||
                    packageName.contains("mms", ignoreCase = true) ||
                    packageName.contains("message", ignoreCase = true)

            val containsBankKeywords = BANK_KEYWORDS.any { fullText.contains(it) }

            if (isTargetPackage || containsBankKeywords) {
                Log.d(TAG, "Notification matched from $packageName: Title='$title', Text='$messageContent'")
                val transaction = NotificationParser.parse(packageName, title, messageContent)
                if (transaction != null) {
                    Log.d(TAG, "Parsed Transaction: Amount=${transaction.amount}, Merchant=${transaction.merchantName}, Category=${transaction.category}, Type=${transaction.type}")
                    scope.launch {
                        val db = AppDatabase.getInstance(applicationContext)
                        val dao = db.transactionDao()
                        val sinceTime = System.currentTimeMillis() - 90000L
                        val recentTransactions = dao.getRecentTransactions(sinceTime)

                        val existingMatch = recentTransactions.find { kotlin.math.abs(it.amount - transaction.amount) < 0.01 }
                        if (existingMatch != null) {
                            val isIncomingRicher = transaction.merchantName != "Merchant Payment" && existingMatch.merchantName == "Merchant Payment"
                            if (isIncomingRicher) {
                                val updated = existingMatch.copy(
                                    merchantName = transaction.merchantName,
                                    category = transaction.category,
                                    rawMessage = "${existingMatch.rawMessage} | ${transaction.rawMessage}"
                                )
                                dao.insert(updated)
                                Log.d(TAG, "Updated existing transaction with richer metadata: ID=${updated.id}")
                            } else {
                                Log.d(TAG, "Duplicate transaction detected (Amount: ${transaction.amount}), skipping insertion.")
                            }
                        } else {
                            dao.insert(transaction)
                            Log.d(TAG, "Successfully inserted new transaction into Room Database")
                        }
                    }
                }
            }
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
