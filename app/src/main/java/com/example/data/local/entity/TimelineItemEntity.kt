package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "timeline_items",
    indices = [Index(value = ["projectId"])]
)
data class TimelineItemEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val trackId: String,
    val assetId: String,
    val name: String = "",
    val type: String,
    val timelineStartMs: Long,
    val durationMs: Long,
    val sourceStartMs: Long,
    val sourceDurationMs: Long,
    val speed: Float,
    val volume: Float,
    val isMuted: Boolean,
    val fadeInMs: Long,
    val fadeOutMs: Long,
    // Transform
    val rotationDegrees: Int,
    val flipHorizontal: Boolean,
    val flipVertical: Boolean,
    val scale: Float,
    val offsetX: Float,
    val offsetY: Float,
    val cropLeft: Float,
    val cropTop: Float,
    val cropRight: Float,
    val cropBottom: Float,
    val opacity: Float = 1.0f,
    // Transition
    val transitionType: String,
    val transitionDurationMs: Long,
    val fromClipId: String = "",
    val toClipId: String = "",
    // Text Properties
    val text: String?,
    val textFontFamily: String?,
    val textFontSizeSp: Float?,
    val textColorHex: String?,
    val textBackgroundColorHex: String?,
    val textStrokeColorHex: String?,
    val textStrokeWidth: Float?,
    val textHasShadow: Boolean?,
    val textAlignment: String?,
    val textPositionX: Float?,
    val textPositionY: Float?,
    val textScale: Float?,
    val textRotationDegrees: Float?,
    val textOpacity: Float?,
    // Image Properties
    val imagePositionX: Float?,
    val imagePositionY: Float?,
    val imageScale: Float?,
    val imageRotationDegrees: Float?,
    val imageOpacity: Float?
)
