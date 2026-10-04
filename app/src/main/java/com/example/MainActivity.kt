package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.editor.viewmodel.EditorViewModel
import com.example.editor.viewmodel.HomeViewModel
import com.example.ui.editor.EditorScreen
import com.example.ui.home.HomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.VistaraDarkBackground

sealed class Screen {
    object Home : Screen()
    data class Editor(val projectId: String) : Screen()
}

class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()
    private val editorViewModel: EditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = VistaraDarkBackground
                ) {
                    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "screen_navigation"
                    ) { screen ->
                        when (screen) {
                            is Screen.Home -> {
                                HomeScreen(
                                    viewModel = homeViewModel,
                                    onOpenProject = { projectId ->
                                        currentScreen = Screen.Editor(projectId)
                                    }
                                )
                            }
                            is Screen.Editor -> {
                                EditorScreen(
                                    projectId = screen.projectId,
                                    viewModel = editorViewModel,
                                    onNavigateBack = {
                                        editorViewModel.timelinePlayer.pause()
                                        currentScreen = Screen.Home
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
