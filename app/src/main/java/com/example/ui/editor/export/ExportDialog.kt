package com.example.ui.editor.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.common.FileUtils
import com.example.common.TimeUtils
import com.example.domain.model.ExportQuality
import com.example.domain.model.ExportResolution
import com.example.domain.model.ExportSettings
import com.example.editor.export.ExportState
import com.example.ui.theme.VistaraDarkSurface
import com.example.ui.theme.VistaraDarkSurfaceBorder
import com.example.ui.theme.VistaraDarkSurfaceHighlight
import com.example.ui.theme.VistaraPrimary
import com.example.ui.theme.VistaraSecondary
import com.example.ui.theme.VistaraSuccess
import com.example.ui.theme.VistaraTextPrimary
import com.example.ui.theme.VistaraTextSecondary

@Composable
fun ExportConfigDialog(
    initialSettings: ExportSettings,
    onStartExport: (ExportSettings) -> Unit,
    onDismiss: () -> Unit
) {
    var resolution by remember { mutableStateOf(initialSettings.resolution) }
    var quality by remember { mutableStateOf(initialSettings.quality) }
    var fps by remember { mutableStateOf(initialSettings.fps) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = VistaraDarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, VistaraDarkSurfaceBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("export_config_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = VistaraSecondary,
                    modifier = Modifier.size(36.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Export Project",
                    color = VistaraTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Resolution Selection
                Text(
                    text = "Resolution",
                    color = VistaraTextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(ExportResolution.RES_720P, ExportResolution.RES_1080P).forEach { res ->
                        val isSelected = res == resolution
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) VistaraSecondary.copy(alpha = 0.2f) else VistaraDarkSurfaceHighlight)
                                .clickable { resolution = res }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = res.label,
                                color = if (isSelected) VistaraSecondary else VistaraTextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Frame Rate (FPS)
                Text(
                    text = "Frame Rate",
                    color = VistaraTextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(24, 30, 60).forEach { rate ->
                        val isSelected = rate == fps
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) VistaraSecondary.copy(alpha = 0.2f) else VistaraDarkSurfaceHighlight)
                                .clickable { fps = rate }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${rate}fps",
                                color = if (isSelected) VistaraSecondary else VistaraTextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quality Selection
                Text(
                    text = "Quality",
                    color = VistaraTextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(ExportQuality.LOW, ExportQuality.MEDIUM, ExportQuality.HIGH).forEach { q ->
                        val isSelected = q == quality
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) VistaraSecondary.copy(alpha = 0.2f) else VistaraDarkSurfaceHighlight)
                                .clickable { quality = q }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = q.label,
                                color = if (isSelected) VistaraSecondary else VistaraTextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text("Cancel", color = VistaraTextSecondary)
                    }

                    Button(
                        onClick = {
                            onStartExport(
                                ExportSettings(
                                    resolution = resolution,
                                    fps = fps,
                                    quality = quality
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VistaraPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("start_export_button")
                    ) {
                        Text("Export Now", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun ExportStatusDialog(
    exportState: ExportState,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    if (exportState is ExportState.Idle) return

    Dialog(onDismissRequest = {
        if (exportState is ExportState.Success || exportState is ExportState.Error) {
            onDismiss()
        }
    }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = VistaraDarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, VistaraDarkSurfaceBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("export_status_dialog")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (exportState) {
                    is ExportState.Exporting -> {
                        Text(
                            text = "Exporting Video…",
                            color = VistaraTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = exportState.statusMessage,
                            color = VistaraTextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        LinearProgressIndicator(
                            progress = { exportState.progress },
                            color = VistaraSecondary,
                            trackColor = VistaraDarkSurfaceHighlight,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "${(exportState.progress * 100).toInt()}%",
                            color = VistaraSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedButton(
                            onClick = onCancel,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("cancel_export_button")
                        ) {
                            Text("Cancel", color = Color.Red)
                        }
                    }

                    is ExportState.Success -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = VistaraSuccess,
                            modifier = Modifier.size(48.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Export Complete",
                            color = VistaraTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Video saved to Gallery",
                            color = VistaraSuccess,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Location: Movies/Vistara Edit • ${TimeUtils.formatDurationHuman(exportState.durationMs)}",
                            color = VistaraTextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Open Video
                            Button(
                                onClick = { openVideoUri(context, exportState.mediaStoreUri) },
                                colors = ButtonDefaults.buttonColors(containerColor = VistaraDarkSurfaceHighlight),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("open_video_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = null,
                                    tint = VistaraSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open", color = VistaraTextPrimary, fontSize = 13.sp)
                            }

                            // Share Video
                            Button(
                                onClick = { shareVideoUri(context, exportState.mediaStoreUri) },
                                colors = ButtonDefaults.buttonColors(containerColor = VistaraPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("share_video_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share", color = Color.White, fontSize = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            modifier = Modifier.testTag("export_done_button")
                        ) {
                            Text("Done", color = VistaraTextSecondary, fontSize = 14.sp)
                        }
                    }

                    is ExportState.Error -> {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(48.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Export Failed",
                            color = Color.Red,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = exportState.message,
                            color = VistaraTextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = VistaraDarkSurfaceHighlight),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("export_error_close_button")
                        ) {
                            Text("Close", color = VistaraTextPrimary)
                        }
                    }

                    ExportState.Idle -> {}
                }
            }
        }
    }
}

private fun openVideoUri(context: Context, uri: Uri) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/mp4")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun shareVideoUri(context: Context, uri: Uri) {
    try {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Share Video via").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
