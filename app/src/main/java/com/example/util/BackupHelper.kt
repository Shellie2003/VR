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
}
