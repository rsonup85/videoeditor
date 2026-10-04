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
import com.example.domain.model.ItemType
import com.example.domain.model.MediaAsset
import com.example.domain.model.MediaType
import com.example.domain.model.Project
import com.example.domain.model.TimelineItem
import com.example.domain.model.Track
import com.example.domain.model.TrackType
import com.example.media.MediaMetadataReader
import com.example.media.ThumbnailLoader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProjectRepository(AppDatabase.getInstance(application))

    val searchQuery = MutableStateFlow("")

    val projects: StateFlow<List<Project>> = repository.getAllProjects()
        .combine(searchQuery) { projectList, query ->
            if (query.isBlank()) {
                projectList
            } else {
                projectList.filter { it.name.contains(query, ignoreCase = true) }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isCreatingProject = MutableStateFlow(false)
    val isCreatingProject: StateFlow<Boolean> = _isCreatingProject.asStateFlow()

    fun createProjectFromMedia(uris: List<Uri>, onProjectCreated: (String) -> Unit) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _isCreatingProject.value = true
            val context = getApplication<Application>()
            val projectId = UUID.randomUUID().toString()
            val timeStamp = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date())
            val projectName = "Project $timeStamp"

            val assets = mutableListOf<MediaAsset>()
            val timelineItems = mutableListOf<TimelineItem>()
            var currentTimelineStartMs = 0L

            val tracks = listOf(
                Track(id = "track_overlay", type = TrackType.OVERLAY, name = "Overlay", order = 0),
                Track(id = "track_text", type = TrackType.TEXT, name = "Text", order = 1),
                Track(id = "track_main_video", type = TrackType.VIDEO, name = "Main Video", order = 2),
                Track(id = "track_audio", type = TrackType.AUDIO, name = "Audio", order = 3)
            )

            var firstVideoAssetCover: String? = null

            for ((index, rawUri) in uris.withIndex()) {
                // Cache media locally to ensure uninterrupted random-access & persistence
                val localUri = FileUtils.cacheMediaLocally(context, rawUri, prefix = "clip_$index")
                val asset = MediaMetadataReader.analyzeMedia(context, localUri)
                assets.add(asset)

                // Generate project cover from the first video if available
                if (firstVideoAssetCover == null && asset.mediaType == MediaType.VIDEO) {
                    val frame = ThumbnailLoader.getFrameThumbnail(context, asset.uriString, 1000L, 320, 180)
                    if (frame != null) {
                        firstVideoAssetCover = ThumbnailLoader.saveProjectCover(context, projectId, frame)
                    }
                }

                val item = TimelineItem(
                    id = UUID.randomUUID().toString(),
                    trackId = if (asset.mediaType == MediaType.AUDIO) "track_audio" else "track_main_video",
                    assetId = asset.id,
                    type = when (asset.mediaType) {
                        MediaType.AUDIO -> ItemType.AUDIO
                        MediaType.IMAGE -> ItemType.IMAGE
                        MediaType.VIDEO -> ItemType.VIDEO
                    },
                    timelineStartMs = currentTimelineStartMs,
                    durationMs = asset.durationMs,
                    sourceStartMs = 0L,
                    sourceDurationMs = asset.durationMs,
                    transform = ClipTransform()
                )
                timelineItems.add(item)
                currentTimelineStartMs += asset.durationMs
            }

            val newProject = Project(
                id = projectId,
                name = projectName,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                canvasRatio = CanvasAspectRatio.RATIO_9_16,
                exportSettings = ExportSettings(),
                tracks = tracks,
                items = timelineItems,
                assets = assets
            )

            repository.saveProject(newProject)
            _isCreatingProject.value = false
            onProjectCreated(projectId)
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            repository.deleteProject(projectId)
        }
    }

    fun duplicateProject(projectId: String) {
        viewModelScope.launch {
            repository.duplicateProject(projectId)
        }
    }

    fun renameProject(projectId: String, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            repository.renameProject(projectId, newName.trim())
        }
    }
}
