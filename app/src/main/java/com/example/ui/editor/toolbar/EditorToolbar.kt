package com.example.ui.editor.toolbar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Transform
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.VistaraDarkSurface
import com.example.ui.theme.VistaraDarkSurfaceHighlight
import com.example.ui.theme.VistaraSecondary
import com.example.ui.theme.VistaraTextPrimary
import com.example.ui.theme.VistaraTextSecondary

@Composable
fun EditorToolbar(
    canSplit: Boolean,
    hasSelectedItem: Boolean,
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
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Split Action
            ToolbarToolItem(
                icon = Icons.Default.ContentCut,
                label = "Split",
                enabled = canSplit,
                onClick = onSplit,
                testTag = "toolbar_split"
            )

            // Delete Action
            ToolbarToolItem(
                icon = Icons.Default.Delete,
                label = "Delete",
                enabled = hasSelectedItem,
                onClick = onDelete,
                testTag = "toolbar_delete"
            )

            // Duplicate Action
            ToolbarToolItem(
                icon = Icons.Default.ContentCopy,
                label = "Duplicate",
                enabled = hasSelectedItem,
                onClick = onDuplicate,
                testTag = "toolbar_duplicate"
            )

            // Speed Action
            ToolbarToolItem(
                icon = Icons.Default.Speed,
                label = "Speed",
                enabled = hasSelectedItem,
                onClick = onOpenSpeed,
                testTag = "toolbar_speed"
            )

            // Volume Action
            ToolbarToolItem(
                icon = Icons.Default.VolumeUp,
                label = "Volume",
                enabled = hasSelectedItem,
                onClick = onOpenVolume,
                testTag = "toolbar_volume"
            )

            // Crop / Transform
            ToolbarToolItem(
                icon = Icons.Default.CropRotate,
                label = "Transform",
                enabled = hasSelectedItem,
                onClick = onOpenTransform,
                testTag = "toolbar_transform"
            )

            // Canvas Aspect Ratio
            ToolbarToolItem(
                icon = Icons.Default.AspectRatio,
                label = "Canvas",
                enabled = true,
                onClick = onOpenCanvas,
                testTag = "toolbar_canvas"
            )

            // Text Layer
            ToolbarToolItem(
                icon = Icons.Default.TextFields,
                label = "Text",
                enabled = true,
                onClick = onOpenText,
                testTag = "toolbar_text"
            )

            // Image Overlay
            ToolbarToolItem(
                icon = Icons.Default.AddPhotoAlternate,
                label = "Overlay",
                enabled = true,
                onClick = onOpenOverlay,
                testTag = "toolbar_overlay"
            )

            // Transition
            ToolbarToolItem(
                icon = Icons.Default.Transform,
                label = "Transition",
                enabled = hasSelectedItem,
                onClick = onOpenTransition,
                testTag = "toolbar_transition"
            )

            // Add Media to Timeline
            ToolbarToolItem(
                icon = Icons.Default.AddCircleOutline,
                label = "Add Media",
                enabled = true,
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
    onClick: () -> Unit,
    testTag: String
) {
    val contentAlpha = if (enabled) 1f else 0.35f
    val tintColor = if (enabled) VistaraTextPrimary else VistaraTextSecondary.copy(alpha = 0.4f)

    Column(
        modifier = Modifier
            .width(64.dp)
            .height(58.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(testTag)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tintColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = tintColor,
            fontSize = 11.sp,
            maxLines = 1
        )
    }
}
