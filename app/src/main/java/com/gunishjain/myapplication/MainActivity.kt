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
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.gunishjain.myapplication.data.DrawingAction
import com.gunishjain.myapplication.data.DrawingState
import com.gunishjain.myapplication.data.UndoRedoManager
import com.gunishjain.myapplication.ui.DrawingCanvas
import com.gunishjain.myapplication.ui.ToolOverlay
import com.gunishjain.myapplication.ui.theme.SnappyRulerSetTheme

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
    var drawingState by remember { mutableStateOf(DrawingState()) }
    val undoRedoManager = remember { UndoRedoManager() }
    
    fun handleAction(action: DrawingAction) {
        drawingState = when (action) {
            is DrawingAction.SetTool -> {
                drawingState.copy(currentTool = action.tool)
            }
            is DrawingAction.AddElement -> {
                val newElements = drawingState.elements + action.element
                undoRedoManager.saveState(newElements)
                drawingState.copy(
                    elements = newElements,
                    canUndo = undoRedoManager.canUndo(),
                    canRedo = undoRedoManager.canRedo()
                )
            }
            is DrawingAction.StartDrawing -> {
                drawingState.copy(isDrawing = true)
            }
            is DrawingAction.UpdateDrawing -> {
                drawingState
            }
            is DrawingAction.EndDrawing -> {
                drawingState.copy(isDrawing = false)
            }
            is DrawingAction.ClearCanvas -> {
                undoRedoManager.saveState(emptyList())
                drawingState.copy(
                    elements = emptyList(),
                    canUndo = undoRedoManager.canUndo(),
                    canRedo = undoRedoManager.canRedo()
                )
            }
            is DrawingAction.SetCanvasTransform -> {
                drawingState.copy(
                    canvasOffset = action.offset,
                    canvasScale = action.scale,
                    canvasRotation = action.rotation
                )
            }
            is DrawingAction.SetStrokeColor -> {
                drawingState.copy(strokeColor = action.color)
            }
            is DrawingAction.SetStrokeWidth -> {
                drawingState.copy(strokeWidth = action.width)
            }
            is DrawingAction.ToggleSnap -> {
                drawingState.copy(snapEnabled = action.enabled)
            }
            is DrawingAction.Undo -> {
                val previousElements = undoRedoManager.undo()
                if (previousElements != null) {
                    drawingState.copy(
                        elements = previousElements,
                        canUndo = undoRedoManager.canUndo(),
                        canRedo = undoRedoManager.canRedo()
                    )
                } else {
                    drawingState
                }
            }
            is DrawingAction.Redo -> {
                val nextElements = undoRedoManager.redo()
                if (nextElements != null) {
                    drawingState.copy(
                        elements = nextElements,
                        canUndo = undoRedoManager.canUndo(),
                        canRedo = undoRedoManager.canRedo()
                    )
                } else {
                    drawingState
                }
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
                        onClick = { handleAction(DrawingAction.Undo) },
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
                        onClick = { handleAction(DrawingAction.Redo) },
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
                        onClick = { handleAction(DrawingAction.ClearCanvas) }
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
                    handleAction(DrawingAction.SetTool(tool))
                }
            )
            
            // Drawing canvas
            DrawingCanvas(
                state = drawingState,
                onAction = ::handleAction,
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