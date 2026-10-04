package com.example.media

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.example.common.FileUtils
import com.example.domain.model.MediaAsset
import com.example.domain.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

object MediaMetadataReader {

    suspend fun analyzeMedia(context: Context, uri: Uri): MediaAsset = withContext(Dispatchers.IO) {
        val fileName = FileUtils.getFileName(context, uri)
        val fileSizeBytes = FileUtils.getFileSize(context, uri)
        val mimeType = context.contentResolver.getType(uri) ?: ""

        val isImage = mimeType.startsWith("image") || fileName.endsWith(".jpg", true) ||
                fileName.endsWith(".png", true) || fileName.endsWith(".jpeg", true) || fileName.endsWith(".webp", true)
        val isAudio = mimeType.startsWith("audio") || fileName.endsWith(".mp3", true) ||
                fileName.endsWith(".wav", true) || fileName.endsWith(".m4a", true) || fileName.endsWith(".aac", true)

        val determinedType = when {
            isImage -> MediaType.IMAGE
            isAudio -> MediaType.AUDIO
            else -> MediaType.VIDEO
        }

        var durationMs = if (isImage) 3000L else 0L
        var width = 0
        var height = 0
        var rotation = 0

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            if (!durationStr.isNullOrBlank()) {
                durationMs = durationStr.toLongOrNull() ?: durationMs
            }

            val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)

            width = widthStr?.toIntOrNull() ?: 0
            height = heightStr?.toIntOrNull() ?: 0
            rotation = rotationStr?.toIntOrNull() ?: 0

            // If video has 90 or 270 rotation metadata, swap display width/height
            if (rotation == 90 || rotation == 270) {
                val temp = width
                width = height
                height = temp
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }

        // If duration is 0, give minimum fallback safe 3000ms
        if (durationMs <= 0) {
            durationMs = 3000L
        }

        MediaAsset(
            id = UUID.randomUUID().toString(),
            uriString = uri.toString(),
            fileName = fileName,
            mediaType = determinedType,
            durationMs = durationMs,
            width = if (width > 0) width else 1920,
            height = if (height > 0) height else 1080,
            rotationDegrees = rotation,
            fileSizeBytes = fileSizeBytes
        )
    }
}
