package com.example.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.common.TimeUtils
import com.example.domain.model.Project
import com.example.editor.viewmodel.HomeViewModel
import com.example.ui.theme.VistaraDarkBackground
import com.example.ui.theme.VistaraDarkSurface
import com.example.ui.theme.VistaraDarkSurfaceBorder
import com.example.ui.theme.VistaraDarkSurfaceHighlight
import com.example.ui.theme.VistaraPrimary
import com.example.ui.theme.VistaraPrimaryVariant
import com.example.ui.theme.VistaraSecondary
import com.example.ui.theme.VistaraTextMuted
import com.example.ui.theme.VistaraTextPrimary
import com.example.ui.theme.VistaraTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenProject: (String) -> Unit
) {
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isCreating by viewModel.isCreatingProject.collectAsStateWithLifecycle()

    var projectToRename by remember { mutableStateOf<Project?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var projectToDelete by remember { mutableStateOf<Project?>(null) }

    // Media Picker for New Project
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.createProjectFromMedia(uris) { newProjectId ->
                onOpenProject(newProjectId)
            }
        }
    }

    Scaffold(
        containerColor = VistaraDarkBackground,
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("home_screen")
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Branding
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(VistaraSecondary)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "VISTARA EDIT",
                                    color = VistaraTextPrimary,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.2.sp
                                )
                            }
                            Text(
                                text = "Pro Mobile Video Studio",
                                color = VistaraTextMuted,
                                fontSize = 12.sp
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = VistaraDarkSurfaceHighlight,
                            modifier = Modifier.height(28.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            ) {
                                Text(
                                    text = "${projects.size} Projects",
                                    color = VistaraTextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // New Project Hero Action Card
                item {
                    Surface(
                        onClick = {
                            mediaPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .testTag("create_new_project_card")
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            VistaraPrimary,
                                            VistaraPrimaryVariant.copy(alpha = 0.85f),
                                            Color(0xFF4C1D95)
                                        )
                                    )
                                )
                                .padding(20.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "+ New Project",
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Import videos & photos to edit",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 13.sp
                                    )
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = Color.White,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "New Project",
                                            tint = VistaraPrimary,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Search Bar
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.searchQuery.value = it },
                        placeholder = { Text("Search projects…", color = VistaraTextMuted) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = VistaraTextMuted
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = VistaraDarkSurface,
                            unfocusedContainerColor = VistaraDarkSurface,
                            focusedBorderColor = VistaraSecondary,
                            unfocusedBorderColor = VistaraDarkSurfaceBorder,
                            focusedTextColor = VistaraTextPrimary,
                            unfocusedTextColor = VistaraTextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("search_projects_input")
                    )
                }

                // Section Title: My Projects
                item {
                    Text(
                        text = "Projects",
                        color = VistaraTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Projects List or Empty State
                if (projects.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp)
                                .testTag("empty_projects_view"),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.MovieCreation,
                                contentDescription = null,
                                tint = VistaraTextMuted.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (searchQuery.isBlank()) "No Projects Yet" else "No matching projects",
                                color = VistaraTextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Import photos or videos to begin editing",
                                color = VistaraTextSecondary,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = {
                                    mediaPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = VistaraPrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("empty_new_project_button")
                            ) {
                                Text("Create New Project", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    items(projects, key = { it.id }) { project ->
                        ProjectListItem(
                            project = project,
                            onClick = { onOpenProject(project.id) },
                            onRename = {
                                projectToRename = project
                                renameInput = project.name
                            },
                            onDuplicate = { viewModel.duplicateProject(project.id) },
                            onDelete = { projectToDelete = project }
                        )
                    }
                }
            }

            // Loading indicator when processing media import
            if (isCreating) {
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = VistaraSecondary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Importing media & preparing project…",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Rename Dialog
    projectToRename?.let { project ->
        AlertDialog(
            onDismissRequest = { projectToRename = null },
            title = { Text("Rename Project", color = VistaraTextPrimary) },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VistaraSecondary,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rename_input_field")
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.renameProject(project.id, renameInput)
                        projectToRename = null
                    },
                    modifier = Modifier.testTag("rename_confirm_button")
                ) {
                    Text("Rename", color = VistaraSecondary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToRename = null }) {
                    Text("Cancel", color = VistaraTextSecondary)
                }
            },
            containerColor = VistaraDarkSurface
        )
    }

    // Delete Confirmation Dialog
    projectToDelete?.let { project ->
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            title = { Text("Delete Project?", color = VistaraTextPrimary) },
            text = {
                Text(
                    "Are you sure you want to delete '${project.name}'? This cannot be undone.",
                    color = VistaraTextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteProject(project.id)
                        projectToDelete = null
                    },
                    modifier = Modifier.testTag("delete_confirm_button")
                ) {
                    Text("Delete", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToDelete = null }) {
                    Text("Cancel", color = VistaraTextSecondary)
                }
            },
            containerColor = VistaraDarkSurface
        )
    }
}

@Composable
private fun ProjectListItem(
    project: Project,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val formattedDate = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
        .format(Date(project.updatedAt))

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = VistaraDarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, VistaraDarkSurfaceBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("project_item_${project.id}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail / Icon
            val firstAsset = project.assets.firstOrNull()
            Box(
                modifier = Modifier
                    .size(width = 84.dp, height = 56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(VistaraDarkSurfaceHighlight),
                contentAlignment = Alignment.Center
            ) {
                if (firstAsset != null && firstAsset.uriString.isNotBlank()) {
                    AsyncImage(
                        model = firstAsset.uriString,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = null,
                        tint = VistaraTextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Aspect ratio tag
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = project.canvasRatio.label,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Project Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.name,
                    color = VistaraTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = TimeUtils.formatTimeShort(project.totalDurationMs),
                        color = VistaraSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = " • $formattedDate",
                        color = VistaraTextMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Options Menu
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = VistaraTextSecondary
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(VistaraDarkSurfaceHighlight)
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename", color = VistaraTextPrimary) },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = VistaraTextSecondary) },
                        onClick = {
                            menuExpanded = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Duplicate", color = VistaraTextPrimary) },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = VistaraTextSecondary) },
                        onClick = {
                            menuExpanded = false
                            onDuplicate()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = Color.Red) },
                        leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color.Red) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}
