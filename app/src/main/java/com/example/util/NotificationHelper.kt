package com.example.util

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.data.model.Product

object NotificationHelper {
    private const val CHANNEL_ID = "low_stock_alerts"
    private const val CHANNEL_NAME = "Hatairana Tahiry Mihavitsy"
    private const val CHANNEL_DESC = "Mandefa fampandrenesana raha misy entana latsaky ny fetra voafaritra"
    private const val NOTIFICATION_ID_BASE = 1000
    private const val GROUP_KEY_LOW_STOCK = "com.example.LOW_STOCK_GROUP"

    private const val DEBT_CHANNEL_ID = "overdue_debt_alerts"
    private const val DEBT_CHANNEL_NAME = "Fampahatsiahivana Trosa"
    private const val DEBT_CHANNEL_DESC = "Mandefa fampandrenesana raha misy trosa tara fandoavana"
    private const val NOTIFICATION_ID_OVERDUE_DEBTS = 2000

    private const val EXPIRY_CHANNEL_ID = "expiry_alerts"
    private const val EXPIRY_CHANNEL_NAME = "Fampahatsiahivana Fetr'andro Peremptiona"
    private const val EXPIRY_CHANNEL_DESC = "Mandefa fampandrenesana raha misy entana tara peremptiona na akaiky peremptiona"
    private const val NOTIFICATION_ID_EXPIRY_ALERTS = 3000

    // Same alert red used across the app's own low-stock badges (InventoryListScreen, Dettes).
    private val ALERT_RED = Color.parseColor("#D32F2F")

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableLights(true)
                lightColor = ALERT_RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
            }
            val debtChannel = NotificationChannel(DEBT_CHANNEL_ID, DEBT_CHANNEL_NAME, importance).apply {
                description = DEBT_CHANNEL_DESC
                enableLights(true)
                lightColor = ALERT_RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
            }
            val expiryChannel = NotificationChannel(EXPIRY_CHANNEL_ID, EXPIRY_CHANNEL_NAME, importance).apply {
                description = EXPIRY_CHANNEL_DESC
                enableLights(true)
                lightColor = ALERT_RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            notificationManager.createNotificationChannel(debtChannel)
            notificationManager.createNotificationChannel(expiryChannel)
        }
    }

    // C.4: fired at most once per day when at least one lot is expired or expiring within the
    // warning window.
    @SuppressLint("MissingPermission")
    fun showExpiryAlertNotification(context: Context, expiredCount: Int, expiringSoonCount: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, pendingIntentFlags)

        val title = if (expiredCount > 0) {
            "⚠️ $expiredCount lot(s) périmé(s)"
        } else {
            "⏳ $expiringSoonCount lot(s) bientôt périmé(s)"
        }
        val bodyText = when {
            expiredCount > 0 && expiringSoonCount > 0 -> "$expiredCount déjà périmé(s), $expiringSoonCount à surveiller cette semaine"
            expiredCount > 0 -> "À retirer de la vente dès que possible"
            else -> "À vendre en priorité cette semaine"
        }

        val builder = NotificationCompat.Builder(context, EXPIRY_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setColor(ALERT_RED)
            .setColorized(false)
            .setContentTitle(title)
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText).setBigContentTitle(title))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_EXPIRY_ALERTS, builder.build())
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    // C.3: fired at most once per day (throttled by InventoryViewModel via AppPreferences) when at
    // least one debt has a due date in the past and is still unpaid.
    @SuppressLint("MissingPermission")
    fun showOverdueDebtsNotification(context: Context, count: Int, totalAmount: Double) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, pendingIntentFlags)

        val title = "⚠️ $count trosa tara fandoavana"
        val bodyText = "Vola tokony ho voaray: ${FormatUtil.formatPrice(totalAmount)} Ar"

        val builder = NotificationCompat.Builder(context, DEBT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setColor(ALERT_RED)
            .setColorized(false)
            .setContentTitle(title)
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText).setBigContentTitle(title))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_OVERDUE_DEBTS, builder.build())
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    @SuppressLint("MissingPermission")
    fun showLowStockNotification(context: Context, product: Product) {
        // Create an explicit intent for MainActivity
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, pendingIntentFlags)

        val title = "⚠️ Tsy ampy tahiry: ${product.name}"
        val bodyText = "Sisa ${FormatUtil.formatQty(product.stock, product.unit)} (Fetra: ${FormatUtil.formatQty(product.lowStockThreshold, product.unit)})"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setColor(ALERT_RED)
            .setColorized(false)
            .setContentTitle(title)
            .setContentText(bodyText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(bodyText)
                    .setBigContentTitle(title)
                    .setSummaryText(product.category)
            )
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setGroup(GROUP_KEY_LOW_STOCK)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            // Use unique ID for each product notification so they don't overwrite each other
            notificationManager.notify(NOTIFICATION_ID_BASE + product.id, builder.build())

            // Summary notification so multiple low-stock alerts fold into one group instead of
            // flooding the shade, matching how modern Android notification groups behave.
            val summary = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setColor(ALERT_RED)
                .setContentTitle("Tahiry mihavitsy")
                .setStyle(NotificationCompat.InboxStyle().setSummaryText("Tsindrio raha te hijery"))
                .setGroup(GROUP_KEY_LOW_STOCK)
                .setGroupSummary(true)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()
            notificationManager.notify(NOTIFICATION_ID_BASE - 1, summary)
        } catch (e: Throwable) {
            // Handle cases where permission is denied or notification manager fails
            e.printStackTrace()
        }
    }
}
