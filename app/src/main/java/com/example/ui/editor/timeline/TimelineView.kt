package com.example.ui.editor.timeline

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ItemType
import com.example.domain.model.Project
import com.example.domain.model.TimelineItem
import com.example.domain.model.TransitionType
import com.example.media.ThumbnailLoader
import com.example.ui.theme.TimelineClipAudioBg
import com.example.ui.theme.TimelineClipOverlayBg
import com.example.ui.theme.TimelineClipTextBg
import com.example.ui.theme.TimelineClipVideoBg
import com.example.ui.theme.TimelineClipVideoBorder
import com.example.ui.theme.TimelinePlayhead
import com.example.ui.theme.TimelineRulerBg
import com.example.ui.theme.TimelineTrackBg
import com.example.ui.theme.VistaraDarkSurface
import com.example.ui.theme.VistaraDarkSurfaceHighlight
import com.example.ui.theme.VistaraPrimary
import com.example.ui.theme.VistaraSecondary
import com.example.ui.theme.VistaraTextMuted
import com.example.ui.theme.VistaraTextPrimary
import com.example.ui.theme.VistaraTextSecondary
import kotlin.math.abs

@Composable
fun TimelineView(
    project: Project,
    playheadMs: Long,
    selectedItemId: String?,
    onSeek: (Long) -> Unit,
    onSelectItem: (String?) -> Unit,
    onTrimClip: (newStartMs: Long, newDurationMs: Long) -> Unit,
    onMoveLayer: (itemId: String, newTimelineStartMs: Long) -> Unit,
    onTrimLayer: (itemId: String, newDurationMs: Long) -> Unit,
    onOpenTransition: (String) -> Unit,
    onToggleSnap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    // Zoom level: pixels per second
    var dpPerSecond by remember { mutableFloatStateOf(60f) }

    // Scroll state for horizontal timeline tracks
    val scrollState = rememberScrollState()

    // Calculate total timeline width in DP with generous right padding
    val totalSeconds = (project.totalDurationMs / 1000f).coerceAtLeast(10f)
    val timelineWidthDp = (totalSeconds * dpPerSecond + 350f).dp

    // Thumbnail cache for video clips
    val thumbnails = remember { mutableStateMapOf<String, Bitmap?>() }

    LaunchedEffect(project.items) {
        for (item in project.videoClips) {
            val asset = project.assets.find { it.id == item.assetId }
            if (asset != null && !thumbnails.containsKey(item.id)) {
                val frame = ThumbnailLoader.getFrameThumbnail(
                    context,
                    asset.uriString,
                    item.sourceStartMs + 500L,
                    targetWidth = 140,
                    targetHeight = 90
                )
                thumbnails[item.id] = frame
            }
        }
    }

    // Magnetic Snapping helper
    fun applyMagneticSnap(targetMs: Long): Long {
        if (!project.isSnapEnabled) return targetMs
        val snapThresholdMs = (250f / (dpPerSecond / 60f)).toLong()

        // Candidate snap points
        val snapPoints = mutableListOf(0L, playheadMs)
        for (clip in project.videoClips) {
            snapPoints.add(clip.timelineStartMs)
            snapPoints.add(clip.timelineStartMs + clip.durationMs)
        }

        var closest = targetMs
        var minDiff = Long.MAX_VALUE

        for (pt in snapPoints) {
            val diff = abs(pt - targetMs)
            if (diff <= snapThresholdMs && diff < minDiff) {
                minDiff = diff
                closest = pt
            }
        }
        return closest
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(TimelineTrackBg)
            .testTag("timeline_container")
    ) {
        // Control Bar: Track Title, Magnetic Snap Toggle, Zoom Presets
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(VistaraDarkSurface)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "TRACKS",
                    color = VistaraTextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Magnetic Snap Toggle Button
                Surface(
                    onClick = onToggleSnap,
                    shape = RoundedCornerShape(8.dp),
                    color = if (project.isSnapEnabled) VistaraSecondary.copy(alpha = 0.25f) else VistaraDarkSurfaceHighlight,
                    modifier = Modifier.height(26.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoFixHigh,
                            contentDescription = "Snap",
                            tint = if (project.isSnapEnabled) VistaraSecondary else VistaraTextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (project.isSnapEnabled) "Snap ON" else "Snap OFF",
                            color = if (project.isSnapEnabled) VistaraSecondary else VistaraTextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Zoom Scale Chips (1x, 2x, 3x)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(40f to "1x", 70f to "2x", 110f to "3x").forEach { (scale, label) ->
                    val isSelected = dpPerSecond == scale
                    Surface(
                        onClick = { dpPerSecond = scale },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) VistaraSecondary.copy(alpha = 0.2f) else VistaraDarkSurfaceHighlight,
                        modifier = Modifier.height(24.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) VistaraSecondary else VistaraTextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Dedicated Interactive Time Ruler Header with Scrubbing Gestures
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .background(TimelineRulerBg)
                .horizontalScroll(scrollState)
                .pointerInput(dpPerSecond, project.totalDurationMs) {
                    detectTapGestures { offset ->
                        val clickedSeconds = (offset.x / density.density) / dpPerSecond
                        val clickedMs = (clickedSeconds * 1000L).toLong()
                        onSeek(clickedMs.coerceIn(0L, project.totalDurationMs))
                    }
                }
                .pointerInput(dpPerSecond, project.totalDurationMs) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val scrubSeconds = (change.position.x / density.density) / dpPerSecond
                        val scrubMs = (scrubSeconds * 1000L).toLong()
                        onSeek(scrubMs.coerceIn(0L, project.totalDurationMs))
                    }
                }
        ) {
            TimeRuler(
                totalSeconds = totalSeconds,
                dpPerSecond = dpPerSecond,
                modifier = Modifier
                    .width(timelineWidthDp)
                    .height(30.dp)
            )

            // Playhead indicator pin on ruler
            val playheadOffsetDp = (playheadMs / 1000f * dpPerSecond).dp
            Box(
                modifier = Modifier
                    .offset(x = playheadOffsetDp - 8.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(TimelinePlayhead)
            )
        }

        // Multi-Track Viewport (Horizontally Scrollable)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .horizontalScroll(scrollState)
        ) {
            Column(
                modifier = Modifier
                    .width(timelineWidthDp)
                    .fillMaxHeight()
                    .padding(vertical = 4.dp)
            ) {
                // Track 1: TEXT
                TrackRow(title = "TEXT", height = 36.dp) {
                    for (item in project.textLayers) {
                        val startDp = (item.timelineStartMs / 1000f * dpPerSecond).dp
                        val widthDp = (item.durationMs / 1000f * dpPerSecond).coerceAtLeast(36f).dp
                        val isSelected = selectedItemId == item.id

                        Box(
                            modifier = Modifier
                                .offset(x = startDp)
                                .width(widthDp)
                                .height(30.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(TimelineClipTextBg)
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) VistaraSecondary else Color.Transparent,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { onSelectItem(item.id) }
                                .pointerInput(item.id, dpPerSecond) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val deltaSeconds = (dragAmount.x / density.density) / dpPerSecond
                                        val deltaMs = (deltaSeconds * 1000L).toLong()
                                        val newStart = applyMagneticSnap((item.timelineStartMs + deltaMs).coerceAtLeast(0L))
                                        onMoveLayer(item.id, newStart)
                                    }
                                }
                                .padding(horizontal = 6.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TextFields, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = item.textProperties?.text ?: "Text",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Track 2: IMAGE / OVERLAY
                TrackRow(title = "IMAGE", height = 36.dp) {
                    for (item in project.imageLayers) {
                        val startDp = (item.timelineStartMs / 1000f * dpPerSecond).dp
                        val widthDp = (item.durationMs / 1000f * dpPerSecond).coerceAtLeast(36f).dp
                        val isSelected = selectedItemId == item.id

                        Box(
                            modifier = Modifier
                                .offset(x = startDp)
                                .width(widthDp)
                                .height(30.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(TimelineClipOverlayBg)
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) VistaraSecondary else Color.Transparent,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { onSelectItem(item.id) }
                                .pointerInput(item.id, dpPerSecond) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val deltaSeconds = (dragAmount.x / density.density) / dpPerSecond
                                        val deltaMs = (deltaSeconds * 1000L).toLong()
                                        val newStart = applyMagneticSnap((item.timelineStartMs + deltaMs).coerceAtLeast(0L))
                                        onMoveLayer(item.id, newStart)
                                    }
                                }
                                .padding(horizontal = 6.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Image, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Sticker",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Track 3: VIDEO 1 (Main Track with Thumbnails, Trimming, Transitions)
                TrackRow(title = "VIDEO 1", height = 74.dp) {
                    for ((index, item) in project.videoClips.withIndex()) {
                        val startDp = (item.timelineStartMs / 1000f * dpPerSecond).dp
                        val widthDp = (item.durationMs / 1000f * dpPerSecond).coerceAtLeast(44f).dp
                        val isSelected = selectedItemId == item.id
                        val asset = project.assets.find { it.id == item.assetId }
                        val bitmap = thumbnails[item.id]

                        Box(
                            modifier = Modifier
                                .offset(x = startDp)
                                .width(widthDp)
                                .height(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(TimelineClipVideoBg)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) TimelineClipVideoBorder else Color.DarkGray,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    onSelectItem(item.id)
                                    onSeek(item.timelineStartMs)
                                }
                        ) {
                            // Real video frame thumbnail
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.35f))
                                )
                            }

                            // Clip Title & Duration details
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = asset?.fileName ?: "Clip ${index + 1}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${(item.durationMs / 1000f)}s",
                                        color = VistaraSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (item.speed != 1.0f) {
                                        Text(
                                            text = "${item.speed}x",
                                            color = Color.Yellow,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Trim Handles (Left & Right)
                            if (isSelected) {
                                // Left Trim
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .width(18.dp)
                                        .fillMaxHeight()
                                        .background(VistaraSecondary.copy(alpha = 0.85f))
                                        .pointerInput(item.id, dpPerSecond) {
                                            detectDragGestures { change, dragAmount ->
                                                change.consume()
                                                val deltaSeconds = (dragAmount.x / density.density) / dpPerSecond
                                                val deltaMs = (deltaSeconds * 1000L).toLong()
                                                val newSourceStart = (item.sourceStartMs + deltaMs).coerceAtLeast(0L)
                                                val newDuration = (item.sourceDurationMs - deltaMs).coerceAtLeast(500L)
                                                onTrimClip(newSourceStart, newDuration)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(modifier = Modifier.width(2.dp).height(18.dp).background(Color.White))
                                }

                                // Right Trim
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .width(18.dp)
                                        .fillMaxHeight()
                                        .background(VistaraSecondary.copy(alpha = 0.85f))
                                        .pointerInput(item.id, dpPerSecond) {
                                            detectDragGestures { change, dragAmount ->
                                                change.consume()
                                                val deltaSeconds = (dragAmount.x / density.density) / dpPerSecond
                                                val deltaMs = (deltaSeconds * 1000L).toLong()
                                                val newDuration = (item.sourceDurationMs + deltaMs).coerceAtLeast(500L)
                                                onTrimClip(item.sourceStartMs, newDuration)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(modifier = Modifier.width(2.dp).height(18.dp).background(Color.White))
                                }
                            }
                        }

                        // Transition Chip between adjacent clips
                        if (index < project.videoClips.size - 1) {
                            val nextClip = project.videoClips[index + 1]
                            val transitionEndDp = (nextClip.timelineStartMs / 1000f * dpPerSecond - 12f).dp
                            val hasTransition = item.transition.type != TransitionType.NONE

                            Surface(
                                onClick = { onOpenTransition(item.id) },
                                shape = CircleShape,
                                color = if (hasTransition) VistaraPrimary else VistaraDarkSurfaceHighlight,
                                modifier = Modifier
                                    .offset(x = transitionEndDp)
                                    .size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Transform,
                                        contentDescription = "Transition",
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Track 4: AUDIO 1
                TrackRow(title = "AUDIO 1", height = 36.dp) {
                    for (item in project.audioClips) {
                        val startDp = (item.timelineStartMs / 1000f * dpPerSecond).dp
                        val widthDp = (item.durationMs / 1000f * dpPerSecond).coerceAtLeast(36f).dp
                        val isSelected = selectedItemId == item.id
                        val asset = project.assets.find { it.id == item.assetId }

                        Box(
                            modifier = Modifier
                                .offset(x = startDp)
                                .width(widthDp)
                                .height(30.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(TimelineClipAudioBg)
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) VistaraSecondary else Color.Transparent,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { onSelectItem(item.id) }
                                .padding(horizontal = 6.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Audiotrack, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = asset?.fileName ?: "Audio",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // High-Visibility Playhead Needle with Generous Draggable Touch Target
            val playheadOffsetDp = (playheadMs / 1000f * dpPerSecond).dp

            Box(
                modifier = Modifier
                    .offset(x = playheadOffsetDp - 18.dp)
                    .width(36.dp)
                    .fillMaxHeight()
                    .pointerInput(dpPerSecond, project.totalDurationMs) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val scrubSeconds = (change.position.x / density.density) / dpPerSecond
                            val deltaSeconds = (scrubSeconds)
                            val newMs = (playheadMs + (deltaSeconds * 1000L).toLong()).coerceIn(0L, project.totalDurationMs)
                            onSeek(newMs)
                        }
                    },
                contentAlignment = Alignment.TopCenter
            ) {
                // Top Playhead Diamond
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(TimelinePlayhead)
                )
                // Vertical Line
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(TimelinePlayhead)
                )
            }
        }
    }
}

@Composable
private fun TrackRow(
    title: String,
    height: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .height(height)
            .padding(vertical = 2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        content()
    }
}

@Composable
private fun TimeRuler(
    totalSeconds: Float,
    dpPerSecond: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val totalSecInt = totalSeconds.toInt() + 8
        val strokeColor = Color(0xFF64748B)

        for (sec in 0..totalSecInt) {
            val x = sec * dpPerSecond * density
            // Major second mark
            drawLine(
                color = strokeColor,
                start = Offset(x, size.height - 12.dp.toPx()),
                end = Offset(x, size.height),
                strokeWidth = 2f
            )

            // Half-second tick
            val halfX = x + (dpPerSecond * density) / 2f
            drawLine(
                color = strokeColor.copy(alpha = 0.45f),
                start = Offset(halfX, size.height - 6.dp.toPx()),
                end = Offset(halfX, size.height),
                strokeWidth = 1.5f
            )
        }
    }
}
