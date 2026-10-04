package com.example.editor.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
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

    private val _isScrubbing = MutableStateFlow(false)
    val isScrubbing: StateFlow<Boolean> = _isScrubbing.asStateFlow()

    private var currentProject: Project? = null
    private var currentLoadedAssetId: String? = null
    private var tickerJob: Job? = null

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                _isPlaying.value = playing
                if (playing) {
                    startPlaybackTicker()
                } else {
                    stopPlaybackTicker()
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
        val maxDuration = project.totalDurationMs
        if (_playheadMs.value > maxDuration) {
            seekTo(maxDuration)
        } else {
            syncPlayerToCurrentPlayhead(forceReload = false)
        }
    }

    fun play() {
        val project = currentProject ?: return
        if (project.videoClips.isEmpty()) return

        if (_playheadMs.value >= project.totalDurationMs && project.totalDurationMs > 0) {
            seekTo(0L)
        }
        syncPlayerToCurrentPlayhead(forceReload = true)
        exoPlayer.play()
    }

    fun pause() {
        exoPlayer.pause()
        stopPlaybackTicker()
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            play()
        }
    }

    fun startScrubbing() {
        _isScrubbing.value = true
        if (_isPlaying.value) {
            pause()
        }
    }

    fun stopScrubbing() {
        _isScrubbing.value = false
    }

    /**
     * Authoritative seek method called by playhead drag, ruler clicks, or transport controls.
     */
    fun seekTo(timelineMs: Long) {
        val project = currentProject ?: return
        val clampedTime = timelineMs.coerceIn(0L, project.totalDurationMs.coerceAtLeast(0L))
        _playheadMs.value = clampedTime
        syncPlayerToCurrentPlayhead(forceReload = false)
    }

    fun jumpToStart() {
        seekTo(0L)
    }

    fun jumpToEnd() {
        val project = currentProject ?: return
        seekTo(project.totalDurationMs)
    }

    private fun syncPlayerToCurrentPlayhead(forceReload: Boolean) {
        val project = currentProject ?: return
        val currentPlayhead = _playheadMs.value

        // Locate active clip at this timeline millisecond
        val activeClip = project.videoClips.find { clip ->
            currentPlayhead >= clip.timelineStartMs && currentPlayhead < (clip.timelineStartMs + clip.durationMs)
        } ?: project.videoClips.lastOrNull()

        if (activeClip == null) return

        val asset = project.assets.find { it.id == activeClip.assetId } ?: return
        val relativeOffsetMs = (currentPlayhead - activeClip.timelineStartMs).coerceAtLeast(0L)
        val sourceMediaSeekMs = activeClip.sourceStartMs + (relativeOffsetMs * activeClip.speed).toLong()

        val needsNewMedia = currentLoadedAssetId != asset.id || forceReload

        if (needsNewMedia) {
            currentLoadedAssetId = asset.id
            val mediaItem = MediaItem.fromUri(Uri.parse(asset.uriString))
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
        }

        exoPlayer.playbackParameters = PlaybackParameters(activeClip.speed)
        exoPlayer.volume = if (activeClip.isMuted) 0f else activeClip.volume.coerceIn(0f, 2f)
        exoPlayer.seekTo(sourceMediaSeekMs.coerceAtLeast(0L))
    }

    private fun startPlaybackTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch(Dispatchers.Main) {
            while (isActive && _isPlaying.value) {
                val project = currentProject
                if (project != null && project.videoClips.isNotEmpty()) {
                    val currentPlayhead = _playheadMs.value
                    val activeClip = project.videoClips.find { clip ->
                        currentPlayhead >= clip.timelineStartMs && currentPlayhead < (clip.timelineStartMs + clip.durationMs)
                    }

                    if (activeClip != null) {
                        val playerPos = exoPlayer.currentPosition
                        val relativeSourceMs = (playerPos - activeClip.sourceStartMs).coerceAtLeast(0L)
                        val timelineDerivedMs = activeClip.timelineStartMs + (relativeSourceMs / activeClip.speed).toLong()

                        if (timelineDerivedMs >= (activeClip.timelineStartMs + activeClip.durationMs)) {
                            // Clip finished playing, transition to next clip
                            val nextClip = project.videoClips.find { it.timelineStartMs >= (activeClip.timelineStartMs + activeClip.durationMs) }
                            if (nextClip != null) {
                                _playheadMs.value = nextClip.timelineStartMs
                                syncPlayerToCurrentPlayhead(forceReload = true)
                                exoPlayer.play()
                            } else {
                                _playheadMs.value = project.totalDurationMs
                                pause()
                                seekTo(0L)
                                break
                            }
                        } else {
                            _playheadMs.value = timelineDerivedMs.coerceIn(0L, project.totalDurationMs)
                        }
                    }
                }
                delay(20) // ~50fps smooth playhead synchronization
            }
        }
    }

    private fun stopPlaybackTicker() {
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
        stopPlaybackTicker()
        exoPlayer.release()
    }
}
