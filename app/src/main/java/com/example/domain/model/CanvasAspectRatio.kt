package com.example.domain.model

enum class CanvasAspectRatio(val label: String, val ratio: Float?, val description: String) {
    ORIGINAL("Original", null, "Fit source media"),
    RATIO_9_16("9:16", 9f / 16f, "TikTok, Reels, Shorts"),
    RATIO_16_9("16:9", 16f / 9f, "YouTube, Landscape"),
    RATIO_1_1("1:1", 1f, "Square, Instagram Feed"),
    RATIO_4_5("4:5", 4f / 5f, "Portrait Feed"),
    RATIO_3_4("3:4", 3f / 4f, "Classic Portrait");

    companion object {
        fun fromName(name: String?): CanvasAspectRatio {
            return entries.firstOrNull { it.name == name } ?: RATIO_9_16
        }
    }
}
