package com.example.editor.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.common.FileUtils
import com.example.data.local.AppDatabase
import com.example.data.repository.ProjectRepository
import com.example.domain.model.CanvasAspectRatio
import com.example.domain.model.ClipTransform
import com.example.domain.model.ExportSettings
import com.example.domain.model.ImageLayerProperties
import com.example.domain.model.ItemType
import com.example.domain.model.MediaAsset
import com.example.domain.model.MediaType
import com.example.domain.model.Project
import com.example.domain.model.TextLayerProperties
import com.example.domain.model.TimelineItem
import com.example.domain.model.TransitionConfig
import com.example.domain.model.TransitionType
import com.example.editor.export.ExportState
import com.example.editor.export.VideoExporter
import com.example.editor.player.TimelinePlayer
import com.example.editor.undo.UndoRedoManager
import com.example.media.MediaMetadataReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

enum class EditorSheet {
    NONE,
    SPEED,
    VOLUME,
    TRANSFORM,
    CANVAS,
    TEXT_EDITOR,
    TRANSITION,
    EXPORT_CONFIG
}

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProjectRepository(AppDatabase.getInstance(application))
    val timelinePlayer = TimelinePlayer(application, viewModelScope)
    val videoExporter = VideoExporter(application)
    private val undoRedoManager = UndoRedoManager()

    private val _project = MutableStateFlow<Project?>(null)
    val project: StateFlow<Project?> = _project.asStateFlow()

    private val _selectedItemId = MutableStateFlow<String?>(null)
    val selectedItemId: StateFlow<String?> = _selectedItemId.asStateFlow()

    private val _activeSheet = MutableStateFlow(EditorSheet.NONE)
    val activeSheet: StateFlow<EditorSheet> = _activeSheet.asStateFlow()

    private val _autosaveStatus = MutableStateFlow("Saved")
    val autosaveStatus: StateFlow<String> = _autosaveStatus.asStateFlow()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private val _isFullscreen = MutableStateFlow(false)
    val isFullscreen: StateFlow<Boolean> = _isFullscreen.asStateFlow()

    val playheadMs = timelinePlayer.playheadMs
    val isPlaying = timelinePlayer.isPlaying
    val exportState = videoExporter.exportState

    private var autosaveJob: Job? = null

    fun loadProject(projectId: String) {
        viewModelScope.launch {
            val loaded = repository.getProject(projectId)
            if (loaded != null) {
                _project.value = loaded
                timelinePlayer.setProject(loaded)
                // Select first clip by default if available
                _selectedItemId.value = loaded.videoClips.firstOrNull()?.id
                undoRedoManager.clear()
                updateUndoRedoStates()
            }
        }
    }

    fun selectItem(itemId: String?) {
        _selectedItemId.value = itemId
    }

    fun openSheet(sheet: EditorSheet) {
        _activeSheet.value = sheet
    }

    fun closeSheet() {
        _activeSheet.value = EditorSheet.NONE
    }

    fun toggleFullscreen() {
        _isFullscreen.value = !_isFullscreen.value
    }

    fun seekTo(timeMs: Long) {
        timelinePlayer.seekTo(timeMs)
    }

    fun togglePlayPause() {
        timelinePlayer.togglePlayPause()
    }

    private fun mutateProject(recordUndo: Boolean = true, mutator: (Project) -> Project) {
        val current = _project.value ?: return
        if (recordUndo) {
            undoRedoManager.pushState(current)
            updateUndoRedoStates()
        }
        val mutated = mutator(current).copy(updatedAt = System.currentTimeMillis())
        _project.value = mutated
        timelinePlayer.setProject(mutated)
        triggerAutosave(mutated)
    }

    private fun updateUndoRedoStates() {
        _canUndo.value = undoRedoManager.canUndo()
        _canRedo.value = undoRedoManager.canRedo()
    }

    fun undo() {
        val current = _project.value ?: return
        val previous = undoRedoManager.undo(current) ?: return
        _project.value = previous
        timelinePlayer.setProject(previous)
        updateUndoRedoStates()
        triggerAutosave(previous)
    }

    fun redo() {
        val current = _project.value ?: return
        val next = undoRedoManager.redo(current) ?: return
        _project.value = next
        timelinePlayer.setProject(next)
        updateUndoRedoStates()
        triggerAutosave(next)
    }

    private fun triggerAutosave(projectToSave: Project) {
        autosaveJob?.cancel()
        _autosaveStatus.value = "Saving…"
        autosaveJob = viewModelScope.launch {
            delay(500) // Debounce autosave
            repository.saveProject(projectToSave)
            _autosaveStatus.value = "Saved"
        }
    }

    // --- Editing Operations ---

    /**
     * Splits selected video clip at current playhead position into two distinct clips
     */
    fun splitSelectedClip() {
        val current = _project.value ?: return
        val selectedId = _selectedItemId.value ?: return
        val currentPlayhead = playheadMs.value

        val clip = current.items.find { it.id == selectedId && it.type == ItemType.VIDEO } ?: return
        val clipStart = clip.timelineStartMs
        val clipEnd = clipStart + clip.durationMs

        // Check if playhead is strictly inside the clip bounds
        if (currentPlayhead <= clipStart + 100 || currentPlayhead >= clipEnd - 100) return

        mutateProject { proj ->
            val splitTimelineOffset = currentPlayhead - clipStart
            val splitSourceOffset = (splitTimelineOffset * clip.speed).toLong()

            // Clip 1 (First half)
            val clip1 = clip.copy(
                durationMs = splitTimelineOffset,
                sourceDurationMs = splitSourceOffset
            )

            // Clip 2 (Second half)
            val clip2 = clip.copy(
                id = UUID.randomUUID().toString(),
                timelineStartMs = currentPlayhead,
                durationMs = clip.durationMs - splitTimelineOffset,
                sourceStartMs = clip.sourceStartMs + splitSourceOffset,
                sourceDurationMs = clip.sourceDurationMs - splitSourceOffset,
                transition = TransitionConfig() // Reset transition on split
            )

            val updatedItems = proj.items.toMutableList()
            val clipIndex = updatedItems.indexOfFirst { it.id == clip.id }
            if (clipIndex != -1) {
                updatedItems[clipIndex] = clip1
                updatedItems.add(clipIndex + 1, clip2)
            }

            _selectedItemId.value = clip2.id
            proj.copy(items = updatedItems)
        }
    }

    /**
     * Trims source start or end of the selected clip, recalibrating the sequence
     */
    fun trimSelectedClip(newSourceStartMs: Long, newSourceDurationMs: Long) {
        val selectedId = _selectedItemId.value ?: return
        mutateProject { proj ->
            val videoClips = proj.videoClips.toMutableList()
            val index = videoClips.indexOfFirst { it.id == selectedId }
            if (index == -1) return@mutateProject proj

            val oldClip = videoClips[index]
            val safeSourceDuration = newSourceDurationMs.coerceAtLeast(300L)
            val safeSourceStart = newSourceStartMs.coerceAtLeast(0L)
            val newDurationMs = (safeSourceDuration / oldClip.speed).toLong()

            val updatedClip = oldClip.copy(
                sourceStartMs = safeSourceStart,
                sourceDurationMs = safeSourceDuration,
                durationMs = newDurationMs
            )
            videoClips[index] = updatedClip

            // Re-sequence subsequent clips sequentially
            var currentTimeline = 0L
            val resequenced = videoClips.map { item ->
                val shifted = item.copy(timelineStartMs = currentTimeline)
                currentTimeline += shifted.durationMs
                shifted
            }

            val nonVideoItems = proj.items.filter { it.type != ItemType.VIDEO }
            proj.copy(items = resequenced + nonVideoItems)
        }
    }

    /**
     * Deletes the currently selected timeline item
     */
    fun deleteSelectedItem() {
        val selectedId = _selectedItemId.value ?: return
        mutateProject { proj ->
            val remainingItems = proj.items.filterNot { it.id == selectedId }
            val videoClips = remainingItems.filter { it.type == ItemType.VIDEO }.sortedBy { it.timelineStartMs }
            var currentTimeline = 0L
            val resequencedVideo = videoClips.map { item ->
                val shifted = item.copy(timelineStartMs = currentTimeline)
                currentTimeline += shifted.durationMs
                shifted
            }

            val otherItems = remainingItems.filter { it.type != ItemType.VIDEO }
            _selectedItemId.value = resequencedVideo.firstOrNull()?.id ?: otherItems.firstOrNull()?.id
            proj.copy(items = resequencedVideo + otherItems)
        }
    }

    /**
     * Duplicates the selected item and inserts right after it
     */
    fun duplicateSelectedItem() {
        val selectedId = _selectedItemId.value ?: return
        mutateProject { proj ->
            val item = proj.items.find { it.id == selectedId } ?: return@mutateProject proj
            val newItem = item.copy(id = UUID.randomUUID().toString())

            if (item.type == ItemType.VIDEO) {
                val videoClips = proj.videoClips.toMutableList()
                val index = videoClips.indexOfFirst { it.id == selectedId }
                if (index != -1) {
                    videoClips.add(index + 1, newItem)
                } else {
                    videoClips.add(newItem)
                }

                var currentTimeline = 0L
                val resequenced = videoClips.map { clip ->
                    val shifted = clip.copy(timelineStartMs = currentTimeline)
                    currentTimeline += shifted.durationMs
                    shifted
                }
                _selectedItemId.value = newItem.id
                val otherItems = proj.items.filter { it.type != ItemType.VIDEO }
                proj.copy(items = resequenced + otherItems)
            } else {
                // Overlay or Text duplicate
                val updated = proj.items.toMutableList()
                updated.add(newItem.copy(timelineStartMs = item.timelineStartMs + 500L))
                _selectedItemId.value = newItem.id
                proj.copy(items = updated)
            }
        }
    }

    /**
     * Reorders video clips (moving clip left or right in sequence)
     */
    fun reorderClip(fromIndex: Int, toIndex: Int) {
        mutateProject { proj ->
            val videoClips = proj.videoClips.toMutableList()
            if (fromIndex !in videoClips.indices || toIndex !in videoClips.indices) return@mutateProject proj

            val moved = videoClips.removeAt(fromIndex)
            videoClips.add(toIndex, moved)

            var currentTimeline = 0L
            val resequenced = videoClips.map { clip ->
                val shifted = clip.copy(timelineStartMs = currentTimeline)
                currentTimeline += shifted.durationMs
                shifted
            }

            val otherItems = proj.items.filter { it.type != ItemType.VIDEO }
            proj.copy(items = resequenced + otherItems)
        }
    }

    /**
     * Adjusts playback speed of selected clip and updates duration
     */
    fun updateClipSpeed(speed: Float) {
        val selectedId = _selectedItemId.value ?: return
        mutateProject { proj ->
            val videoClips = proj.videoClips.toMutableList()
            val index = videoClips.indexOfFirst { it.id == selectedId }
            if (index == -1) return@mutateProject proj

            val clip = videoClips[index]
            val safeSpeed = speed.coerceIn(0.2f, 3.0f)
            val newDuration = (clip.sourceDurationMs / safeSpeed).toLong()

            videoClips[index] = clip.copy(speed = safeSpeed, durationMs = newDuration)

            var currentTimeline = 0L
            val resequenced = videoClips.map { item ->
                val shifted = item.copy(timelineStartMs = currentTimeline)
                currentTimeline += shifted.durationMs
                shifted
            }

            val otherItems = proj.items.filter { it.type != ItemType.VIDEO }
            proj.copy(items = resequenced + otherItems)
        }
    }

    /**
     * Updates clip audio volume and mute toggle
     */
    fun updateClipVolume(volume: Float, isMuted: Boolean) {
        val selectedId = _selectedItemId.value ?: return
        mutateProject { proj ->
            val updatedItems = proj.items.map { item ->
                if (item.id == selectedId) {
                    item.copy(volume = volume.coerceIn(0f, 2f), isMuted = isMuted)
                } else item
            }
            proj.copy(items = updatedItems)
        }
    }

    /**
     * Updates visual transform (rotation, flip, scale, offset, crop)
     */
    fun updateClipTransform(transform: ClipTransform) {
        val selectedId = _selectedItemId.value ?: return
        mutateProject { proj ->
            val updatedItems = proj.items.map { item ->
                if (item.id == selectedId) {
                    item.copy(transform = transform)
                } else item
            }
            proj.copy(items = updatedItems)
        }
    }

    /**
     * Sets Canvas Aspect Ratio (9:16, 16:9, 1:1, 4:5, 3:4, Original)
     */
    fun setCanvasAspectRatio(ratio: CanvasAspectRatio) {
        mutateProject { proj ->
            proj.copy(canvasRatio = ratio)
        }
    }

    /**
     * Adds a text layer at current playhead position
     */
    fun addTextLayer(text: String, colorHex: String, bgHex: String?, fontSize: Float) {
        mutateProject { proj ->
            val currentPlayhead = playheadMs.value
            val newItem = TimelineItem(
                id = UUID.randomUUID().toString(),
                trackId = "track_text",
                type = ItemType.TEXT,
                timelineStartMs = currentPlayhead,
                durationMs = 3000L,
                textProperties = TextLayerProperties(
                    text = text,
                    fontSizeSp = fontSize,
                    colorHex = colorHex,
                    backgroundColorHex = bgHex
                )
            )
            _selectedItemId.value = newItem.id
            proj.copy(items = proj.items + newItem)
        }
    }

    /**
     * Updates text layer properties
     */
    fun updateTextProperties(properties: TextLayerProperties) {
        val selectedId = _selectedItemId.value ?: return
        mutateProject { proj ->
            val updatedItems = proj.items.map { item ->
                if (item.id == selectedId && item.type == ItemType.TEXT) {
                    item.copy(textProperties = properties)
                } else item
            }
            proj.copy(items = updatedItems)
        }
    }

    /**
     * Adds an image overlay at current playhead
     */
    fun addImageOverlay(rawUri: Uri) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val localUri = FileUtils.cacheMediaLocally(context, rawUri, prefix = "overlay")
            val asset = MediaMetadataReader.analyzeMedia(context, localUri)

            mutateProject { proj ->
                val currentPlayhead = playheadMs.value
                val newItem = TimelineItem(
                    id = UUID.randomUUID().toString(),
                    trackId = "track_overlay",
                    assetId = asset.id,
                    type = ItemType.IMAGE,
                    timelineStartMs = currentPlayhead,
                    durationMs = 3000L,
                    imageProperties = ImageLayerProperties()
                )
                _selectedItemId.value = newItem.id
                proj.copy(
                    assets = proj.assets + asset,
                    items = proj.items + newItem
                )
            }
        }
    }

    /**
     * Updates image overlay properties
     */
    fun updateImageProperties(properties: ImageLayerProperties) {
        val selectedId = _selectedItemId.value ?: return
        mutateProject { proj ->
            val updatedItems = proj.items.map { item ->
                if (item.id == selectedId && item.type == ItemType.IMAGE) {
                    item.copy(imageProperties = properties)
                } else item
            }
            proj.copy(items = updatedItems)
        }
    }

    /**
     * Sets transition on selected clip
     */
    fun setClipTransition(transition: TransitionConfig) {
        val selectedId = _selectedItemId.value ?: return
        mutateProject { proj ->
            val updatedItems = proj.items.map { item ->
                if (item.id == selectedId && item.type == ItemType.VIDEO) {
                    item.copy(transition = transition)
                } else item
            }
            proj.copy(items = updatedItems)
        }
    }

    /**
     * Imports additional media clips to the current project timeline
     */
    fun addMediaClips(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val context = getApplication<Application>()
            val newAssets = mutableListOf<MediaAsset>()
            val newItems = mutableListOf<TimelineItem>()

            for ((index, rawUri) in uris.withIndex()) {
                val localUri = FileUtils.cacheMediaLocally(context, rawUri, prefix = "add_${index}")
                val asset = MediaMetadataReader.analyzeMedia(context, localUri)
                newAssets.add(asset)
            }

            mutateProject { proj ->
                var currentTimeline = proj.totalDurationMs
                for (asset in newAssets) {
                    val item = TimelineItem(
                        id = UUID.randomUUID().toString(),
                        trackId = if (asset.mediaType == MediaType.AUDIO) "track_audio" else "track_main_video",
                        assetId = asset.id,
                        type = when (asset.mediaType) {
                            MediaType.AUDIO -> ItemType.AUDIO
                            MediaType.IMAGE -> ItemType.IMAGE
                            MediaType.VIDEO -> ItemType.VIDEO
                        },
                        timelineStartMs = currentTimeline,
                        durationMs = asset.durationMs,
                        sourceStartMs = 0L,
                        sourceDurationMs = asset.durationMs
                    )
                    newItems.add(item)
                    currentTimeline += asset.durationMs
                }
                proj.copy(
                    assets = proj.assets + newAssets,
                    items = proj.items + newItems
                )
            }
        }
    }

    // --- Export ---

    fun startExport(settings: ExportSettings) {
        val current = _project.value ?: return
        _activeSheet.value = EditorSheet.NONE
        viewModelScope.launch {
            val updated = current.copy(exportSettings = settings)
            _project.value = updated
            videoExporter.exportProject(updated)
        }
    }

    fun cancelExport() {
        videoExporter.cancelExport()
    }

    fun resetExportState() {
        videoExporter.resetState()
    }

    override fun onCleared() {
        super.onCleared()
        timelinePlayer.release()
    }
}
