package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "media_assets",
    indices = [Index(value = ["projectId"])]
)
data class MediaAssetEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val uriString: String,
    val fileName: String,
    val mediaType: String,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val rotationDegrees: Int,
    val fileSizeBytes: Long,
    val thumbnailPath: String?
)
