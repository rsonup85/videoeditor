package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.MediaAssetEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.TimelineItemEntity
import com.example.domain.model.CanvasAspectRatio
import com.example.domain.model.ClipTransform
import com.example.domain.model.ExportQuality
import com.example.domain.model.ExportResolution
import com.example.domain.model.ExportSettings
import com.example.domain.model.ImageLayerProperties
import com.example.domain.model.ItemType
import com.example.domain.model.MediaAsset
import com.example.domain.model.MediaType
import com.example.domain.model.Project
import com.example.domain.model.TextLayerProperties
import com.example.domain.model.TimelineItem
import com.example.domain.model.Track
import com.example.domain.model.TrackType
import com.example.domain.model.TransitionConfig
import com.example.domain.model.TransitionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class ProjectRepository(private val database: AppDatabase) {

    private val projectDao = database.projectDao()
    private val mediaAssetDao = database.mediaAssetDao()
    private val timelineItemDao = database.timelineItemDao()

    fun getAllProjects(): Flow<List<Project>> {
        return projectDao.getAllProjects().map { entities ->
            entities.map { entity ->
                entity.toDomain()
            }
        }
    }

    suspend fun getProject(projectId: String): Project? = withContext(Dispatchers.IO) {
        val projectEntity = projectDao.getProjectById(projectId) ?: return@withContext null
        val assetEntities = mediaAssetDao.getAssetsForProject(projectId)
        val itemEntities = timelineItemDao.getItemsForProject(projectId)

        val assets = assetEntities.map { it.toDomain() }
        val items = itemEntities.map { it.toDomain() }

        val tracks = listOf(
            Track(id = "track_text", type = TrackType.TEXT, name = "TEXT", order = 0),
            Track(id = "track_image", type = TrackType.OVERLAY, name = "IMAGE", order = 1),
            Track(id = "track_video_2", type = TrackType.VIDEO, name = "VIDEO 2", order = 2),
            Track(id = "track_video_1", type = TrackType.VIDEO, name = "VIDEO 1", order = 3),
            Track(id = "track_audio_1", type = TrackType.AUDIO, name = "AUDIO 1", order = 4),
            Track(id = "track_audio_2", type = TrackType.AUDIO, name = "AUDIO 2", order = 5)
        )

        projectEntity.toDomain(assets = assets, items = items, tracks = tracks)
    }

    suspend fun saveProject(project: Project) = withContext(Dispatchers.IO) {
        val projectEntity = project.toEntity()
        val assetEntities = project.assets.map { it.toEntity(project.id) }
        val itemEntities = project.items.map { it.toEntity(project.id) }

        projectDao.insertProject(projectEntity)
        mediaAssetDao.deleteAssetsForProject(project.id)
        if (assetEntities.isNotEmpty()) {
            mediaAssetDao.insertAssets(assetEntities)
        }
        timelineItemDao.deleteItemsForProject(project.id)
        if (itemEntities.isNotEmpty()) {
            timelineItemDao.insertItems(itemEntities)
        }
    }

    suspend fun deleteProject(projectId: String) = withContext(Dispatchers.IO) {
        projectDao.deleteProjectById(projectId)
        mediaAssetDao.deleteAssetsForProject(projectId)
        timelineItemDao.deleteItemsForProject(projectId)
    }

    suspend fun renameProject(projectId: String, newName: String) = withContext(Dispatchers.IO) {
        val existing = projectDao.getProjectById(projectId) ?: return@withContext
        projectDao.updateProject(existing.copy(name = newName, updatedAt = System.currentTimeMillis()))
    }

    suspend fun duplicateProject(projectId: String): Project? = withContext(Dispatchers.IO) {
        val original = getProject(projectId) ?: return@withContext null
        val newId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val cloned = original.copy(
            id = newId,
            name = "${original.name} (Copy)",
            createdAt = now,
            updatedAt = now,
            items = original.items.map { it.copy(id = UUID.randomUUID().toString()) }
        )
        saveProject(cloned)
        cloned
    }

    // --- Entity Mappers ---

    private fun ProjectEntity.toDomain(
        assets: List<MediaAsset> = emptyList(),
        items: List<TimelineItem> = emptyList(),
        tracks: List<Track> = emptyList()
    ): Project {
        return Project(
            id = id,
            name = name,
            createdAt = createdAt,
            updatedAt = updatedAt,
            canvasRatio = CanvasAspectRatio.fromName(canvasRatio),
            canvasBackgroundColorHex = canvasBackgroundColorHex,
            isSnapEnabled = isSnapEnabled,
            exportSettings = ExportSettings(
                resolution = runCatching { ExportResolution.valueOf(exportResolution) }.getOrDefault(ExportResolution.RES_1080P),
                fps = exportFps,
                quality = runCatching { ExportQuality.valueOf(exportQuality) }.getOrDefault(ExportQuality.MEDIUM)
            ),
            tracks = tracks,
            items = items,
            assets = assets
        )
    }

    private fun Project.toEntity(): ProjectEntity {
        return ProjectEntity(
            id = id,
            name = name,
            createdAt = createdAt,
            updatedAt = updatedAt,
            canvasRatio = canvasRatio.name,
            canvasBackgroundColorHex = canvasBackgroundColorHex,
            isSnapEnabled = isSnapEnabled,
            exportResolution = exportSettings.resolution.name,
            exportFps = exportSettings.fps,
            exportQuality = exportSettings.quality.name
        )
    }

    private fun MediaAssetEntity.toDomain(): MediaAsset {
        return MediaAsset(
            id = id,
            uriString = uriString,
            fileName = fileName,
            mediaType = runCatching { MediaType.valueOf(mediaType) }.getOrDefault(MediaType.VIDEO),
            durationMs = durationMs,
            width = width,
            height = height,
            rotationDegrees = rotationDegrees,
            fileSizeBytes = fileSizeBytes,
            thumbnailPath = thumbnailPath
        )
    }

    private fun MediaAsset.toEntity(projectId: String): MediaAssetEntity {
        return MediaAssetEntity(
            id = id,
            projectId = projectId,
            uriString = uriString,
            fileName = fileName,
            mediaType = mediaType.name,
            durationMs = durationMs,
            width = width,
            height = height,
            rotationDegrees = rotationDegrees,
            fileSizeBytes = fileSizeBytes,
            thumbnailPath = thumbnailPath
        )
    }

    private fun TimelineItemEntity.toDomain(): TimelineItem {
        val parsedType = runCatching { ItemType.valueOf(type) }.getOrDefault(ItemType.VIDEO)
        val textProps = if (parsedType == ItemType.TEXT) {
            TextLayerProperties(
                text = text ?: "Text",
                fontFamily = textFontFamily ?: "sans",
                fontSizeSp = textFontSizeSp ?: 24f,
                colorHex = textColorHex ?: "#FFFFFF",
                backgroundColorHex = textBackgroundColorHex,
                strokeColorHex = textStrokeColorHex,
                strokeWidth = textStrokeWidth ?: 0f,
                hasShadow = textHasShadow ?: false,
                alignment = textAlignment ?: "CENTER",
                positionX = textPositionX ?: 0.5f,
                positionY = textPositionY ?: 0.5f,
                scale = textScale ?: 1.0f,
                rotationDegrees = textRotationDegrees ?: 0.0f,
                opacity = textOpacity ?: 1.0f
            )
        } else null

        val imageProps = if (parsedType == ItemType.IMAGE) {
            ImageLayerProperties(
                positionX = imagePositionX ?: 0.5f,
                positionY = imagePositionY ?: 0.5f,
                scale = imageScale ?: 1.0f,
                rotationDegrees = imageRotationDegrees ?: 0.0f,
                opacity = imageOpacity ?: 1.0f
            )
        } else null

        return TimelineItem(
            id = id,
            trackId = trackId,
            assetId = assetId,
            name = name,
            type = parsedType,
            timelineStartMs = timelineStartMs,
            durationMs = durationMs,
            sourceStartMs = sourceStartMs,
            sourceDurationMs = sourceDurationMs,
            speed = speed,
            volume = volume,
            isMuted = isMuted,
            fadeInMs = fadeInMs,
            fadeOutMs = fadeOutMs,
            transform = ClipTransform(
                rotationDegrees = rotationDegrees,
                flipHorizontal = flipHorizontal,
                flipVertical = flipVertical,
                scale = scale,
                offsetX = offsetX,
                offsetY = offsetY,
                cropLeft = cropLeft,
                cropTop = cropTop,
                cropRight = cropRight,
                cropBottom = cropBottom,
                opacity = opacity
            ),
            transition = TransitionConfig(
                type = runCatching { TransitionType.valueOf(transitionType) }.getOrDefault(TransitionType.NONE),
                durationMs = transitionDurationMs,
                fromClipId = fromClipId,
                toClipId = toClipId
            ),
            textProperties = textProps,
            imageProperties = imageProps
        )
    }

    private fun TimelineItem.toEntity(projectId: String): TimelineItemEntity {
        return TimelineItemEntity(
            id = id,
            projectId = projectId,
            trackId = trackId,
            assetId = assetId,
            name = name,
            type = type.name,
            timelineStartMs = timelineStartMs,
            durationMs = durationMs,
            sourceStartMs = sourceStartMs,
            sourceDurationMs = sourceDurationMs,
            speed = speed,
            volume = volume,
            isMuted = isMuted,
            fadeInMs = fadeInMs,
            fadeOutMs = fadeOutMs,
            rotationDegrees = transform.rotationDegrees,
            flipHorizontal = transform.flipHorizontal,
            flipVertical = transform.flipVertical,
            scale = transform.scale,
            offsetX = transform.offsetX,
            offsetY = transform.offsetY,
            cropLeft = transform.cropLeft,
            cropTop = transform.cropTop,
            cropRight = transform.cropRight,
            cropBottom = transform.cropBottom,
            opacity = transform.opacity,
            transitionType = transition.type.name,
            transitionDurationMs = transition.durationMs,
            fromClipId = transition.fromClipId,
            toClipId = transition.toClipId,
            text = textProperties?.text,
            textFontFamily = textProperties?.fontFamily,
            textFontSizeSp = textProperties?.fontSizeSp,
            textColorHex = textProperties?.colorHex,
            textBackgroundColorHex = textProperties?.backgroundColorHex,
            textStrokeColorHex = textProperties?.strokeColorHex,
            textStrokeWidth = textProperties?.strokeWidth,
            textHasShadow = textProperties?.hasShadow,
            textAlignment = textProperties?.alignment,
            textPositionX = textProperties?.positionX,
            textPositionY = textProperties?.positionY,
            textScale = textProperties?.scale,
            textRotationDegrees = textProperties?.rotationDegrees,
            textOpacity = textProperties?.opacity,
            imagePositionX = imageProperties?.positionX,
            imagePositionY = imageProperties?.positionY,
            imageScale = imageProperties?.scale,
            imageRotationDegrees = imageProperties?.rotationDegrees,
            imageOpacity = imageProperties?.opacity
        )
    }
}
