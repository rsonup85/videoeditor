package com.example.common

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
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
     * Creates a temporary working file for the video export pipeline before publishing to MediaStore
     */
    fun createTempExportFile(context: Context, projectName: String = "VistaraVideo"): File {
        val cacheDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val sanitizedName = projectName.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        return File(cacheDir, "temp_export_${sanitizedName}_$timeStamp.mp4")
    }

    /**
     * Publishes the completed export video file to Android MediaStore in Movies/Vistara Edit,
     * ensuring it is immediately visible in Google Photos and device Gallery apps.
     */
    fun saveVideoToGallery(context: Context, sourceFile: File, projectName: String): Uri? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val sanitizedName = projectName.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val displayName = "VISTARA_${sanitizedName}_$timeStamp.mp4"

        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis())

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Vistara Edit")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            } else {
                val legacyDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                    "Vistara Edit"
                ).apply { mkdirs() }
                put(MediaStore.Video.Media.DATA, File(legacyDir, displayName).absolutePath)
            }
        }

        val resolver = context.contentResolver
        val collectionUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val itemUri = resolver.insert(collectionUri, values) ?: return null

        try {
            resolver.openOutputStream(itemUri)?.use { out ->
                FileInputStream(sourceFile).use { input ->
                    input.copyTo(out)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(itemUri, values, null, null)
            }

            // Trigger system media scan so the video appears instantaneously in Gallery
            val path = getPathFromUri(context, itemUri)
            if (path != null) {
                MediaScannerConnection.scanFile(context, arrayOf(path), arrayOf("video/mp4"), null)
            }

            return itemUri
        } catch (e: Exception) {
            e.printStackTrace()
            // Cleanup on failure
            try {
                resolver.delete(itemUri, null, null)
            } catch (_: Exception) {}
            return null
        }
    }

    private fun getPathFromUri(context: Context, uri: Uri): String? {
        if (uri.scheme == "file") return uri.path
        return try {
            context.contentResolver.query(uri, arrayOf(MediaStore.Video.Media.DATA), null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                if (cursor.moveToFirst()) cursor.getString(index) else null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Resolves shareable URI for external playback or sharing
     */
    fun getShareableUri(context: Context, fileOrUri: Any): Uri {
        return when (fileOrUri) {
            is Uri -> fileOrUri
            is File -> {
                try {
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        fileOrUri
                    )
                } catch (_: Exception) {
                    Uri.fromFile(fileOrUri)
                }
            }
            else -> Uri.EMPTY
        }
    }
}
