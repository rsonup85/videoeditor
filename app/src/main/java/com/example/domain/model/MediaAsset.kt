package com.example.domain.model

enum class MediaType {
    VIDEO,
    IMAGE,
    AUDIO
}

data class MediaAsset(
    val id: String,
    val uriString: String,
    val fileName: String,
    val mediaType: MediaType,
    val durationMs: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val rotationDegrees: Int = 0,
    val fileSizeBytes: Long = 0L,
    val thumbnailPath: String? = null
)
