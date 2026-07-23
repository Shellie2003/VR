package com.example.util

import android.util.Base64
import android.util.Log
import java.io.File

/**
 * Embeds locally-stored product photos (taken via camera or picked from the gallery, saved as
 * "file://..." under the app's private storage) directly as base64 inside the JSON backup blob,
 * so they survive a fresh install / app data wipe / device change exactly like the rest of the
 * data — without needing any paid image hosting. Remote "http(s)://" images (e.g. from Open Food
 * Facts) are already durably hosted elsewhere and are left untouched.
 *
 * Product photos are downsampled to a modest resolution before being saved locally (see
 * AddProductScreen.saveBitmapToLocalFile/saveUriToLocalFile), which keeps this comfortably small
 * even for a large catalog and well within Firebase Realtime Database's free tier.
 */
object ImageBackupUtil {
    private const val LOCAL_FILE_PREFIX = "file://"

    fun isLocalImage(imageUrl: String?): Boolean =
        !imageUrl.isNullOrBlank() && imageUrl.startsWith(LOCAL_FILE_PREFIX)

    /** Returns the base64-encoded bytes of a local product image, or null if remote/missing. */
    fun encodeLocalImage(imageUrl: String?): String? {
        if (!isLocalImage(imageUrl)) return null
        return try {
            val file = File(imageUrl!!.removePrefix(LOCAL_FILE_PREFIX))
            if (!file.exists()) return null
            Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e("ImageBackupUtil", "Failed to encode image for backup: $imageUrl", e)
            null
        }
    }

    /** Recreates a local product image file from its backed-up base64 data, if it's missing. */
    fun restoreLocalImageIfMissing(imageUrl: String?, base64Data: String?) {
        if (!isLocalImage(imageUrl) || base64Data.isNullOrBlank()) return
        try {
            val file = File(imageUrl!!.removePrefix(LOCAL_FILE_PREFIX))
            if (file.exists()) return
            file.parentFile?.mkdirs()
            file.writeBytes(Base64.decode(base64Data, Base64.NO_WRAP))
        } catch (e: Exception) {
            Log.e("ImageBackupUtil", "Failed to restore image from backup: $imageUrl", e)
        }
    }
}
