package com.example.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.VistaraDarkSurface
import com.example.ui.theme.VistaraDarkSurfaceHighlight
import com.example.ui.theme.VistaraPrimary
import com.example.ui.theme.VistaraSecondary
import com.example.ui.theme.VistaraSuccess
import com.example.ui.theme.VistaraTextMuted
import com.example.ui.theme.VistaraTextPrimary
import com.example.ui.theme.VistaraTextSecondary

@Composable
fun EditorTopBar(
    projectName: String,
    autosaveStatus: String,
    canUndo: Boolean,
    canRedo: Boolean,
    onBack: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onOpenExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = VistaraDarkSurface,
        modifier = modifier
            .fillMaxWidth()
            .testTag("editor_top_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back Button
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("top_bar_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = VistaraTextPrimary
                )
            }

            // Project Name & Autosave badge
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = projectName,
                    color = VistaraTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Autosave pill
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = VistaraDarkSurfaceHighlight,
                    modifier = Modifier.height(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    if (autosaveStatus == "Saved") VistaraSuccess else VistaraSecondary,
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = autosaveStatus,
                            color = VistaraTextMuted,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Undo Button
            IconButton(
                onClick = onUndo,
                enabled = canUndo,
                modifier = Modifier.testTag("top_bar_undo_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo",
                    tint = if (canUndo) VistaraTextPrimary else VistaraTextSecondary.copy(alpha = 0.3f)
                )
            }

            // Redo Button
            IconButton(
                onClick = onRedo,
                enabled = canRedo,
                modifier = Modifier.testTag("top_bar_redo_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Redo,
                    contentDescription = "Redo",
                    tint = if (canRedo) VistaraTextPrimary else VistaraTextSecondary.copy(alpha = 0.3f)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Export Action Pill Button
            Button(
                onClick = onOpenExport,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VistaraPrimary),
                modifier = Modifier
                    .height(36.dp)
                    .testTag("top_bar_export_button")
            ) {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Export",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}
