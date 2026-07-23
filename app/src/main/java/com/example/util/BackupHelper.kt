package com.example.util

import android.content.Context
import java.io.File

object BackupHelper {
    private const val BACKUP_FILE_NAME = "database_safety_backup.json"

    fun saveBackup(context: Context, json: String) {
        try {
            val file = File(context.filesDir, BACKUP_FILE_NAME)
            file.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun readBackup(context: Context): String? {
        return try {
            val file = File(context.filesDir, BACKUP_FILE_NAME)
            if (file.exists()) file.readText() else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun hasBackup(context: Context): Boolean {
        return try {
            val file = File(context.filesDir, BACKUP_FILE_NAME)
            file.exists() && file.length() > 20
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Copies the backup into the app's external files dir (covered by the FileProvider used for
     * sharing/export elsewhere) so it can be sent through the Android share sheet — WhatsApp,
     * Bluetooth, email, USB file transfer, an SD card — for shop owners who don't have a Firebase
     * project or even an email address. Returns null if there's no backup yet.
     */
    fun getShareableBackupFile(context: Context): File? {
        return try {
            val source = File(context.filesDir, BACKUP_FILE_NAME)
            if (!source.exists()) return null
            val exportDir = context.getExternalFilesDir("Exports") ?: return null
            if (!exportDir.exists()) exportDir.mkdirs()
            val dest = File(exportDir, BACKUP_FILE_NAME)
            source.copyTo(dest, overwrite = true)
            dest
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
