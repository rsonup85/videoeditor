package com.example.domain.model

enum class TrackType {
    VIDEO,
    AUDIO,
    TEXT,
    OVERLAY
}

data class Track(
    val id: String,
    val type: TrackType,
    val name: String,
    val order: Int = 0,
    val isMuted: Boolean = false,
    val isLocked: Boolean = false,
    val isVisible: Boolean = true
)
