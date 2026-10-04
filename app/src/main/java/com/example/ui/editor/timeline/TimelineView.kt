package com.example.ui.editor.timeline

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
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
import coil.compose.AsyncImage
import com.example.common.TimeUtils
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
import kotlin.math.roundToInt

@Composable
fun TimelineView(
    project: Project,
    playheadMs: Long,
    selectedItemId: String?,
    onSeek: (Long) -> Unit,
    onSelectItem: (String?) -> Unit,
    onTrimClip: (newStartMs: Long, newDurationMs: Long) -> Unit,
    onOpenTransition: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    // Zoom level: pixels per second (dpPerSecond)
    var dpPerSecond by remember { mutableFloatStateOf(60f) }

    // Scroll state for horizontal timeline track
    val scrollState = rememberScrollState()

    // Calculate total timeline width in DP
    val totalSeconds = (project.totalDurationMs / 1000f).coerceAtLeast(10f)
    val timelineWidthDp = (totalSeconds * dpPerSecond + 300f).dp

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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(TimelineTrackBg)
            .testTag("timeline_container")
    ) {
        // Zoom bar & Quick Time summary
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(VistaraDarkSurface)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TIMELINE",
                color = VistaraTextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            // Zoom Scale Chips (1x, 2x, 4x)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(40f to "1x", 70f to "2x", 120f to "3x").forEach { (scale, label) ->
                    val isSelected = dpPerSecond == scale
                    Surface(
                        onClick = { dpPerSecond = scale },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) VistaraSecondary.copy(alpha = 0.2f) else VistaraDarkSurfaceHighlight,
                        modifier = Modifier.height(26.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) VistaraSecondary else VistaraTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Timeline Scrollable Container with multi-track layout
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .horizontalScroll(scrollState)
                .pointerInput(project.totalDurationMs, dpPerSecond) {
                    detectTapGestures { offset ->
                        val clickedSeconds = (offset.x / density.density) / dpPerSecond
                        val clickedMs = (clickedSeconds * 1000L).toLong()
                        onSeek(clickedMs.coerceIn(0L, project.totalDurationMs))
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .width(timelineWidthDp)
                    .fillMaxHeight()
            ) {
                // 1. Time Ruler
                TimeRuler(
                    totalSeconds = totalSeconds,
                    dpPerSecond = dpPerSecond,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                )

                // 2. Overlay Track (Images / Stickers)
                if (project.imageLayers.isNotEmpty()) {
                    TrackRow(
                        title = "Overlay",
                        height = 36.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
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
                                    .padding(horizontal = 6.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Photo",
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

                // 3. Text Track
                if (project.textLayers.isNotEmpty()) {
                    TrackRow(
                        title = "Text",
                        height = 36.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
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
                                    .padding(horizontal = 6.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.TextFields,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
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
                }

                // 4. Main Video Track
                TrackRow(
                    title = "Video",
                    height = 76.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
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
                                .clickable { onSelectItem(item.id) }
                        ) {
                            // Thumbnail background strip
                            if (bitmap != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                // Gradient shade over thumbnail for legible labels
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.35f))
                                )
                            }

                            // Clip Title & Duration overlay
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

                            // Trim Handles (Left & Right) shown on selected clip
                            if (isSelected) {
                                // Left Trim Handle
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .width(16.dp)
                                        .fillMaxHeight()
                                        .background(VistaraSecondary.copy(alpha = 0.8f))
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
                                    Box(
                                        modifier = Modifier
                                            .width(2.dp)
                                            .height(16.dp)
                                            .background(Color.White)
                                    )
                                }

                                // Right Trim Handle
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .width(16.dp)
                                        .fillMaxHeight()
                                        .background(VistaraSecondary.copy(alpha = 0.8f))
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
                                    Box(
                                        modifier = Modifier
                                            .width(2.dp)
                                            .height(16.dp)
                                            .background(Color.White)
                                    )
                                }
                            }
                        }

                        // Transition button between this clip and the next
                        if (index < project.videoClips.size - 1) {
                            val nextClip = project.videoClips[index + 1]
                            val transitionEndDp = (nextClip.timelineStartMs / 1000f * dpPerSecond - 11f).dp
                            val hasTransition = item.transition.type != TransitionType.NONE

                            Surface(
                                onClick = { onOpenTransition(item.id) },
                                shape = CircleShape,
                                color = if (hasTransition) VistaraPrimary else VistaraDarkSurfaceHighlight,
                                modifier = Modifier
                                    .offset(x = transitionEndDp)
                                    .size(22.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Transform,
                                        contentDescription = "Transition",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. Audio Track
                if (project.audioClips.isNotEmpty()) {
                    TrackRow(
                        title = "Audio",
                        height = 36.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
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
                                    Icon(
                                        imageVector = Icons.Default.Audiotrack,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
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
            }

            // Draggable Playhead Needle
            val playheadOffsetDp = (playheadMs / 1000f * dpPerSecond).dp

            Box(
                modifier = Modifier
                    .offset(x = playheadOffsetDp - 10.dp)
                    .width(20.dp)
                    .fillMaxHeight()
                    .pointerInput(dpPerSecond, project.totalDurationMs) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaSeconds = (dragAmount.x / density.density) / dpPerSecond
                            val deltaMs = (deltaSeconds * 1000L).toLong()
                            val newMs = (playheadMs + deltaMs).coerceIn(0L, project.totalDurationMs)
                            onSeek(newMs)
                        }
                    },
                contentAlignment = Alignment.TopCenter
            ) {
                // Top Playhead Diamond / Pill
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(TimelinePlayhead)
                )
                // Vertical Line Needle
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
            .padding(vertical = 3.dp),
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
    Canvas(modifier = modifier.background(TimelineRulerBg)) {
        val totalSecInt = totalSeconds.toInt() + 5
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

            // Sub-second marks (half seconds)
            val halfX = x + (dpPerSecond * density) / 2f
            drawLine(
                color = strokeColor.copy(alpha = 0.5f),
                start = Offset(halfX, size.height - 6.dp.toPx()),
                end = Offset(halfX, size.height),
                strokeWidth = 1.5f
            )
        }
    }
}
