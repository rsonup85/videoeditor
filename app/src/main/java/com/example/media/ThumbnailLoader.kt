package com.example.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object ThumbnailLoader {

    // Cache up to 60MB of bitmap thumbnails in memory
    private val memoryCache: LruCache<String, Bitmap> by lazy {
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize = (maxMemory / 8).coerceAtMost(60 * 1024) // 60MB
        object : LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                return bitmap.byteCount / 1024
            }
        }
    }

    suspend fun getFrameThumbnail(
        context: Context,
        uriString: String,
        timeMs: Long,
        targetWidth: Int = 160,
        targetHeight: Int = 90
    ): Bitmap? = withContext(Dispatchers.IO) {
        val cacheKey = "${uriString}_${timeMs / 1000}_${targetWidth}x$targetHeight"
        memoryCache.get(cacheKey)?.let { return@withContext it }

        val uri = Uri.parse(uriString)
        var bitmap: Bitmap? = null

        // If image uri, decode bitmap directly
        val mimeType = context.contentResolver.getType(uri) ?: ""
        if (mimeType.startsWith("image") || uriString.endsWith(".jpg", true) || uriString.endsWith(".png", true)) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val original = BitmapFactory.decodeStream(stream)
                    if (original != null) {
                        bitmap = Bitmap.createScaledBitmap(original, targetWidth, targetHeight, true)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            // Video: extract frame using MediaMetadataRetriever
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val timeUs = timeMs * 1000L
                val frame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.getFrameAtTime(timeUs)
                if (frame != null) {
                    bitmap = Bitmap.createScaledBitmap(frame, targetWidth, targetHeight, true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) {}
            }
        }

        if (bitmap != null) {
            memoryCache.put(cacheKey, bitmap)
        }
        bitmap
    }

    suspend fun saveProjectCover(context: Context, projectId: String, bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val coversDir = File(context.filesDir, "project_covers").apply { mkdirs() }
        val coverFile = File(coversDir, "${projectId}_cover.jpg")
        FileOutputStream(coverFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        coverFile.absolutePath
    }
}
