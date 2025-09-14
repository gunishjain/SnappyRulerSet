package com.gunishjain.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.ui.Alignment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import com.gunishjain.myapplication.data.DrawingAction
import com.gunishjain.myapplication.data.DrawingState
import com.gunishjain.myapplication.drawing.PrecisionHUD
import com.gunishjain.myapplication.drawing.CompassPrecisionHUD
import com.gunishjain.myapplication.drawing.tool.RulerTool
import com.gunishjain.myapplication.model.DrawingTool
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
                    
                    // Snap toggle button
                    IconButton(
                        onClick = { 
                            viewModel.handleAction(DrawingAction.ToggleSnap(!drawingState.snapEnabled))
                        }
                    ) {
                        Icon(
                            imageVector = if (drawingState.snapEnabled) Icons.Default.Check else Icons.Default.Clear,
                            contentDescription = if (drawingState.snapEnabled) "Disable Snap" else "Enable Snap",
                            tint = if (drawingState.snapEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            }
                        )
                    }
                    
                    // Grid spacing dropdown (always visible)
                    var expanded by remember { mutableStateOf(false) }
                    val gridSpacingOptions = listOf(10f, 20f, 30f, 40f, 50f)
                    
                    Box {
                        TextButton(
                            onClick = { expanded = true }
                        ) {
                            Text("Grid: ${drawingState.gridSpacing.toInt()}px")
                        }
                        
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            gridSpacingOptions.forEach { spacing ->
                                DropdownMenuItem(
                                    text = { Text("${spacing.toInt()}px") },
                                    onClick = {
                                        viewModel.handleAction(DrawingAction.SetGridSpacing(spacing))
                                        expanded = false
                                    }
                                )
                            }
                        }
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Drawing canvas - covers entire area
            DrawingCanvas(
                state = drawingState,
                snapEngine = viewModel.snapEngine,
                onAction = viewModel::handleAction,
                modifier = Modifier.fillMaxSize()
            )
            
            // Tool selection overlay - positioned on top
            ToolOverlay(
                currentTool = drawingState.currentTool,
                onToolSelected = { tool ->
                    viewModel.handleAction(DrawingAction.SetTool(tool))
                },
                modifier = Modifier.align(Alignment.TopCenter)
            )
            
            // Precision HUD - positioned on top right, above ToolOverlay
            if (drawingState.currentTool == DrawingTool.Ruler && drawingState.isDrawing) {
                // We need to get the current position from the drawing state
                val currentEndPoint = if (drawingState.rulerTool.isVisible) {
                    drawingState.rulerTool.endPoint
                } else {
                    drawingState.rulerTool.startPoint
                }
                
                // Create a temporary ruler tool for HUD display
                val tempRuler = RulerTool(
                    startPoint = drawingState.rulerTool.startPoint,
                    endPoint = currentEndPoint,
                    isVisible = true
                )
                PrecisionHUD(
                    rulerTool = tempRuler,
                    isVisible = true,
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }
            
            // Precision HUD for Compass tool - show radius
            if (drawingState.currentTool == DrawingTool.Compass && drawingState.isDrawing && drawingState.compassTool.isDrawing) {
                CompassPrecisionHUD(
                    compassTool = drawingState.compassTool,
                    isVisible = true,
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }
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