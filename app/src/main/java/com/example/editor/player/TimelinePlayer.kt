package com.example.editor.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.domain.model.ItemType
import com.example.domain.model.Project
import com.example.domain.model.TimelineItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TimelinePlayer(
    private val context: Context,
    private val scope: CoroutineScope
) {
    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build().apply {
        repeatMode = Player.REPEAT_MODE_OFF
    }

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playheadMs = MutableStateFlow(0L)
    val playheadMs: StateFlow<Long> = _playheadMs.asStateFlow()

    private var currentProject: Project? = null
    private var currentLoadedAssetId: String? = null
    private var tickerJob: Job? = null

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                _isPlaying.value = playing
                if (playing) {
                    startTicker()
                } else {
                    stopTicker()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    handleClipEnded()
                }
            }
        })
    }

    fun setProject(project: Project) {
        this.currentProject = project
        // Verify current playhead is within bounds
        val maxDuration = project.totalDurationMs
        if (_playheadMs.value > maxDuration) {
            seekTo(maxDuration)
        } else {
            syncPlayerToPlayhead(loadMediaIfDifferent = false)
        }
    }

    fun play() {
        val project = currentProject ?: return
        if (project.videoClips.isEmpty()) return

        if (_playheadMs.value >= project.totalDurationMs && project.totalDurationMs > 0) {
            seekTo(0L)
        }
        syncPlayerToPlayhead(loadMediaIfDifferent = true)
        exoPlayer.play()
    }

    fun pause() {
        exoPlayer.pause()
        stopTicker()
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            play()
        }
    }

    fun seekTo(timelineMs: Long) {
        val project = currentProject ?: return
        val clampedTime = timelineMs.coerceIn(0L, project.totalDurationMs.coerceAtLeast(0L))
        _playheadMs.value = clampedTime
        syncPlayerToPlayhead(loadMediaIfDifferent = true)
    }

    private fun syncPlayerToPlayhead(loadMediaIfDifferent: Boolean) {
        val project = currentProject ?: return
        val currentPlayhead = _playheadMs.value

        // Find active video clip at playhead
        val activeClip = project.videoClips.find { clip ->
            currentPlayhead >= clip.timelineStartMs && currentPlayhead < (clip.timelineStartMs + clip.durationMs)
        } ?: project.videoClips.lastOrNull()

        if (activeClip == null) return

        val asset = project.assets.find { it.id == activeClip.assetId } ?: return
        val relativeOffsetMs = (currentPlayhead - activeClip.timelineStartMs).coerceAtLeast(0L)
        val sourceMediaSeekMs = activeClip.sourceStartMs + (relativeOffsetMs * activeClip.speed).toLong()

        if (currentLoadedAssetId != asset.id) {
            currentLoadedAssetId = asset.id
            val mediaItem = MediaItem.fromUri(Uri.parse(asset.uriString))
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
        }

        exoPlayer.playbackParameters = PlaybackParameters(activeClip.speed)
        exoPlayer.volume = if (activeClip.isMuted) 0f else activeClip.volume.coerceIn(0f, 2f)
        exoPlayer.seekTo(sourceMediaSeekMs.coerceAtLeast(0L))
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch(Dispatchers.Main) {
            var lastTime = System.currentTimeMillis()
            while (isActive && _isPlaying.value) {
                val now = System.currentTimeMillis()
                val delta = now - lastTime
                lastTime = now

                val project = currentProject
                if (project != null) {
                    val newPlayhead = _playheadMs.value + delta
                    if (newPlayhead >= project.totalDurationMs) {
                        _playheadMs.value = project.totalDurationMs
                        pause()
                        seekTo(0L) // Reset to start
                        break
                    } else {
                        _playheadMs.value = newPlayhead

                        // Check if we stepped into a new clip
                        val activeClip = project.videoClips.find { clip ->
                            newPlayhead >= clip.timelineStartMs && newPlayhead < (clip.timelineStartMs + clip.durationMs)
                        }
                        if (activeClip != null && activeClip.assetId != currentLoadedAssetId) {
                            syncPlayerToPlayhead(loadMediaIfDifferent = true)
                            exoPlayer.play()
                        }
                    }
                }
                delay(30) // ~33fps playhead update tick
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun handleClipEnded() {
        val project = currentProject ?: return
        val nextClip = project.videoClips.find { it.timelineStartMs > _playheadMs.value }
        if (nextClip != null) {
            seekTo(nextClip.timelineStartMs)
            exoPlayer.play()
        } else {
            pause()
            seekTo(0L)
        }
    }

    fun release() {
        stopTicker()
        exoPlayer.release()
    }
}
