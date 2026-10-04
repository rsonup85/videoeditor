package com.example.ui.editor.preview

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
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
import com.example.domain.model.ItemType
import com.example.domain.model.Project
import com.example.domain.model.TimelineItem
import com.example.domain.model.TransitionType
import com.example.ui.theme.VistaraDarkBackground
import com.example.ui.theme.VistaraDarkSurface
import com.example.ui.theme.VistaraPrimary
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
    onTogglePlayPause: () -> Unit,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Find active video clip
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

    // Calculate transition effect (Fade / Dissolve)
    var transitionAlpha = 1.0f
    if (activeClip != null && activeClip.transition.type == TransitionType.FADE) {
        val clipEnd = activeClip.timelineStartMs + activeClip.durationMs
        val remaining = clipEnd - playheadMs
        val transitionDuration = activeClip.transition.durationMs.coerceAtLeast(100L)
        if (remaining in 0..transitionDuration) {
            transitionAlpha = (remaining.toFloat() / transitionDuration.toFloat()).coerceIn(0f, 1f)
        }
    }

    Box(
        modifier = modifier
            .background(VistaraDarkBackground)
            .testTag("preview_canvas_container"),
        contentAlignment = Alignment.Center
    ) {
        // Aspect-ratio-constrained viewport box
        Box(
            modifier = Modifier
                .padding(if (isFullscreen) 0.dp else 12.dp)
                .aspectRatio(targetRatio)
                .clip(RoundedCornerShape(if (isFullscreen) 0.dp else 12.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            // Video Surface with real-time transforms
            val transform = activeClip?.transform
            val rotation = (transform?.rotationDegrees ?: 0).toFloat()
            val scaleX = (transform?.scale ?: 1f) * (if (transform?.flipHorizontal == true) -1f else 1f)
            val scaleY = (transform?.scale ?: 1f) * (if (transform?.flipVertical == true) -1f else 1f)
            val translationX = transform?.offsetX ?: 0f
            val translationY = transform?.offsetY ?: 0f

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
                        this.scaleX = scaleX
                        this.scaleY = scaleY
                        this.translationX = translationX
                        this.translationY = translationY
                        this.alpha = transitionAlpha
                    }
            )

            // Image Overlays
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val boxWidth = maxWidth
                val boxHeight = maxHeight

                for (overlay in activeImageLayers) {
                    val asset = project.assets.find { it.id == overlay.assetId }
                    val props = overlay.imageProperties
                    if (asset != null && props != null) {
                        AsyncImage(
                            model = asset.uriString,
                            contentDescription = "Image Overlay",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size((boxWidth * 0.45f) * props.scale)
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
                        )
                    }
                }

                // Text Layers
                for (textItem in activeTextLayers) {
                    val props = textItem.textProperties ?: continue
                    val textColor = runCatching { Color(android.graphics.Color.parseColor(props.colorHex)) }
                        .getOrDefault(Color.White)
                    val bgColor = props.backgroundColorHex?.let {
                        runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull()
                    } ?: Color.Transparent

                    val textAlign = when (props.alignment) {
                        "LEFT" -> TextAlign.Left
                        "RIGHT" -> TextAlign.Right
                        else -> TextAlign.Center
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
                            .background(bgColor, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = props.text,
                            color = textColor,
                            fontSize = props.fontSizeSp.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = textAlign
                        )
                    }
                }
            }
        }

        // Floating Transport Overlay Controls (Play/Pause, Time, Fullscreen)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play / Pause Circle Button
            Surface(
                onClick = onTogglePlayPause,
                shape = CircleShape,
                color = VistaraDarkSurface.copy(alpha = 0.85f),
                contentColor = VistaraTextPrimary,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("play_pause_button")
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

            Spacer(modifier = Modifier.width(12.dp))

            // Time Indicator (00:02.15 / 00:15.00)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = VistaraDarkSurface.copy(alpha = 0.85f),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = TimeUtils.formatTimeDetailed(playheadMs),
                        color = VistaraSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = " / ${TimeUtils.formatTimeDetailed(project.totalDurationMs)}",
                        color = VistaraTextPrimary.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Fullscreen Toggle Button
            IconButton(
                onClick = onToggleFullscreen,
                modifier = Modifier
                    .background(VistaraDarkSurface.copy(alpha = 0.85f), CircleShape)
                    .size(40.dp)
                    .testTag("fullscreen_toggle_button")
            ) {
                Icon(
                    imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDescription = if (isFullscreen) "Exit Fullscreen" else "Fullscreen",
                    tint = VistaraTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
