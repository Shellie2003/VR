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
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
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
