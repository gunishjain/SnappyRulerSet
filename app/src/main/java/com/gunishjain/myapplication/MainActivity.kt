package com.gunishjain.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.Alignment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gunishjain.myapplication.data.DrawingAction
import com.gunishjain.myapplication.drawing.PrecisionHUD
import com.gunishjain.myapplication.drawing.CompassPrecisionHUD
import com.gunishjain.myapplication.drawing.ProtractorPrecisionHUD
import com.gunishjain.myapplication.drawing.tool.RulerTool
import com.gunishjain.myapplication.export.BitmapExporter
import com.gunishjain.myapplication.model.DrawingTool
import com.gunishjain.myapplication.ui.DrawingCanvas
import com.gunishjain.myapplication.ui.ExportDialog
import com.gunishjain.myapplication.ui.ToolOverlay
import com.gunishjain.myapplication.ui.theme.SnappyRulerSetTheme
import com.gunishjain.myapplication.utils.HapticFeedbackUtil
import com.gunishjain.myapplication.utils.PermissionHandler
import com.gunishjain.myapplication.utils.rememberPermissionHandler
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
    
    // Export dialog state
    var showExportDialog by remember { mutableStateOf(false) }
    var showExportResult by remember { mutableStateOf(false) }
    var exportResultMessage by remember { mutableStateOf("") }
    var exportFormat by remember { mutableStateOf<BitmapExporter.ExportFormat?>(null) }
    var exportQuality by remember { mutableStateOf(90) }
    
    // Permission handler for export
    val requestExport = rememberPermissionHandler(
        onPermissionGranted = {
            showExportDialog = true
        },
        onPermissionDenied = {
            exportResultMessage = "Permission denied. Cannot export drawing."
            showExportResult = true
        }
    )
    
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
    
    // Handle export when format is selected
    LaunchedEffect(exportFormat) {
        exportFormat?.let { format ->
            try {
                val result = BitmapExporter.exportDrawing(
                    context = context,
                    drawingState = drawingState,
                    snapEngine = viewModel.snapEngine,
                    canvasWidth = 1080, // Default canvas width
                    canvasHeight = 1920, // Default canvas height
                    format = format,
                    quality = exportQuality
                )
                exportResultMessage = result.getOrThrow()
                showExportResult = true
                exportFormat = null // Reset
            } catch (e: Exception) {
                exportResultMessage = "Export failed: ${e.message}"
                showExportResult = true
                exportFormat = null // Reset
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
            
            // Precision HUD for Protractor tool - show angle and line lengths
            if (drawingState.currentTool == DrawingTool.Protractor && drawingState.protractorTool.isVisible) {
                ProtractorPrecisionHUD(
                    protractorTool = drawingState.protractorTool,
                    isVisible = true,
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }
            
            // Floating Action Button for export
            FloatingActionButton(
                onClick = { requestExport() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Export Drawing"
                )
            }
        }
    }
    
    // Export format selection dialog
    if (showExportDialog) {
        ExportDialog(
            onDismiss = { showExportDialog = false },
            onExport = { format, quality ->
                showExportDialog = false
                exportFormat = format
                exportQuality = quality
            }
        )
    }
    
    // Export result dialog
    if (showExportResult) {
        AlertDialog(
            onDismissRequest = { showExportResult = false },
            title = { Text("Export Result") },
            text = { Text(exportResultMessage) },
            confirmButton = {
                TextButton(
                    onClick = { showExportResult = false }
                ) {
                    Text("OK")
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SnappyRulerSetPreview() {
    SnappyRulerSetTheme {
        SnappyRulerSetApp()
    }
}