package com.example.common

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object FileUtils {

    fun getFileName(context: Context, uri: Uri): String {
        var name = "media_${System.currentTimeMillis()}"
        try {
            if (uri.scheme == "content") {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        val displayName = cursor.getString(nameIndex)
                        if (!displayName.isNullOrBlank()) {
                            name = displayName
                        }
                    }
                }
            } else if (uri.scheme == "file") {
                uri.lastPathSegment?.let { name = it }
            }
        } catch (_: Exception) {
            // Graceful fallback
        }
        return name
    }

    fun getFileSize(context: Context, uri: Uri): Long {
        var size = 0L
        try {
            if (uri.scheme == "content") {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1 && cursor.moveToFirst()) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
            } else if (uri.scheme == "file") {
                val file = File(uri.path ?: "")
                if (file.exists()) size = file.length()
            }
        } catch (_: Exception) {
            // Graceful fallback
        }
        return size
    }

    /**
     * Copies a content URI into the app's persistent internal media directory to ensure uninterrupted
     * non-destructive access, random-access seeking in MediaExtractor/ExoPlayer, and survive URI permission expirations.
     */
    fun cacheMediaLocally(context: Context, uri: Uri, prefix: String = "asset"): Uri {
        return try {
            val fileName = getFileName(context, uri)
            val extension = fileName.substringAfterLast('.', "")
            val safeExtension = if (extension.isNotBlank()) ".$extension" else ".mp4"
            val localDir = File(context.filesDir, "imported_media").apply { mkdirs() }
            val uniqueFile = File(localDir, "${prefix}_${UUID.randomUUID()}$safeExtension")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(uniqueFile).use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(uniqueFile)
        } catch (e: Exception) {
            e.printStackTrace()
            uri
        }
    }

    /**
     * Creates a destination file for exported videos
     */
    fun createExportFile(context: Context, projectName: String = "VistaraVideo"): File {
        val moviesDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) 
            ?: File(context.filesDir, "exports").apply { mkdirs() }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val sanitizedName = projectName.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        return File(moviesDir, "VISTARA_${sanitizedName}_$timeStamp.mp4")
    }

    /**
     * Resolves FileProvider URI for external playback and sharing
     */
    fun getShareableUri(context: Context, file: File): Uri {
        return try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (_: Exception) {
            Uri.fromFile(file)
        }
    }
}
