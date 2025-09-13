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
    // Track double tap for ruler line creation
    var lastTapTime by remember { mutableStateOf(0L) }
    val doubleTapThreshold = 300L // milliseconds
    val context = LocalContext.current
    val density = LocalDensity.current
    var currentPath by remember { mutableStateOf<Path?>(null) }
    
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(state.currentTool) {
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
                                        // For ruler, a tap toggles the ruler visibility or creates a line on double tap
                                        if (state.rulerTool.isVisible) {
                                            // Create a line along the ruler on double tap
                                            println("DEBUG: DrawingCanvas - Creating line along ruler")
                                            onAction(DrawingAction.AddElement(
                                                DrawingElement.LineElement(
                                                    Line(
                                                        start = state.rulerTool.startPoint,
                                                        end = state.rulerTool.endPoint,
                                                        color = state.strokeColor,
                                                        strokeWidth = state.strokeWidth
                                                    )
                                                )
                                            ))
                                            // Hide the ruler after creating the line
                                            onAction(DrawingAction.UpdateRulerTool(state.rulerTool.copy(isVisible = false)))
                                        } else {
                                            // Show the ruler at the tapped position
                                            println("DEBUG: DrawingCanvas - Showing ruler at tapped position")
                                            val rulerLength = 200f
                                            val startPoint = Point(point.x - rulerLength / 2, point.y)
                                            val endPoint = Point(point.x + rulerLength / 2, point.y)
                                            onAction(DrawingAction.UpdateRulerTool(state.rulerTool.copy(
                                                startPoint = startPoint,
                                                endPoint = endPoint,
                                                isVisible = true
                                            )))
                                        }
                            }
                            else -> { /* Other tools */ }
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val point = Point(offset.x, offset.y)
                        println("DEBUG: DrawingCanvas - onDragStart - Current tool: ${state.currentTool.name}")
                        println("DEBUG: DrawingCanvas - onDragStart - Ruler visibility: ${state.rulerTool.isVisible}")
                        
                        when (state.currentTool) {
                            DrawingTool.Freehand -> {
                                println("DEBUG: DrawingCanvas - onDragStart - Handling Freehand tool drag")
                                onAction(DrawingAction.StartDrawing(point))
                                currentPath = Path().apply { moveTo(offset.x, offset.y) }
                            }
                            DrawingTool.Ruler -> {
                                println("DEBUG: DrawingCanvas - onDragStart - Ruler tool drag - isVisible: ${state.rulerTool.isVisible}")
                                if (state.rulerTool.isVisible) {
                                    onAction(DrawingAction.StartRulerDrag(point))
                                } else {
                                    println("DEBUG: DrawingCanvas - onDragStart - Ruler not visible, initializing")
                                    // Initialize ruler if not visible
                                    val rulerLength = 200f
                                    val startPoint = Point(offset.x - rulerLength / 2, offset.y)
                                    val endPoint = Point(offset.x + rulerLength / 2, offset.y)
                                    val newRuler = state.rulerTool.copy(
                                        startPoint = startPoint,
                                        endPoint = endPoint,
                                        isVisible = true,
                                        isDragging = true
                                    )
                                    onAction(DrawingAction.UpdateRulerTool(newRuler))
                                }
                            }
                            else -> { /* Other tools */ }
                          }
                      },
                      onDrag = { change, _ ->
                        val point = Point(change.position.x, change.position.y)
                        when (state.currentTool) {
                            DrawingTool.Freehand -> {
                                onAction(DrawingAction.UpdateDrawing(point))
                                currentPath?.quadraticTo(
                                    change.previousPosition.x,
                                    change.previousPosition.y,
                                    change.position.x,
                                    change.position.y
                                )
                            }
                            DrawingTool.Ruler -> {
                                if (state.rulerTool.isVisible) {
                                    // Apply snapping if enabled
                                    val snapResult = if (state.snapEnabled) {
                                        snapEngine.findBestSnapTarget(point, state.elements)
                                    } else null
                                    
                                    // Use snapped point if available
                                    val finalPoint = snapResult?.snappedPoint ?: point
                                    
                                    // Trigger haptic feedback if snapped
                                    if (snapResult != null) {
                                        onAction(DrawingAction.PerformHapticFeedback)
                                    }
                                    
                                    onAction(DrawingAction.UpdateRulerDrag(finalPoint))
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
                                if (state.rulerTool.isVisible) {
                                    onAction(DrawingAction.EndRulerDrag(Point(0f, 0f)))
                                }
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
                
                // Draw ruler tool
                if (state.currentTool == DrawingTool.Ruler && state.rulerTool.isVisible) {
                    println("DEBUG: Drawing ruler tool - visible: ${state.rulerTool.isVisible}, start: ${state.rulerTool.startPoint}, end: ${state.rulerTool.endPoint}")
                    state.rulerTool.draw(this)
                    
                    // Draw snap indicators if snap is enabled
                    if (state.snapEnabled) {
                        val currentPoint = state.rulerTool.endPoint
                        val snapTargets = snapEngine.generateSnapTargets(state.elements)
                        snapEngine.drawSnapIndicators(this, currentPoint, snapTargets)
                    }
                }
            }
        }
        
        // Add PrecisionHUD overlay when ruler is visible
        if (state.currentTool == DrawingTool.Ruler && state.rulerTool.isVisible) {
            PrecisionHUD(
                rulerTool = state.rulerTool,
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
