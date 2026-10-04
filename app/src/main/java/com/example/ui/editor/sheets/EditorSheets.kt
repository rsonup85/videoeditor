package com.example.ui.editor.sheets

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.CanvasAspectRatio
import com.example.domain.model.ClipTransform
import com.example.domain.model.TextLayerProperties
import com.example.domain.model.TransitionConfig
import com.example.domain.model.TransitionType
import com.example.ui.theme.VistaraDarkSurface
import com.example.ui.theme.VistaraDarkSurfaceBorder
import com.example.ui.theme.VistaraDarkSurfaceHighlight
import com.example.ui.theme.VistaraPrimary
import com.example.ui.theme.VistaraSecondary
import com.example.ui.theme.VistaraTextMuted
import com.example.ui.theme.VistaraTextPrimary
import com.example.ui.theme.VistaraTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedSheet(
    currentSpeed: Float,
    onApplySpeed: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var speed by remember { mutableFloatStateOf(currentSpeed) }
    val presets = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = VistaraDarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .testTag("speed_sheet")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Clip Speed",
                    color = VistaraTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${String.format("%.2f", speed)}x",
                    color = VistaraSecondary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Presets
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(presets) { p ->
                    val isSelected = (speed - p) in -0.05f..0.05f
                    Surface(
                        onClick = { speed = p },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) VistaraSecondary else VistaraDarkSurfaceHighlight,
                        modifier = Modifier.height(36.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        ) {
                            Text(
                                text = "${p}x",
                                color = if (isSelected) Color.Black else VistaraTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Slider(
                value = speed,
                onValueChange = { speed = it },
                valueRange = 0.25f..2.5f,
                colors = SliderDefaults.colors(
                    thumbColor = VistaraSecondary,
                    activeTrackColor = VistaraSecondary
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    onApplySpeed(speed)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = VistaraPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("apply_speed_button")
            ) {
                Text("Apply Speed", fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolumeSheet(
    currentVolume: Float,
    isMuted: Boolean,
    onApplyVolume: (volume: Float, isMuted: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var volume by remember { mutableFloatStateOf(currentVolume) }
    var muted by remember { mutableStateOf(isMuted) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = VistaraDarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .testTag("volume_sheet")
        ) {
            Text(
                text = "Audio & Volume",
                color = VistaraTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Mute Switch Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(VistaraDarkSurfaceHighlight, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (muted) Icons.Default.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = if (muted) Color.Red else VistaraSecondary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (muted) "Muted" else "Audio Enabled",
                        color = VistaraTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Switch(
                    checked = !muted,
                    onCheckedChange = { muted = !it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = VistaraSecondary,
                        checkedTrackColor = VistaraSecondary.copy(alpha = 0.4f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (!muted) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Clip Volume", color = VistaraTextSecondary, fontSize = 14.sp)
                    Text(
                        text = "${(volume * 100).toInt()}%",
                        color = VistaraSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Slider(
                    value = volume,
                    onValueChange = { volume = it },
                    valueRange = 0f..2.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = VistaraSecondary,
                        activeTrackColor = VistaraSecondary
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    onApplyVolume(volume, muted)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = VistaraPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("apply_volume_button")
            ) {
                Text("Apply Volume", fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransformSheet(
    initialTransform: ClipTransform,
    onApplyTransform: (ClipTransform) -> Unit,
    onDismiss: () -> Unit
) {
    var rotation by remember { mutableStateOf(initialTransform.rotationDegrees) }
    var flipH by remember { mutableStateOf(initialTransform.flipHorizontal) }
    var flipV by remember { mutableStateOf(initialTransform.flipVertical) }
    var scale by remember { mutableFloatStateOf(initialTransform.scale) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = VistaraDarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .testTag("transform_sheet")
        ) {
            Text(
                text = "Transform & Rotate",
                color = VistaraTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Rotate 90 deg
                Button(
                    onClick = { rotation = (rotation + 90) % 360 },
                    colors = ButtonDefaults.buttonColors(containerColor = VistaraDarkSurfaceHighlight),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.RotateRight, contentDescription = null, tint = VistaraSecondary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Rotate (${rotation}°)", color = VistaraTextPrimary, fontSize = 12.sp)
                }

                // Flip Horizontal
                Button(
                    onClick = { flipH = !flipH },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (flipH) VistaraSecondary.copy(alpha = 0.3f) else VistaraDarkSurfaceHighlight
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Flip, contentDescription = null, tint = if (flipH) VistaraSecondary else Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Flip H", color = VistaraTextPrimary, fontSize = 12.sp)
                }

                // Flip Vertical
                Button(
                    onClick = { flipV = !flipV },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (flipV) VistaraSecondary.copy(alpha = 0.3f) else VistaraDarkSurfaceHighlight
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Flip, contentDescription = null, tint = if (flipV) VistaraSecondary else Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Flip V", color = VistaraTextPrimary, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Zoom / Scale", color = VistaraTextSecondary, fontSize = 14.sp)
                Text(text = "${String.format("%.2f", scale)}x", color = VistaraSecondary, fontWeight = FontWeight.Bold)
            }

            Slider(
                value = scale,
                onValueChange = { scale = it },
                valueRange = 0.5f..2.5f,
                colors = SliderDefaults.colors(
                    thumbColor = VistaraSecondary,
                    activeTrackColor = VistaraSecondary
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    onApplyTransform(
                        initialTransform.copy(
                            rotationDegrees = rotation,
                            flipHorizontal = flipH,
                            flipVertical = flipV,
                            scale = scale
                        )
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = VistaraPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("apply_transform_button")
            ) {
                Text("Apply Transform", fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasSheet(
    currentRatio: CanvasAspectRatio,
    onSelectRatio: (CanvasAspectRatio) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = VistaraDarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .testTag("canvas_sheet")
        ) {
            Text(
                text = "Canvas Aspect Ratio",
                color = VistaraTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            CanvasAspectRatio.entries.forEach { ratio ->
                val isSelected = ratio == currentRatio
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) VistaraSecondary.copy(alpha = 0.15f) else VistaraDarkSurfaceHighlight)
                        .clickable {
                            onSelectRatio(ratio)
                            onDismiss()
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = ratio.label,
                            color = if (isSelected) VistaraSecondary else VistaraTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = ratio.description,
                            color = VistaraTextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    if (isSelected) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = VistaraSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextLayerSheet(
    initialProperties: TextLayerProperties? = null,
    onApply: (text: String, colorHex: String, bgHex: String?, fontSize: Float) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialProperties?.text ?: "My Title") }
    var fontSize by remember { mutableFloatStateOf(initialProperties?.fontSizeSp ?: 24f) }
    var selectedColor by remember { mutableStateOf(initialProperties?.colorHex ?: "#FFFFFF") }
    var hasBackground by remember { mutableStateOf(initialProperties?.backgroundColorHex != null) }

    val colors = listOf("#FFFFFF", "#F59E0B", "#38BDF8", "#8B5CF6", "#10B981", "#EF4444", "#000000")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = VistaraDarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .testTag("text_layer_sheet")
        ) {
            Text(
                text = "Text Layer",
                color = VistaraTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Enter text") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VistaraSecondary,
                    unfocusedBorderColor = VistaraDarkSurfaceBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("text_input_field")
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Font Size (${fontSize.toInt()}sp)", color = VistaraTextSecondary, fontSize = 13.sp)
            Slider(
                value = fontSize,
                onValueChange = { fontSize = it },
                valueRange = 14f..48f,
                colors = SliderDefaults.colors(thumbColor = VistaraSecondary, activeTrackColor = VistaraSecondary)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(text = "Color Palette", color = VistaraTextSecondary, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                colors.forEach { hex ->
                    val color = Color(android.graphics.Color.parseColor(hex))
                    val isSelected = selectedColor.equals(hex, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) VistaraSecondary else Color.Gray,
                                shape = CircleShape
                            )
                            .clickable { selectedColor = hex }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Add Background Box", color = VistaraTextPrimary, fontSize = 14.sp)
                Switch(
                    checked = hasBackground,
                    onCheckedChange = { hasBackground = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = VistaraSecondary)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val bgHex = if (hasBackground) "#99000000" else null
                    onApply(text, selectedColor, bgHex, fontSize)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = VistaraPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("apply_text_button")
            ) {
                Text("Save Text", fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransitionSheet(
    initialTransition: TransitionConfig,
    onApply: (TransitionConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedType by remember { mutableStateOf(initialTransition.type) }
    var durationMs by remember { mutableFloatStateOf(initialTransition.durationMs.toFloat()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = VistaraDarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .testTag("transition_sheet")
        ) {
            Text(
                text = "Clip Transition",
                color = VistaraTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            TransitionType.entries.forEach { type ->
                val isSelected = type == selectedType
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) VistaraSecondary.copy(alpha = 0.2f) else VistaraDarkSurfaceHighlight)
                        .clickable { selectedType = type }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = type.label,
                        color = if (isSelected) VistaraSecondary else VistaraTextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    if (isSelected) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = VistaraSecondary)
                    }
                }
            }

            if (selectedType != TransitionType.NONE) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Transition Duration", color = VistaraTextSecondary, fontSize = 13.sp)
                    Text(text = "${(durationMs / 1000f)}s", color = VistaraSecondary, fontWeight = FontWeight.Bold)
                }

                Slider(
                    value = durationMs,
                    onValueChange = { durationMs = it },
                    valueRange = 200f..1500f,
                    colors = SliderDefaults.colors(thumbColor = VistaraSecondary, activeTrackColor = VistaraSecondary)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    onApply(TransitionConfig(type = selectedType, durationMs = durationMs.toLong()))
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = VistaraPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("apply_transition_button")
            ) {
                Text("Apply Transition", fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
