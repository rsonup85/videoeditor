package com.example.ui.editor.toolbar

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.CropRotate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ItemType
import com.example.domain.model.TimelineItem
import com.example.ui.theme.VistaraDarkSurface
import com.example.ui.theme.VistaraPrimary
import com.example.ui.theme.VistaraSecondary
import com.example.ui.theme.VistaraTextPrimary
import com.example.ui.theme.VistaraTextSecondary

@Composable
fun EditorToolbar(
    selectedItem: TimelineItem?,
    canSplit: Boolean,
    onSplit: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onOpenSpeed: () -> Unit,
    onOpenVolume: () -> Unit,
    onOpenTransform: () -> Unit,
    onOpenCanvas: () -> Unit,
    onOpenText: () -> Unit,
    onOpenOverlay: () -> Unit,
    onOpenTransition: () -> Unit,
    onAddMedia: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Surface(
        color = VistaraDarkSurface,
        modifier = modifier
            .fillMaxWidth()
            .testTag("editor_toolbar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (selectedItem?.type) {
                ItemType.TEXT -> {
                    // Contextual Text Tools
                    ToolbarToolItem(
                        icon = Icons.Default.Edit,
                        label = "Edit Text",
                        enabled = true,
                        highlight = true,
                        onClick = onOpenText,
                        testTag = "toolbar_text_edit"
                    )
                    ToolbarToolItem(
                        icon = Icons.Default.CropRotate,
                        label = "Transform",
                        enabled = true,
                        onClick = onOpenTransform,
                        testTag = "toolbar_transform"
                    )
                    ToolbarToolItem(
                        icon = Icons.Default.ContentCopy,
                        label = "Duplicate",
                        enabled = true,
                        onClick = onDuplicate,
                        testTag = "toolbar_duplicate"
                    )
                    ToolbarToolItem(
                        icon = Icons.Default.Delete,
                        label = "Delete",
                        enabled = true,
                        onClick = onDelete,
                        testTag = "toolbar_delete"
                    )
                }

                ItemType.IMAGE -> {
                    // Contextual Overlay Tools
                    ToolbarToolItem(
                        icon = Icons.Default.CropRotate,
                        label = "Transform",
                        enabled = true,
                        highlight = true,
                        onClick = onOpenTransform,
                        testTag = "toolbar_transform"
                    )
                    ToolbarToolItem(
                        icon = Icons.Default.ContentCopy,
                        label = "Duplicate",
                        enabled = true,
                        onClick = onDuplicate,
                        testTag = "toolbar_duplicate"
                    )
                    ToolbarToolItem(
                        icon = Icons.Default.Delete,
                        label = "Delete",
                        enabled = true,
                        onClick = onDelete,
                        testTag = "toolbar_delete"
                    )
                }

                else -> {
                    // Video Clip or Default Tools
                    ToolbarToolItem(
                        icon = Icons.Default.ContentCut,
                        label = "Split",
                        enabled = canSplit,
                        highlight = canSplit,
                        onClick = onSplit,
                        testTag = "toolbar_split"
                    )

                    ToolbarToolItem(
                        icon = Icons.Default.Speed,
                        label = "Speed",
                        enabled = selectedItem != null,
                        onClick = onOpenSpeed,
                        testTag = "toolbar_speed"
                    )

                    ToolbarToolItem(
                        icon = Icons.Default.VolumeUp,
                        label = "Volume",
                        enabled = selectedItem != null,
                        onClick = onOpenVolume,
                        testTag = "toolbar_volume"
                    )

                    ToolbarToolItem(
                        icon = Icons.Default.CropRotate,
                        label = "Transform",
                        enabled = selectedItem != null,
                        onClick = onOpenTransform,
                        testTag = "toolbar_transform"
                    )

                    ToolbarToolItem(
                        icon = Icons.Default.Transform,
                        label = "Transition",
                        enabled = selectedItem != null,
                        onClick = onOpenTransition,
                        testTag = "toolbar_transition"
                    )

                    ToolbarToolItem(
                        icon = Icons.Default.ContentCopy,
                        label = "Duplicate",
                        enabled = selectedItem != null,
                        onClick = onDuplicate,
                        testTag = "toolbar_duplicate"
                    )

                    ToolbarToolItem(
                        icon = Icons.Default.Delete,
                        label = "Delete",
                        enabled = selectedItem != null,
                        onClick = onDelete,
                        testTag = "toolbar_delete"
                    )
                }
            }

            // Universal Tools (Canvas, Text, Overlay, Add Media)
            ToolbarToolItem(
                icon = Icons.Default.AspectRatio,
                label = "Canvas",
                enabled = true,
                onClick = onOpenCanvas,
                testTag = "toolbar_canvas"
            )

            ToolbarToolItem(
                icon = Icons.Default.TextFields,
                label = "+ Text",
                enabled = true,
                onClick = onOpenText,
                testTag = "toolbar_text"
            )

            ToolbarToolItem(
                icon = Icons.Default.AddPhotoAlternate,
                label = "+ Overlay",
                enabled = true,
                onClick = onOpenOverlay,
                testTag = "toolbar_overlay"
            )

            ToolbarToolItem(
                icon = Icons.Default.AddCircleOutline,
                label = "Add Media",
                enabled = true,
                highlight = true,
                onClick = onAddMedia,
                testTag = "toolbar_add_media"
            )
        }
    }
}

@Composable
private fun ToolbarToolItem(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    highlight: Boolean = false,
    onClick: () -> Unit,
    testTag: String
) {
    val tintColor = when {
        !enabled -> VistaraTextSecondary.copy(alpha = 0.35f)
        highlight -> VistaraSecondary
        else -> VistaraTextPrimary
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent,
        modifier = Modifier
            .width(62.dp)
            .height(56.dp)
            .testTag(testTag)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tintColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = label,
                color = tintColor,
                fontSize = 10.sp,
                fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1
            )
        }
    }
}
