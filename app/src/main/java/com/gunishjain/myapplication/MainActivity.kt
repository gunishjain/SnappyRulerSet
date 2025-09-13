package com.gunishjain.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import com.gunishjain.myapplication.data.DrawingAction
import com.gunishjain.myapplication.data.DrawingState
import com.gunishjain.myapplication.ui.DrawingCanvas
import com.gunishjain.myapplication.ui.ToolOverlay
import com.gunishjain.myapplication.ui.theme.SnappyRulerSetTheme
import com.gunishjain.myapplication.utils.HapticFeedbackUtil
import com.gunishjain.myapplication.viewmodel.DrawingViewModel
import kotlinx.coroutines.flow.collect

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SnappyRulerSetTheme {
                SnappyRulerSetApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnappyRulerSetApp() {
    val viewModel: DrawingViewModel = remember { DrawingViewModel() }
    val drawingState by viewModel.drawingState
    
    // Get the view and context for haptic feedback
    val view = LocalView.current
    val context = LocalContext.current
    
    // Set up haptic feedback handler
    LaunchedEffect(Unit) {
        // Monitor the drawing state for haptic feedback actions
        snapshotFlow { viewModel.drawingState.value }
            .collect { state ->
                if (state.lastAction is DrawingAction.PerformHapticFeedback) {
                    HapticFeedbackUtil.performSnapHapticFeedback(view)
                    HapticFeedbackUtil.performSnapVibration(context)
                }
            }
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("SnappyRulerSet") },
                actions = {
                    // Undo button
                    IconButton(
                        onClick = { viewModel.handleAction(DrawingAction.Undo) },
                        enabled = drawingState.canUndo
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Undo",
                            tint = if (drawingState.canUndo) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            }
                        )
                    }
                    
                    // Redo button
                    IconButton(
                        onClick = { viewModel.handleAction(DrawingAction.Redo) },
                        enabled = drawingState.canRedo
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Redo",
                            tint = if (drawingState.canRedo) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            }
                        )
                    }
                    
                    // Clear button
                    TextButton(
                        onClick = { viewModel.handleAction(DrawingAction.ClearCanvas) }
                    ) {
                        Text("Clear")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tool selection overlay
            ToolOverlay(
                currentTool = drawingState.currentTool,
                onToolSelected = { tool ->
                    viewModel.handleAction(DrawingAction.SetTool(tool))
                }
            )
            
            // Drawing canvas
            DrawingCanvas(
                state = drawingState,
                snapEngine = viewModel.snapEngine,
                onAction = viewModel::handleAction,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SnappyRulerSetPreview() {
    SnappyRulerSetTheme {
        SnappyRulerSetApp()
    }
}