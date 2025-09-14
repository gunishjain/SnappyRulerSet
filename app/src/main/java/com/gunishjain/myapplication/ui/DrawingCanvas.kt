package com.gunishjain.myapplication.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.gunishjain.myapplication.data.DrawingAction
import com.gunishjain.myapplication.data.DrawingState
import com.gunishjain.myapplication.drawing.PrecisionHUD
import com.gunishjain.myapplication.drawing.SnapEngine
import com.gunishjain.myapplication.drawing.tool.RulerTool
import com.gunishjain.myapplication.model.*
import kotlinx.coroutines.delay


/**
 * Main drawing canvas composable that handles all drawing operations
 * Includes support for ruler-based line drawing and precision measurements
 */
@Composable
fun DrawingCanvas(
    state: DrawingState,
    snapEngine: SnapEngine,
    onAction: (DrawingAction) -> Unit,
    modifier: Modifier = Modifier
) {
    // Debug: Log state changes
    LaunchedEffect(state.currentTool) {
        println("DEBUG: DrawingCanvas - Received state with tool: ${state.currentTool.name}")
    }
    // Track double tap for ruler line creation
    var lastTapTime by remember { mutableStateOf(0L) }
    val doubleTapThreshold = 300L // milliseconds
    val context = LocalContext.current
    val density = LocalDensity.current
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var rulerStartPoint by remember { mutableStateOf<Point?>(null) }
    
    // Clear ruler state when switching away from ruler tool
    LaunchedEffect(state.currentTool) {
        if (state.currentTool != DrawingTool.Ruler) {
            rulerStartPoint = null
        }
    }
    
    // Debug: Log current tool changes
    LaunchedEffect(state.currentTool) {
        println("DEBUG: DrawingCanvas - Tool changed to: ${state.currentTool.name}")
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        val point = Point(offset.x, offset.y)
                        println("DEBUG: DrawingCanvas - Current tool: ${state.currentTool.name}")
                        println("DEBUG: DrawingCanvas - Ruler visible: ${state.rulerTool.isVisible}")
                        
                        when (state.currentTool) {
                            DrawingTool.Freehand -> {
                                println("DEBUG: DrawingCanvas - Freehand tool tap handling")
                                // For freehand, a tap creates a dot
                                onAction(DrawingAction.AddElement(
                                    DrawingElement.CircleElement(
                                        Circle(
                                            center = point,
                                            radius = state.strokeWidth / 2,
                                            color = state.strokeColor,
                                            strokeWidth = state.strokeWidth
                                        )
                                    )
                                ))
                            }
                            DrawingTool.Ruler -> {
                                        println("DEBUG: DrawingCanvas - Ruler tool tap handling")
                                        // For ruler, a tap creates a dot (same as freehand)
                                        onAction(DrawingAction.AddElement(
                                            DrawingElement.CircleElement(
                                                Circle(
                                                    center = point,
                                                    radius = state.strokeWidth / 2,
                                                    color = state.strokeColor,
                                                    strokeWidth = state.strokeWidth
                                                )
                                            )
                                        ))
                            }
                            else -> { /* Other tools */ }
                        }
                    }
                )
            }
            .pointerInput(state.currentTool) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val point = Point(offset.x, offset.y)
                        println("DEBUG: DrawingCanvas - onDragStart - Current tool: ${state.currentTool.name}")
                        println("DEBUG: DrawingCanvas - onDragStart - Ruler visibility: ${state.rulerTool.isVisible}")
                        println("DEBUG: DrawingCanvas - onDragStart - State tool: ${state.currentTool}")
                        println("DEBUG: DrawingCanvas - onDragStart - Tool comparison: ${state.currentTool == DrawingTool.Ruler}")
                        
                        when (state.currentTool) {
                            DrawingTool.Freehand -> {
                                println("DEBUG: DrawingCanvas - onDragStart - Handling Freehand tool drag")
                                println("DEBUG: DrawingCanvas - Freehand - Current path before: $currentPath")
                                onAction(DrawingAction.StartDrawing(point))
                                currentPath = Path().apply { moveTo(offset.x, offset.y) }
                                println("DEBUG: DrawingCanvas - Freehand - Current path after: $currentPath")
                            }
                            DrawingTool.Ruler -> {
                                println("DEBUG: DrawingCanvas - onDragStart - Ruler tool drag - Current tool: ${state.currentTool.name}")
                                // For ruler, start drawing a straight line
                                onAction(DrawingAction.StartDrawing(point))
                                rulerStartPoint = point
                                currentPath = Path().apply { moveTo(offset.x, offset.y) }
                                println("DEBUG: DrawingCanvas - Ruler start point set: $rulerStartPoint")
                            }
                            else -> { 
                                println("DEBUG: DrawingCanvas - onDragStart - Other tool: ${state.currentTool.name}")
                            }
                          }
                      },
                      onDrag = { change, _ ->
                        val point = Point(change.position.x, change.position.y)
                        when (state.currentTool) {
                            DrawingTool.Freehand -> {
                                println("DEBUG: DrawingCanvas - Freehand onDrag - Current path: $currentPath")
                                onAction(DrawingAction.UpdateDrawing(point))
                                currentPath?.quadraticTo(
                                    change.previousPosition.x,
                                    change.previousPosition.y,
                                    change.position.x,
                                    change.position.y
                                )
                            }
                            DrawingTool.Ruler -> {
                                // For ruler, create straight lines with snapping
                                onAction(DrawingAction.UpdateDrawing(point))
                                
                                // Create a straight line from start to current position
                                rulerStartPoint?.let { startPoint ->
                                    val newPath = Path()
                                    newPath.moveTo(startPoint.x, startPoint.y)
                                    newPath.lineTo(change.position.x, change.position.y)
                                    currentPath = newPath
                                }
                            }
                            else -> { /* Other tools */ }
                        }
                    },
                    onDragEnd = {
                        when (state.currentTool) {
                            DrawingTool.Freehand -> {
                                currentPath?.let { path ->
                                    onAction(DrawingAction.AddElement(
                                        DrawingElement.StrokeElement(
                                            com.gunishjain.myapplication.model.Stroke(
                                                path = path,
                                                color = state.strokeColor,
                                                strokeWidth = state.strokeWidth
                                            )
                                        )
                                    ))
                                }
                                currentPath = null
                            }
                            DrawingTool.Ruler -> {
                                // For ruler, end drawing and create the line element
                                currentPath?.let { path ->
                                    onAction(DrawingAction.AddElement(
                                        DrawingElement.StrokeElement(
                                            com.gunishjain.myapplication.model.Stroke(
                                                path = path,
                                                color = state.strokeColor,
                                                strokeWidth = state.strokeWidth
                                            )
                                        )
                                    ))
                                }
                                currentPath = null
                                rulerStartPoint = null
                            }
                            else -> { /* Other tools */ }
                        }
                    }
                )
            }
        ) {
            // Apply canvas transformations
            withTransform({
                translate(state.canvasOffset.x, state.canvasOffset.y)
                scale(state.canvasScale, state.canvasScale)
                rotate(state.canvasRotation)
            }) {
                // Draw grid if snap is enabled
                if (state.snapEnabled) {
                    snapEngine.drawGrid(this, size)
                }
                
                // Draw all existing elements
                state.elements.forEach { element ->
                    drawElement(element)
                }
                
                // Draw current path being drawn
                currentPath?.let { path ->
                    drawPath(
                        path = path,
                        color = state.strokeColor,
                        style = Stroke(
                            width = state.strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
                
                // Ruler tool now draws straight lines directly, no visible ruler object
            }
        }
        
        // Add PrecisionHUD overlay when ruler tool is selected and drawing
        if (state.currentTool == DrawingTool.Ruler && state.isDrawing && rulerStartPoint != null) {
            // We need to get the current position from the currentPath
            val currentEndPoint = if (currentPath != null) {
                val bounds = currentPath!!.getBounds()
                Point(bounds.right, bounds.bottom)
            } else {
                rulerStartPoint!!
            }
            
            // Create a temporary ruler tool for HUD display with actual points
            val tempRuler = RulerTool(
                startPoint = rulerStartPoint!!,
                endPoint = currentEndPoint,
                isVisible = true
            )
            PrecisionHUD(
                rulerTool = tempRuler,
                isVisible = true
            )
        }
    }
}

/**
 * Draws a drawing element on the canvas
 */
private fun DrawScope.drawElement(element: DrawingElement) {
    when (element) {
        is DrawingElement.LineElement -> {
            drawLine(
                color = element.line.color,
                start = element.line.start.toOffset(),
                end = element.line.end.toOffset(),
                strokeWidth = element.line.strokeWidth
            )
        }
        is DrawingElement.StrokeElement -> {
            drawPath(
                path = element.stroke.path,
                color = element.stroke.color,
                style = Stroke(
                    width = element.stroke.strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
        is DrawingElement.AngleElement -> {
            // TODO: Implement angle drawing
        }
        is DrawingElement.CircleElement -> {
            drawCircle(
                color = element.circle.color,
                radius = element.circle.radius,
                center = element.circle.center.toOffset(),
                style = Stroke(
                    width = element.circle.strokeWidth,
                    cap = StrokeCap.Round
                )
            )
        }
    }
}
