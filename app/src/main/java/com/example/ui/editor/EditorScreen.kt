package com.example.ui.editor

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.ClipTransform
import com.example.domain.model.ItemType
import com.example.domain.model.TransitionConfig
import com.example.editor.export.ExportState
import com.example.editor.viewmodel.EditorSheet
import com.example.editor.viewmodel.EditorViewModel
import com.example.ui.editor.export.ExportConfigDialog
import com.example.ui.editor.export.ExportStatusDialog
import com.example.ui.editor.preview.PreviewCanvas
import com.example.ui.editor.sheets.AddMediaChoiceSheet
import com.example.ui.editor.sheets.CanvasSheet
import com.example.ui.editor.sheets.SpeedSheet
import com.example.ui.editor.sheets.TextLayerSheet
import com.example.ui.editor.sheets.TransformSheet
import com.example.ui.editor.sheets.TransitionSheet
import com.example.ui.editor.sheets.VolumeSheet
import com.example.ui.editor.timeline.TimelineView
import com.example.ui.editor.toolbar.EditorToolbar
import com.example.ui.theme.VistaraDarkBackground
import com.example.ui.theme.VistaraSecondary

@Composable
fun EditorScreen(
    projectId: String,
    viewModel: EditorViewModel,
    onNavigateBack: () -> Unit
) {
    LaunchedEffect(projectId) {
        viewModel.loadProject(projectId)
    }

    val project by viewModel.project.collectAsStateWithLifecycle()
    val selectedItemId by viewModel.selectedItemId.collectAsStateWithLifecycle()
    val playheadMs by viewModel.playheadMs.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val activeSheet by viewModel.activeSheet.collectAsStateWithLifecycle()
    val autosaveStatus by viewModel.autosaveStatus.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()
    val isFullscreen by viewModel.isFullscreen.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()

    var pendingTargetTrack by remember { mutableStateOf("track_video_1") }

    BackHandler {
        onNavigateBack()
    }

    // Media Picker for adding media with track destination
    val mediaAddLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addMediaClips(uris, targetTrack = pendingTargetTrack)
        }
    }

    // Single photo picker for Image Overlay
    val overlayPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.addImageOverlay(uri)
        }
    }

    if (project == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VistaraDarkBackground),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = VistaraSecondary)
        }
        return
    }

    val currentProject = project!!
    val selectedItem = currentProject.items.find { it.id == selectedItemId }
    val isVideoSelected = selectedItem?.type == ItemType.VIDEO

    // Check if playhead is strictly inside the selected video clip for split
    val canSplit = isVideoSelected && selectedItem != null &&
            playheadMs > (selectedItem.timelineStartMs + 50) &&
            playheadMs < (selectedItem.timelineStartMs + selectedItem.durationMs - 50)

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("editor_screen"),
        topBar = {
            if (!isFullscreen) {
                EditorTopBar(
                    projectName = currentProject.name,
                    autosaveStatus = autosaveStatus,
                    canUndo = canUndo,
                    canRedo = canRedo,
                    onBack = onNavigateBack,
                    onUndo = { viewModel.undo() },
                    onRedo = { viewModel.redo() },
                    onOpenExport = { viewModel.openSheet(EditorSheet.EXPORT_CONFIG) }
                )
            }
        },
        containerColor = VistaraDarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Preview Canvas (Upper viewport)
            PreviewCanvas(
                project = currentProject,
                player = viewModel.timelinePlayer.exoPlayer,
                playheadMs = playheadMs,
                isPlaying = isPlaying,
                isFullscreen = isFullscreen,
                selectedItemId = selectedItemId,
                onTogglePlayPause = { viewModel.togglePlayPause() },
                onToggleFullscreen = { viewModel.toggleFullscreen() },
                onSeek = { viewModel.seekTo(it) },
                onJumpToStart = { viewModel.jumpToStart() },
                onJumpToEnd = { viewModel.jumpToEnd() },
                onUpdateTextProperties = { id, props -> viewModel.updateTextProperties(id, props) },
                onUpdateImageProperties = { id, props -> viewModel.updateImageProperties(id, props) },
                onSelectItem = { viewModel.selectItem(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (isFullscreen) 1f else 1.15f)
            )

            // Timeline & Toolbar (Hidden in fullscreen mode)
            if (!isFullscreen) {
                // Interactive Multi-Track Timeline
                TimelineView(
                    project = currentProject,
                    playheadMs = playheadMs,
                    selectedItemId = selectedItemId,
                    onSeek = { viewModel.seekTo(it) },
                    onSelectItem = { viewModel.selectItem(it) },
                    onTrimClip = { newStart, newDuration ->
                        viewModel.trimSelectedClip(newStart, newDuration)
                    },
                    onMoveLayer = { id, newStart ->
                        viewModel.moveLayer(id, newStart)
                    },
                    onTrimLayer = { id, newDuration ->
                        viewModel.trimLayer(id, newDuration)
                    },
                    onOpenTransition = {
                        viewModel.selectItem(it)
                        viewModel.openSheet(EditorSheet.TRANSITION)
                    },
                    onToggleSnap = { viewModel.toggleSnap() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.0f)
                )

                // Contextual Bottom Toolbar
                EditorToolbar(
                    selectedItem = selectedItem,
                    canSplit = canSplit,
                    onSplit = { viewModel.splitSelectedClip() },
                    onDelete = { viewModel.deleteSelectedItem() },
                    onDuplicate = { viewModel.duplicateSelectedItem() },
                    onOpenSpeed = { viewModel.openSheet(EditorSheet.SPEED) },
                    onOpenVolume = { viewModel.openSheet(EditorSheet.VOLUME) },
                    onOpenTransform = { viewModel.openSheet(EditorSheet.TRANSFORM) },
                    onOpenCanvas = { viewModel.openSheet(EditorSheet.CANVAS) },
                    onOpenText = {
                        if (selectedItem?.type == ItemType.TEXT) {
                            viewModel.openSheet(EditorSheet.TEXT_EDITOR)
                        } else {
                            viewModel.addTextLayer("Title", "#FFFFFF", null, 24f)
                            viewModel.openSheet(EditorSheet.TEXT_EDITOR)
                        }
                    },
                    onOpenOverlay = {
                        overlayPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onOpenTransition = { viewModel.openSheet(EditorSheet.TRANSITION) },
                    onAddMedia = { viewModel.openSheet(EditorSheet.ADD_MEDIA_CHOICE) }
                )
            }
        }
    }

    // Modal Bottom Sheets for Tools
    when (activeSheet) {
        EditorSheet.SPEED -> {
            SpeedSheet(
                currentSpeed = selectedItem?.speed ?: 1.0f,
                onApplySpeed = { viewModel.updateClipSpeed(it) },
                onDismiss = { viewModel.closeSheet() }
            )
        }

        EditorSheet.VOLUME -> {
            VolumeSheet(
                currentVolume = selectedItem?.volume ?: 1.0f,
                isMuted = selectedItem?.isMuted ?: false,
                onApplyVolume = { vol, muted ->
                    viewModel.updateClipVolume(vol, muted)
                },
                onDismiss = { viewModel.closeSheet() }
            )
        }

        EditorSheet.TRANSFORM -> {
            TransformSheet(
                initialTransform = selectedItem?.transform ?: ClipTransform(),
                onApplyTransform = { viewModel.updateClipTransform(it) },
                onDismiss = { viewModel.closeSheet() }
            )
        }

        EditorSheet.CANVAS -> {
            CanvasSheet(
                currentRatio = currentProject.canvasRatio,
                currentBgHex = currentProject.canvasBackgroundColorHex,
                onSelectRatio = { viewModel.setCanvasAspectRatio(it) },
                onSelectBgColor = { viewModel.setCanvasBackgroundColor(it) },
                onDismiss = { viewModel.closeSheet() }
            )
        }

        EditorSheet.TEXT_EDITOR -> {
            TextLayerSheet(
                initialProperties = selectedItem?.textProperties,
                onApply = { updatedProps ->
                    selectedItem?.let {
                        viewModel.updateTextProperties(it.id, updatedProps)
                    }
                },
                onDismiss = { viewModel.closeSheet() }
            )
        }

        EditorSheet.TRANSITION -> {
            TransitionSheet(
                initialTransition = selectedItem?.transition ?: TransitionConfig(),
                onApply = { viewModel.setClipTransition(it) },
                onDismiss = { viewModel.closeSheet() }
            )
        }

        EditorSheet.ADD_MEDIA_CHOICE -> {
            AddMediaChoiceSheet(
                onSelectMainVideo = {
                    pendingTargetTrack = "track_video_1"
                    mediaAddLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                },
                onSelectOverlay = {
                    pendingTargetTrack = "track_image"
                    overlayPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                },
                onSelectAudio = {
                    pendingTargetTrack = "track_audio_1"
                    mediaAddLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                },
                onDismiss = { viewModel.closeSheet() }
            )
        }

        EditorSheet.EXPORT_CONFIG -> {
            ExportConfigDialog(
                initialSettings = currentProject.exportSettings,
                onStartExport = { viewModel.startExport(it) },
                onDismiss = { viewModel.closeSheet() }
            )
        }

        EditorSheet.NONE -> {}
    }

    // Export Progress & Completion Dialog
    if (exportState !is ExportState.Idle) {
        ExportStatusDialog(
            exportState = exportState,
            onCancel = { viewModel.cancelExport() },
            onDismiss = { viewModel.resetExportState() }
        )
    }
}
