package com.example.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.util.NotificationParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ExpenseNotificationListener : NotificationListenerService() {

    private lateinit var db: AppDatabase
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "funds-db").build()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        // List of UPI/Bank apps
        val bankingApps = listOf("com.phonepe.app", "com.google.android.apps.nbu.paisa.user", "net.one97.paytm")
        
        if (packageName in bankingApps) {
            val title = sbn.notification.extras.getString("android.title") ?: ""
            val text = sbn.notification.extras.getString("android.text") ?: ""
            
            val transaction = NotificationParser.parse(title, text)
            if (transaction != null) {
                scope.launch {
                    db.transactionDao().insert(transaction)
                }
            }
        }
    }
}
