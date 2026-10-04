package com.example.editor.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import com.example.common.FileUtils
import com.example.domain.model.CanvasAspectRatio
import com.example.domain.model.ClipTransform
import com.example.domain.model.ExportQuality
import com.example.domain.model.ExportResolution
import com.example.domain.model.ExportSettings
import com.example.domain.model.ItemType
import com.example.domain.model.Project
import com.example.domain.model.TimelineItem
import com.example.domain.model.TransitionType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

sealed class ExportState {
    object Idle : ExportState()
    data class Exporting(
        val progress: Float,
        val currentClipIndex: Int,
        val totalClips: Int,
        val statusMessage: String = "Processing video…"
    ) : ExportState()
    data class Success(
        val mediaStoreUri: Uri,
        val localFile: File,
        val durationMs: Long
    ) : ExportState()
    data class Error(val message: String) : ExportState()
}

class VideoExporter(private val context: Context) {

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    private val isCancelled = AtomicBoolean(false)

    fun cancelExport() {
        isCancelled.set(true)
    }

    suspend fun exportProject(project: Project): Uri? = withContext(Dispatchers.IO) {
        val videoClips = project.videoClips
        if (videoClips.isEmpty()) {
            _exportState.value = ExportState.Error("Project contains no video clips to export.")
            return@withContext null
        }

        isCancelled.set(false)
        _exportState.value = ExportState.Exporting(0.01f, 0, videoClips.size, "Preparing export pipeline…")

        val tempOutputFile = FileUtils.createTempExportFile(context, project.name)
        var muxer: MediaMuxer? = null
        var isMuxerStarted = false

        try {
            val totalProjectDurationUs = (project.totalDurationMs.coerceAtLeast(1000L)) * 1000L
            muxer = MediaMuxer(tempOutputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            var muxerVideoTrackIndex = -1
            var muxerAudioTrackIndex = -1

            // Configure muxer tracks from the primary clip format
            val firstClip = videoClips.first()
            val firstAsset = project.assets.find { it.id == firstClip.assetId }
                ?: return@withContext reportError("Missing media asset for clip ${firstClip.id}", tempOutputFile)

            val headerExtractor = MediaExtractor()
            try {
                headerExtractor.setDataSource(context, Uri.parse(firstAsset.uriString), null)
                for (i in 0 until headerExtractor.trackCount) {
                    val format = headerExtractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                    if (mime.startsWith("video/") && muxerVideoTrackIndex == -1) {
                        muxerVideoTrackIndex = muxer.addTrack(format)
                    } else if (mime.startsWith("audio/") && muxerAudioTrackIndex == -1) {
                        muxerAudioTrackIndex = muxer.addTrack(format)
                    }
                }
            } finally {
                headerExtractor.release()
            }

            if (muxerVideoTrackIndex == -1) {
                return@withContext reportError("No compatible video track found in source media.", tempOutputFile)
            }

            muxer.start()
            isMuxerStarted = true

            var videoPtsOffsetUs = 0L
            var audioPtsOffsetUs = 0L
            val bufferSize = 2 * 1024 * 1024 // 2MB buffer for high-bitrate video samples
            val buffer = ByteBuffer.allocateDirect(bufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            var processedDurationUs = 0L

            for ((clipIndex, clip) in videoClips.withIndex()) {
                if (isCancelled.get()) throw CancellationException("Export was cancelled by user.")

                _exportState.value = ExportState.Exporting(
                    progress = (processedDurationUs.toFloat() / totalProjectDurationUs.toFloat()).coerceIn(0.05f, 0.90f),
                    currentClipIndex = clipIndex + 1,
                    totalClips = videoClips.size,
                    statusMessage = "Exporting clip ${clipIndex + 1} of ${videoClips.size}…"
                )

                val asset = project.assets.find { it.id == clip.assetId } ?: continue
                val extractor = MediaExtractor()

                try {
                    extractor.setDataSource(context, Uri.parse(asset.uriString), null)

                    var sourceVideoTrack = -1
                    var sourceAudioTrack = -1

                    for (i in 0 until extractor.trackCount) {
                        val format = extractor.getTrackFormat(i)
                        val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                        if (mime.startsWith("video/") && sourceVideoTrack == -1) {
                            sourceVideoTrack = i
                        } else if (mime.startsWith("audio/") && sourceAudioTrack == -1) {
                            sourceAudioTrack = i
                        }
                    }

                    val clipSourceStartUs = clip.sourceStartMs * 1000L
                    val clipSourceDurationUs = (clip.sourceDurationMs.coerceAtLeast(100L)) * 1000L
                    val clipSourceEndUs = clipSourceStartUs + clipSourceDurationUs

                    // --- Process Video Track for this clip ---
                    if (sourceVideoTrack != -1) {
                        extractor.selectTrack(sourceVideoTrack)
                        extractor.seekTo(clipSourceStartUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

                        var firstSampleTimeUs = -1L
                        var lastWrittenPtsUs = videoPtsOffsetUs

                        while (!isCancelled.get()) {
                            buffer.clear()
                            val sampleSize = extractor.readSampleData(buffer, 0)
                            if (sampleSize < 0) break

                            val sampleTimeUs = extractor.sampleTime
                            val sampleFlags = extractor.sampleFlags

                            if (sampleTimeUs >= clipSourceStartUs) {
                                if (firstSampleTimeUs == -1L) {
                                    firstSampleTimeUs = sampleTimeUs
                                }

                                if (sampleTimeUs <= clipSourceEndUs) {
                                    val relativeTimeUs = sampleTimeUs - firstSampleTimeUs
                                    val speedAdjustedRelativeTimeUs = (relativeTimeUs / clip.speed.coerceAtLeast(0.1f)).toLong()
                                    val outputPtsUs = videoPtsOffsetUs + speedAdjustedRelativeTimeUs

                                    bufferInfo.offset = 0
                                    bufferInfo.size = sampleSize
                                    bufferInfo.presentationTimeUs = outputPtsUs
                                    bufferInfo.flags = sampleFlags

                                    muxer.writeSampleData(muxerVideoTrackIndex, buffer, bufferInfo)
                                    lastWrittenPtsUs = outputPtsUs
                                } else {
                                    break
                                }
                            }
                            extractor.advance()
                        }

                        extractor.unselectTrack(sourceVideoTrack)
                        videoPtsOffsetUs = lastWrittenPtsUs + 33_333L // ~30fps frame increment
                    }

                    // --- Process Audio Track for this clip (if not muted) ---
                    if (sourceAudioTrack != -1 && muxerAudioTrackIndex != -1 && !clip.isMuted) {
                        extractor.selectTrack(sourceAudioTrack)
                        extractor.seekTo(clipSourceStartUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

                        var firstAudioSampleUs = -1L
                        var lastWrittenAudioPtsUs = audioPtsOffsetUs

                        while (!isCancelled.get()) {
                            buffer.clear()
                            val sampleSize = extractor.readSampleData(buffer, 0)
                            if (sampleSize < 0) break

                            val sampleTimeUs = extractor.sampleTime
                            val sampleFlags = extractor.sampleFlags

                            if (sampleTimeUs >= clipSourceStartUs) {
                                if (firstAudioSampleUs == -1L) {
                                    firstAudioSampleUs = sampleTimeUs
                                }

                                if (sampleTimeUs <= clipSourceEndUs) {
                                    val relativeTimeUs = sampleTimeUs - firstAudioSampleUs
                                    val speedAdjustedRelativeUs = (relativeTimeUs / clip.speed.coerceAtLeast(0.1f)).toLong()
                                    val outputPtsUs = audioPtsOffsetUs + speedAdjustedRelativeUs

                                    bufferInfo.offset = 0
                                    bufferInfo.size = sampleSize
                                    bufferInfo.presentationTimeUs = outputPtsUs
                                    bufferInfo.flags = sampleFlags

                                    muxer.writeSampleData(muxerAudioTrackIndex, buffer, bufferInfo)
                                    lastWrittenAudioPtsUs = outputPtsUs
                                } else {
                                    break
                                }
                            }
                            extractor.advance()
                        }

                        extractor.unselectTrack(sourceAudioTrack)
                        audioPtsOffsetUs = lastWrittenAudioPtsUs + 23_000L
                    }

                } finally {
                    extractor.release()
                }

                processedDurationUs += (clip.durationMs * 1000L)
            }

            if (isCancelled.get()) {
                throw CancellationException("Export was cancelled.")
            }

            // Finish and close muxer
            muxer.stop()
            isMuxerStarted = false
            muxer.release()
            muxer = null

            _exportState.value = ExportState.Exporting(
                progress = 0.95f,
                currentClipIndex = videoClips.size,
                totalClips = videoClips.size,
                statusMessage = "Saving video to Gallery…"
            )

            // Save to modern Android MediaStore (Movies/Vistara Edit)
            val mediaStoreUri = FileUtils.saveVideoToGallery(context, tempOutputFile, project.name)
            if (mediaStoreUri == null) {
                return@withContext reportError(
                    "Video processing finished, but failed to save to Android Gallery. Please check device storage permissions.",
                    tempOutputFile
                )
            }

            _exportState.value = ExportState.Success(
                mediaStoreUri = mediaStoreUri,
                localFile = tempOutputFile,
                durationMs = project.totalDurationMs
            )

            mediaStoreUri

        } catch (e: CancellationException) {
            tempOutputFile.delete()
            _exportState.value = ExportState.Idle
            null
        } catch (e: Exception) {
            e.printStackTrace()
            tempOutputFile.delete()
            val userMsg = when {
                e.message?.contains("ENOSPC", true) == true ->
                    "Export failed because the device ran out of available storage space."
                e.message?.contains("codec", true) == true ->
                    "Export failed due to an incompatible video codec in the imported media."
                else -> "Export encountered an error: ${e.localizedMessage ?: "Unknown media processing error"}"
            }
            _exportState.value = ExportState.Error(userMsg)
            null
        } finally {
            try {
                if (isMuxerStarted) muxer?.stop()
                muxer?.release()
            } catch (_: Exception) {}
        }
    }

    private fun reportError(message: String, tempFile: File): Uri? {
        tempFile.delete()
        _exportState.value = ExportState.Error(message)
        return null
    }

    fun resetState() {
        _exportState.value = ExportState.Idle
    }
}
