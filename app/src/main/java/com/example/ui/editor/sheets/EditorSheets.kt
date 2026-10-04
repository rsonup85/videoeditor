package com.example.ui.editor.sheets

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RotateRight
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.CanvasAspectRatio
import com.example.domain.model.ClipTransform
import com.example.domain.model.EditorFont
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
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedSheet(
    currentSpeed: Float,
    onApplySpeed: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var speed by remember { mutableFloatStateOf(currentSpeed) }
    var manualInput by remember { mutableStateOf(String.format(Locale.US, "%.2f", currentSpeed)) }
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
                    text = "Clip Speed (0.1x – 10.0x)",
                    color = VistaraTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                // Current Speed Badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = VistaraSecondary.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "${String.format(Locale.US, "%.2f", speed)}x",
                        color = VistaraSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Presets Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(presets) { p ->
                    val isSelected = (speed - p) in -0.04f..0.04f
                    Surface(
                        onClick = {
                            speed = p
                            manualInput = String.format(Locale.US, "%.2f", p)
                            onApplySpeed(p)
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) VistaraSecondary else VistaraDarkSurfaceHighlight,
                        modifier = Modifier.height(34.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Text(
                                text = "${p}x",
                                color = if (isSelected) Color.Black else VistaraTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Manual Slider with range 0.1x to 5.0x (and up to 10.0x via input)
            Slider(
                value = speed.coerceIn(0.1f, 5.0f),
                onValueChange = {
                    speed = it
                    manualInput = String.format(Locale.US, "%.2f", it)
                    onApplySpeed(it)
                },
                valueRange = 0.1f..5.0f,
                colors = SliderDefaults.colors(
                    thumbColor = VistaraSecondary,
                    activeTrackColor = VistaraSecondary
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Manual Stepper Controls (- / +) and Direct Input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(VistaraDarkSurfaceHighlight, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val newSpeed = (speed - 0.1f).coerceAtLeast(0.1f)
                        speed = newSpeed
                        manualInput = String.format(Locale.US, "%.2f", newSpeed)
                        onApplySpeed(newSpeed)
                    }
                ) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease Speed", tint = VistaraSecondary)
                }

                // Direct Numerical Input Field
                OutlinedTextField(
                    value = manualInput,
                    onValueChange = { input ->
                        manualInput = input
                        val parsed = input.toFloatOrNull()
                        if (parsed != null && parsed in 0.1f..10.0f) {
                            speed = parsed
                            onApplySpeed(parsed)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VistaraSecondary,
                        unfocusedBorderColor = VistaraDarkSurfaceBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.width(100.dp)
                )

                IconButton(
                    onClick = {
                        val newSpeed = (speed + 0.1f).coerceAtMost(10.0f)
                        speed = newSpeed
                        manualInput = String.format(Locale.US, "%.2f", newSpeed)
                        onApplySpeed(newSpeed)
                    }
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Increase Speed", tint = VistaraSecondary)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

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
                Text("Done", fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))
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

            Spacer(modifier = Modifier.height(16.dp))

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
                    onCheckedChange = {
                        muted = !it
                        onApplyVolume(volume, muted)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = VistaraSecondary,
                        checkedTrackColor = VistaraSecondary.copy(alpha = 0.4f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

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
                    onValueChange = {
                        volume = it
                        onApplyVolume(it, muted)
                    },
                    valueRange = 0f..2.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = VistaraSecondary,
                        activeTrackColor = VistaraSecondary
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = VistaraPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("apply_volume_button")
            ) {
                Text("Done", fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))
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
    var transform by remember { mutableStateOf(initialTransform) }

    fun updateAndNotify(newTransform: ClipTransform) {
        transform = newTransform
        onApplyTransform(newTransform)
    }

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

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons: Rotate 90, Flip H, Flip V
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val nextRotation = (transform.rotationDegrees + 90) % 360
                        updateAndNotify(transform.copy(rotationDegrees = nextRotation))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VistaraDarkSurfaceHighlight),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.RotateRight, contentDescription = null, tint = VistaraSecondary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "${transform.rotationDegrees}°", color = VistaraTextPrimary, fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        updateAndNotify(transform.copy(flipHorizontal = !transform.flipHorizontal))
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (transform.flipHorizontal) VistaraSecondary.copy(alpha = 0.3f) else VistaraDarkSurfaceHighlight
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Flip, contentDescription = null, tint = if (transform.flipHorizontal) VistaraSecondary else Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Flip H", color = VistaraTextPrimary, fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        updateAndNotify(transform.copy(flipVertical = !transform.flipVertical))
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (transform.flipVertical) VistaraSecondary.copy(alpha = 0.3f) else VistaraDarkSurfaceHighlight
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Flip, contentDescription = null, tint = if (transform.flipVertical) VistaraSecondary else Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Flip V", color = VistaraTextPrimary, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Scale / Zoom Slider (Live immediate preview update)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Scale", color = VistaraTextSecondary, fontSize = 13.sp)
                Text(
                    text = "${String.format(Locale.US, "%.2f", transform.scale)}x",
                    color = VistaraSecondary,
                    fontWeight = FontWeight.Bold
                )
            }
            Slider(
                value = transform.scale,
                onValueChange = {
                    updateAndNotify(transform.copy(scale = it))
                },
                valueRange = 0.25f..3.0f,
                colors = SliderDefaults.colors(thumbColor = VistaraSecondary, activeTrackColor = VistaraSecondary)
            )

            // Opacity Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Opacity", color = VistaraTextSecondary, fontSize = 13.sp)
                Text(
                    text = "${(transform.opacity * 100).toInt()}%",
                    color = VistaraSecondary,
                    fontWeight = FontWeight.Bold
                )
            }
            Slider(
                value = transform.opacity,
                onValueChange = {
                    updateAndNotify(transform.copy(opacity = it))
                },
                valueRange = 0.1f..1.0f,
                colors = SliderDefaults.colors(thumbColor = VistaraSecondary, activeTrackColor = VistaraSecondary)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = VistaraPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("apply_transform_button")
            ) {
                Text("Done", fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasSheet(
    currentRatio: CanvasAspectRatio,
    currentBgHex: String,
    onSelectRatio: (CanvasAspectRatio) -> Unit,
    onSelectBgColor: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val bgColors = listOf("#000000", "#16161D", "#2A1B4E", "#0A192F", "#334155", "#FFFFFF")

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
                text = "Canvas & Background",
                color = VistaraTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(text = "Aspect Ratio", color = VistaraTextSecondary, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))

            CanvasAspectRatio.entries.forEach { ratio ->
                val isSelected = ratio == currentRatio
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) VistaraSecondary.copy(alpha = 0.15f) else VistaraDarkSurfaceHighlight)
                        .clickable { onSelectRatio(ratio) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = ratio.label,
                            color = if (isSelected) VistaraSecondary else VistaraTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(text = ratio.description, color = VistaraTextMuted, fontSize = 11.sp)
                    }
                    if (isSelected) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = VistaraSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Canvas Background Color
            Text(text = "Canvas Background Color", color = VistaraTextSecondary, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                bgColors.forEach { hex ->
                    val color = Color(android.graphics.Color.parseColor(hex))
                    val isSelected = currentBgHex.equals(hex, ignoreCase = true)
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
                            .clickable { onSelectBgColor(hex) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = VistaraPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Done", fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextLayerSheet(
    initialProperties: TextLayerProperties? = null,
    onApply: (TextLayerProperties) -> Unit,
    onDismiss: () -> Unit
) {
    var props by remember { mutableStateOf(initialProperties ?: TextLayerProperties()) }
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
                .verticalScroll(rememberScrollState())
                .testTag("text_layer_sheet")
        ) {
            Text(
                text = "Text Styling & Typography",
                color = VistaraTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = props.text,
                onValueChange = {
                    val updated = props.copy(text = it)
                    props = updated
                    onApply(updated)
                },
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

            Spacer(modifier = Modifier.height(14.dp))

            // Font Selection Row (Sans, Serif, Mono, Handwritten, Bold, Light)
            Text(text = "Font Family", color = VistaraTextSecondary, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(EditorFont.entries) { font ->
                    val isSelected = props.fontFamily.equals(font.fontId, ignoreCase = true)
                    Surface(
                        onClick = {
                            val updated = props.copy(fontFamily = font.fontId)
                            props = updated
                            onApply(updated)
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) VistaraSecondary else VistaraDarkSurfaceHighlight,
                        modifier = Modifier.height(34.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Text(
                                text = font.displayName,
                                color = if (isSelected) Color.Black else VistaraTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Font Size Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Font Size", color = VistaraTextSecondary, fontSize = 13.sp)
                Text(text = "${props.fontSizeSp.toInt()}sp", color = VistaraSecondary, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = props.fontSizeSp,
                onValueChange = {
                    val updated = props.copy(fontSizeSp = it)
                    props = updated
                    onApply(updated)
                },
                valueRange = 14f..64f,
                colors = SliderDefaults.colors(thumbColor = VistaraSecondary, activeTrackColor = VistaraSecondary)
            )

            // Opacity Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Text Opacity", color = VistaraTextSecondary, fontSize = 13.sp)
                Text(text = "${(props.opacity * 100).toInt()}%", color = VistaraSecondary, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = props.opacity,
                onValueChange = {
                    val updated = props.copy(opacity = it)
                    props = updated
                    onApply(updated)
                },
                valueRange = 0.1f..1.0f,
                colors = SliderDefaults.colors(thumbColor = VistaraSecondary, activeTrackColor = VistaraSecondary)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Color Palette
            Text(text = "Text Color", color = VistaraTextSecondary, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                colors.forEach { hex ->
                    val color = Color(android.graphics.Color.parseColor(hex))
                    val isSelected = props.colorHex.equals(hex, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) VistaraSecondary else Color.Gray,
                                shape = CircleShape
                            )
                            .clickable {
                                val updated = props.copy(colorHex = hex)
                                props = updated
                                onApply(updated)
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Background & Shadow Toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Dark Background Box", color = VistaraTextPrimary, fontSize = 14.sp)
                Switch(
                    checked = props.backgroundColorHex != null,
                    onCheckedChange = { checked ->
                        val bg = if (checked) "#99000000" else null
                        val updated = props.copy(backgroundColorHex = bg)
                        props = updated
                        onApply(updated)
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = VistaraSecondary)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Drop Shadow", color = VistaraTextPrimary, fontSize = 14.sp)
                Switch(
                    checked = props.hasShadow,
                    onCheckedChange = { checked ->
                        val updated = props.copy(hasShadow = checked)
                        props = updated
                        onApply(updated)
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = VistaraSecondary)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = VistaraPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("apply_text_button")
            ) {
                Text("Done", fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))
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
                .verticalScroll(rememberScrollState())
                .testTag("transition_sheet")
        ) {
            Text(
                text = "Clip Transition",
                color = VistaraTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(14.dp))

            TransitionType.entries.forEach { type ->
                val isSelected = type == selectedType
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) VistaraSecondary.copy(alpha = 0.2f) else VistaraDarkSurfaceHighlight)
                        .clickable {
                            selectedType = type
                            onApply(initialTransition.copy(type = type, durationMs = durationMs.toLong()))
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = type.label,
                        color = if (isSelected) VistaraSecondary else VistaraTextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    if (isSelected) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = VistaraSecondary)
                    }
                }
            }

            if (selectedType != TransitionType.NONE) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Transition Duration", color = VistaraTextSecondary, fontSize = 13.sp)
                    Text(
                        text = "${String.format(Locale.US, "%.1f", durationMs / 1000f)}s",
                        color = VistaraSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Slider(
                    value = durationMs,
                    onValueChange = {
                        durationMs = it
                        onApply(initialTransition.copy(type = selectedType, durationMs = it.toLong()))
                    },
                    valueRange = 200f..2000f,
                    colors = SliderDefaults.colors(thumbColor = VistaraSecondary, activeTrackColor = VistaraSecondary)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = VistaraPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("apply_transition_button")
            ) {
                Text("Done", fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMediaChoiceSheet(
    onSelectMainVideo: () -> Unit,
    onSelectOverlay: () -> Unit,
    onSelectAudio: () -> Unit,
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
                .testTag("add_media_choice_sheet")
        ) {
            Text(
                text = "Add Media to Timeline",
                color = VistaraTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Option 1: Main Video Track
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(VistaraDarkSurfaceHighlight)
                    .clickable {
                        onDismiss()
                        onSelectMainVideo()
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.Movie, contentDescription = null, tint = VistaraSecondary, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = "Add to Main Video Track", color = VistaraTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(text = "Appends clips sequentially to Video 1", color = VistaraTextMuted, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Option 2: Add as Overlay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(VistaraDarkSurfaceHighlight)
                    .clickable {
                        onDismiss()
                        onSelectOverlay()
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.Layers, contentDescription = null, tint = VistaraPrimary, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = "Add as Overlay Layer", color = VistaraTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(text = "Places image or video over existing timeline at playhead", color = VistaraTextMuted, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Option 3: Add Audio Track
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(VistaraDarkSurfaceHighlight)
                    .clickable {
                        onDismiss()
                        onSelectAudio()
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.Audiotrack, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = "Add Audio / Music Track", color = VistaraTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(text = "Places music or audio clip onto Audio track", color = VistaraTextMuted, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
