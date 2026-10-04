package com.example.ui.editor.preview

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.common.TimeUtils
import com.example.domain.model.CanvasAspectRatio
import com.example.domain.model.ClipTransform
import com.example.domain.model.ImageLayerProperties
import com.example.domain.model.ItemType
import com.example.domain.model.Project
import com.example.domain.model.TextLayerProperties
import com.example.domain.model.TransitionType
import com.example.ui.theme.VistaraDarkBackground
import com.example.ui.theme.VistaraDarkSurface
import com.example.ui.theme.VistaraSecondary
import com.example.ui.theme.VistaraTextPrimary
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
@Composable
fun PreviewCanvas(
    project: Project,
    player: ExoPlayer,
    playheadMs: Long,
    isPlaying: Boolean,
    isFullscreen: Boolean,
    selectedItemId: String?,
    onTogglePlayPause: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onSeek: (Long) -> Unit,
    onJumpToStart: () -> Unit,
    onJumpToEnd: () -> Unit,
    onUpdateTextProperties: (String, TextLayerProperties) -> Unit,
    onUpdateImageProperties: (String, ImageLayerProperties) -> Unit,
    onSelectItem: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    // Find active main video clip
    val activeClip = project.videoClips.find { clip ->
        playheadMs >= clip.timelineStartMs && playheadMs < (clip.timelineStartMs + clip.durationMs)
    } ?: project.videoClips.lastOrNull()

    // Determine target canvas aspect ratio
    val targetRatio = when (val ratioEnum = project.canvasRatio) {
        CanvasAspectRatio.ORIGINAL -> {
            val asset = project.assets.find { it.id == activeClip?.assetId }
            if (asset != null && asset.width > 0 && asset.height > 0) {
                asset.width.toFloat() / asset.height.toFloat()
            } else 9f / 16f
        }
        else -> ratioEnum.ratio ?: (9f / 16f)
    }

    // Active text layers at playhead
    val activeTextLayers = project.textLayers.filter { item ->
        playheadMs >= item.timelineStartMs && playheadMs < (item.timelineStartMs + item.durationMs)
    }

    // Active image overlays at playhead
    val activeImageLayers = project.imageLayers.filter { item ->
        playheadMs >= item.timelineStartMs && playheadMs < (item.timelineStartMs + item.durationMs)
    }

    // Active overlay video clips at playhead
    val activeOverlayVideos = project.overlayVideoClips.filter { item ->
        playheadMs >= item.timelineStartMs && playheadMs < (item.timelineStartMs + item.durationMs)
    }

    // Canvas background color
    val canvasBg = runCatching { Color(android.graphics.Color.parseColor(project.canvasBackgroundColorHex)) }
        .getOrDefault(Color.Black)

    // Calculate transition visual effect
    var transitionAlpha = 1.0f
    var transitionOffsetX = 0f
    var transitionScale = 1.0f

    if (activeClip != null && activeClip.transition.type != TransitionType.NONE) {
        val clipEnd = activeClip.timelineStartMs + activeClip.durationMs
        val remaining = clipEnd - playheadMs
        val transitionDuration = activeClip.transition.durationMs.coerceAtLeast(100L)
        if (remaining in 0..transitionDuration) {
            val progress = (remaining.toFloat() / transitionDuration.toFloat()).coerceIn(0f, 1f)
            when (activeClip.transition.type) {
                TransitionType.FADE -> {
                    transitionAlpha = progress
                }
                TransitionType.CROSS_DISSOLVE -> {
                    transitionAlpha = progress
                }
                TransitionType.SLIDE_LEFT -> {
                    transitionOffsetX = (1f - progress) * -400f
                }
                TransitionType.SLIDE_RIGHT -> {
                    transitionOffsetX = (1f - progress) * 400f
                }
                TransitionType.ZOOM -> {
                    transitionScale = 1f + (1f - progress) * 0.4f
                }
                TransitionType.WIPE -> {
                    transitionAlpha = if (progress < 0.5f) progress * 2f else 1f
                }
                TransitionType.NONE -> {}
            }
        }
    }

    Column(
        modifier = modifier
            .background(VistaraDarkBackground)
            .testTag("preview_canvas_container"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Video Preview Viewport Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(if (isFullscreen) 0.dp else 8.dp),
            contentAlignment = Alignment.Center
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .aspectRatio(targetRatio)
                    .clip(RoundedCornerShape(if (isFullscreen) 0.dp else 12.dp))
                    .background(canvasBg),
                contentAlignment = Alignment.Center
            ) {
                val boxWidth = maxWidth
                val boxHeight = maxHeight

                // Main Video Surface
                val transform = activeClip?.transform
                val rotation = (transform?.rotationDegrees ?: 0).toFloat()
                val scaleXVal = (transform?.scale ?: 1f) * (if (transform?.flipHorizontal == true) -1f else 1f) * transitionScale
                val scaleYVal = (transform?.scale ?: 1f) * (if (transform?.flipVertical == true) -1f else 1f) * transitionScale
                val translationXVal = (transform?.offsetX ?: 0f) + transitionOffsetX
                val translationYVal = transform?.offsetY ?: 0f
                val clipOpacity = (transform?.opacity ?: 1.0f) * transitionAlpha

                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = false
                            this.player = player
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    update = { view ->
                        if (view.player != player) {
                            view.player = player
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            rotationZ = rotation
                            this.scaleX = scaleXVal
                            this.scaleY = scaleYVal
                            this.translationX = translationXVal
                            this.translationY = translationYVal
                            this.alpha = clipOpacity
                        }
                )

                // Render Image Overlays with Direct Manipulation
                for (overlay in activeImageLayers) {
                    val asset = project.assets.find { it.id == overlay.assetId }
                    val props = overlay.imageProperties
                    val isSelected = selectedItemId == overlay.id

                    if (asset != null && props != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset {
                                    IntOffset(
                                        x = ((props.positionX - 0.5f) * boxWidth.toPx()).roundToInt(),
                                        y = ((props.positionY - 0.5f) * boxHeight.toPx()).roundToInt()
                                    )
                                }
                                .graphicsLayer {
                                    rotationZ = props.rotationDegrees
                                    alpha = props.opacity
                                }
                                .pointerInput(overlay.id) {
                                    detectTransformGestures { _, pan, zoom, rotationChange ->
                                        onSelectItem(overlay.id)
                                        val newX = (props.positionX + pan.x / boxWidth.toPx()).coerceIn(0f, 1f)
                                        val newY = (props.positionY + pan.y / boxHeight.toPx()).coerceIn(0f, 1f)
                                        val newScale = (props.scale * zoom).coerceIn(0.2f, 3.5f)
                                        val newRot = (props.rotationDegrees + rotationChange) % 360f

                                        onUpdateImageProperties(
                                            overlay.id,
                                            props.copy(
                                                positionX = newX,
                                                positionY = newY,
                                                scale = newScale,
                                                rotationDegrees = newRot
                                            )
                                        )
                                    }
                                }
                                .then(
                                    if (isSelected) {
                                        Modifier.border(2.dp, VistaraSecondary, RoundedCornerShape(8.dp))
                                    } else Modifier
                                )
                                .padding(if (isSelected) 4.dp else 0.dp)
                        ) {
                            AsyncImage(
                                model = asset.uriString,
                                contentDescription = "Overlay",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size((boxWidth * 0.45f) * props.scale)
                            )
                        }
                    }
                }

                // Render Text Layers with Direct Manipulation
                for (textItem in activeTextLayers) {
                    val props = textItem.textProperties ?: continue
                    val isSelected = selectedItemId == textItem.id

                    val textColor = runCatching { Color(android.graphics.Color.parseColor(props.colorHex)) }
                        .getOrDefault(Color.White)
                    val bgColor = props.backgroundColorHex?.let {
                        runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull()
                    } ?: Color.Transparent

                    val fontFamily = when (props.fontFamily.lowercase()) {
                        "serif" -> FontFamily.Serif
                        "mono" -> FontFamily.Monospace
                        "cursive" -> FontFamily.Cursive
                        else -> FontFamily.SansSerif
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset {
                                IntOffset(
                                    x = ((props.positionX - 0.5f) * boxWidth.toPx()).roundToInt(),
                                    y = ((props.positionY - 0.5f) * boxHeight.toPx()).roundToInt()
                                )
                            }
                            .graphicsLayer {
                                rotationZ = props.rotationDegrees
                                this.scaleX = props.scale
                                this.scaleY = props.scale
                                alpha = props.opacity
                            }
                            .pointerInput(textItem.id) {
                                detectTransformGestures { _, pan, zoom, rotationChange ->
                                    onSelectItem(textItem.id)
                                    val newX = (props.positionX + pan.x / boxWidth.toPx()).coerceIn(0f, 1f)
                                    val newY = (props.positionY + pan.y / boxHeight.toPx()).coerceIn(0f, 1f)
                                    val newScale = (props.scale * zoom).coerceIn(0.2f, 4.0f)
                                    val newRot = (props.rotationDegrees + rotationChange) % 360f

                                    onUpdateTextProperties(
                                        textItem.id,
                                        props.copy(
                                            positionX = newX,
                                            positionY = newY,
                                            scale = newScale,
                                            rotationDegrees = newRot
                                        )
                                    )
                                }
                            }
                            .then(
                                if (isSelected) {
                                    Modifier.border(2.dp, VistaraSecondary, RoundedCornerShape(8.dp))
                                } else Modifier
                            )
                            .background(bgColor, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = props.text,
                            color = textColor,
                            fontFamily = fontFamily,
                            fontSize = props.fontSizeSp.sp,
                            fontWeight = if (props.fontFamily == "bold") FontWeight.ExtraBold else FontWeight.Bold,
                            textAlign = when (props.alignment) {
                                "LEFT" -> TextAlign.Left
                                "RIGHT" -> TextAlign.Right
                                else -> TextAlign.Center
                            },
                            modifier = if (props.hasShadow) {
                                Modifier.shadow(elevation = 4.dp, shape = RoundedCornerShape(4.dp))
                            } else Modifier
                        )
                    }
                }
            }
        }

        // Live Scrub Bar & Transport Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            // Preview Scrubber Slider (synchronized with timeline playhead)
            val maxMs = project.totalDurationMs.coerceAtLeast(1000L).toFloat()
            Slider(
                value = playheadMs.coerceIn(0L, project.totalDurationMs).toFloat(),
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..maxMs,
                colors = SliderDefaults.colors(
                    thumbColor = VistaraSecondary,
                    activeTrackColor = VistaraSecondary,
                    inactiveTrackColor = Color.DarkGray
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .testTag("preview_scrubber_slider")
            )

            // Transport Control Buttons Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Jump to Start (0:00)
                IconButton(
                    onClick = onJumpToStart,
                    modifier = Modifier.size(36.dp).testTag("jump_start_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FastRewind,
                        contentDescription = "Jump to Start",
                        tint = VistaraTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Play / Pause Circle
                Surface(
                    onClick = onTogglePlayPause,
                    shape = CircleShape,
                    color = VistaraDarkSurface,
                    modifier = Modifier.size(42.dp).testTag("play_pause_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = VistaraSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Jump to End
                IconButton(
                    onClick = onJumpToEnd,
                    modifier = Modifier.size(36.dp).testTag("jump_end_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "Jump to End",
                        tint = VistaraTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Time Indicator (00:01.24 / 00:15.00)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = VistaraDarkSurface,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = TimeUtils.formatTimeDetailed(playheadMs),
                            color = VistaraSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = " / ${TimeUtils.formatTimeDetailed(project.totalDurationMs)}",
                            color = VistaraTextPrimary.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                }

                // Fullscreen Toggle
                IconButton(
                    onClick = onToggleFullscreen,
                    modifier = Modifier.size(36.dp).testTag("fullscreen_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = "Fullscreen",
                        tint = VistaraTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
