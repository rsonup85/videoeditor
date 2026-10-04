package com.example.domain.model

data class Project(
    val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val canvasRatio: CanvasAspectRatio = CanvasAspectRatio.RATIO_9_16,
    val exportSettings: ExportSettings = ExportSettings(),
    val tracks: List<Track> = emptyList(),
    val items: List<TimelineItem> = emptyList(),
    val assets: List<MediaAsset> = emptyList()
) {
    val totalDurationMs: Long
        get() {
            val videoItems = items.filter { it.type == ItemType.VIDEO }
            if (videoItems.isEmpty()) {
                return items.maxOfOrNull { it.timelineStartMs + it.durationMs } ?: 0L
            }
            return videoItems.maxOfOrNull { it.timelineStartMs + it.durationMs } ?: 0L
        }

    val videoClips: List<TimelineItem>
        get() = items.filter { it.type == ItemType.VIDEO }.sortedBy { it.timelineStartMs }

    val audioClips: List<TimelineItem>
        get() = items.filter { it.type == ItemType.AUDIO }.sortedBy { it.timelineStartMs }

    val textLayers: List<TimelineItem>
        get() = items.filter { it.type == ItemType.TEXT }.sortedBy { it.timelineStartMs }

    val imageLayers: List<TimelineItem>
        get() = items.filter { it.type == ItemType.IMAGE }.sortedBy { it.timelineStartMs }
}
