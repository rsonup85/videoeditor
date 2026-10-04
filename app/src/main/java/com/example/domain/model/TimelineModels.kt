package com.example.domain.model

enum class ItemType {
    VIDEO,
    AUDIO,
    IMAGE,
    TEXT
}

enum class TransitionType(val label: String) {
    NONE("None"),
    FADE("Fade to Black"),
    CROSS_DISSOLVE("Cross Dissolve")
}

data class TransitionConfig(
    val type: TransitionType = TransitionType.NONE,
    val durationMs: Long = 500L
)

data class ClipTransform(
    val rotationDegrees: Int = 0, // 0, 90, 180, 270
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val scale: Float = 1.0f,
    val offsetX: Float = 0.0f,
    val offsetY: Float = 0.0f,
    val cropLeft: Float = 0.0f,
    val cropTop: Float = 0.0f,
    val cropRight: Float = 0.0f,
    val cropBottom: Float = 0.0f
)

data class TextLayerProperties(
    val text: String = "Title",
    val fontSizeSp: Float = 24f,
    val colorHex: String = "#FFFFFF",
    val backgroundColorHex: String? = null,
    val alignment: String = "CENTER", // "LEFT", "CENTER", "RIGHT"
    val positionX: Float = 0.5f, // Normalized 0..1
    val positionY: Float = 0.5f, // Normalized 0..1
    val scale: Float = 1.0f,
    val rotationDegrees: Float = 0.0f,
    val opacity: Float = 1.0f
)

data class ImageLayerProperties(
    val positionX: Float = 0.5f,
    val positionY: Float = 0.5f,
    val scale: Float = 1.0f,
    val rotationDegrees: Float = 0.0f,
    val opacity: Float = 1.0f
)

data class TimelineItem(
    val id: String,
    val trackId: String,
    val assetId: String = "",
    val type: ItemType = ItemType.VIDEO,
    val timelineStartMs: Long = 0L,
    val durationMs: Long = 3000L,
    val sourceStartMs: Long = 0L,
    val sourceDurationMs: Long = 3000L,
    val speed: Float = 1.0f,
    val volume: Float = 1.0f,
    val isMuted: Boolean = false,
    val fadeInMs: Long = 0L,
    val fadeOutMs: Long = 0L,
    val transform: ClipTransform = ClipTransform(),
    val transition: TransitionConfig = TransitionConfig(),
    val textProperties: TextLayerProperties? = null,
    val imageProperties: ImageLayerProperties? = null
)
