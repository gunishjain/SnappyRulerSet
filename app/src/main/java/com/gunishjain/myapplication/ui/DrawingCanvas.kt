package com.gunishjain.myapplication.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.gunishjain.myapplication.data.DrawingAction
import com.gunishjain.myapplication.data.DrawingState
import com.gunishjain.myapplication.drawing.SnapEngine
import com.gunishjain.myapplication.drawing.tool.RulerTool
import com.gunishjain.myapplication.drawing.tool.CompassTool
import com.gunishjain.myapplication.model.*
import kotlinx.coroutines.delay
import kotlin.math.*


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
    var currentPathPoints by remember { mutableStateOf<List<Point>>(emptyList()) }
    var rulerStartPoint by remember { mutableStateOf<Point?>(null) }
    var compassStartPoint by remember { mutableStateOf<Point?>(null) }
    var currentCompass by remember { mutableStateOf<CompassTool?>(null) }
    
    // Clear ruler state when switching away from ruler tool
    LaunchedEffect(state.currentTool) {
        if (state.currentTool != DrawingTool.Ruler) {
            rulerStartPoint = null
        }
        if (state.currentTool != DrawingTool.Compass) {
            compassStartPoint = null
            currentCompass = null
        }
        // Clear path state when switching tools
        currentPath = null
        currentPathPoints = emptyList()
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
                                
                                // For freehand, a tap creates a dot - no snapping for natural feel
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
                                        
                                        // For ruler, a tap creates a dot (same as freehand)
                                        onAction(DrawingAction.AddElement(
                                            DrawingElement.CircleElement(
                                                Circle(
                                                    center = finalPoint,
                                                    radius = state.strokeWidth / 2,
                                                    color = state.strokeColor,
                                                    strokeWidth = state.strokeWidth
                                                )
                                            )
                                        ))
                            }
                            DrawingTool.Compass -> {
                                println("DEBUG: DrawingCanvas - Compass tool tap handling")
                                // Compass tool - tap does nothing, only drag works
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
                                currentPathPoints = listOf(point) // Start with first point
                                println("DEBUG: DrawingCanvas - Freehand - Current path after: $currentPath")
                            }
                            DrawingTool.Ruler -> {
                                println("DEBUG: DrawingCanvas - onDragStart - Ruler tool drag - Current tool: ${state.currentTool.name}")
                                // For ruler, start drawing a straight line
                                onAction(DrawingAction.StartDrawing(point))
                                rulerStartPoint = point
                                currentPath = Path().apply { moveTo(offset.x, offset.y) }
                                
                                // Initialize the ruler tool state for PrecisionHUD
                                val initialRuler = RulerTool(
                                    startPoint = point,
                                    endPoint = point,
                                    isVisible = true
                                )
                                onAction(DrawingAction.UpdateRulerTool(initialRuler))
                                
                                println("DEBUG: DrawingCanvas - Ruler start point set: $rulerStartPoint")
                            }
                            DrawingTool.Compass -> {
                                println("DEBUG: DrawingCanvas - onDragStart - Compass tool drag - Current tool: ${state.currentTool.name}")
                                // For compass, start drawing a circle
                                onAction(DrawingAction.StartDrawing(point))
                                compassStartPoint = point
                                // Initialize local compass state
                                currentCompass = CompassTool(
                                    center = point,
                                    radius = 0f,
                                    isVisible = true,
                                    isDrawing = true
                                )
                                onAction(DrawingAction.UpdateCompassTool(currentCompass!!))
                                println("DEBUG: DrawingCanvas - Compass start point set: $point")
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
                                
                                // Freehand drawing - no snapping for smooth, natural lines
                                onAction(DrawingAction.UpdateDrawing(point))
                                currentPath?.quadraticTo(
                                    change.previousPosition.x,
                                    change.previousPosition.y,
                                    change.position.x,
                                    change.position.y
                                )
                                
                                // Add point to our path points list for export
                                currentPathPoints = currentPathPoints + point
                            }
                            DrawingTool.Ruler -> {
                                // For ruler, create straight lines with snapping
                                
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
                                
                                onAction(DrawingAction.UpdateDrawing(finalPoint))
                                
                                // Create a straight line from start to current position
                                rulerStartPoint?.let { startPoint ->
                                    val newPath = Path()
                                    newPath.moveTo(startPoint.x, startPoint.y)
                                    newPath.lineTo(finalPoint.x, finalPoint.y)
                                    currentPath = newPath
                                    
                                    // Update the global ruler tool state for PrecisionHUD
                                    val updatedRuler = RulerTool(
                                        startPoint = startPoint,
                                        endPoint = finalPoint,
                                        isVisible = true
                                    )
                                    onAction(DrawingAction.UpdateRulerTool(updatedRuler))
                                }
                            }
                            DrawingTool.Compass -> {
                                // Compass tool - drag to adjust radius and draw circle
                                println("DEBUG: DrawingCanvas - Compass onDrag - isDrawing: ${state.compassTool.isDrawing}")
                                println("DEBUG: DrawingCanvas - Compass onDrag - current radius: ${state.compassTool.radius}")
                                println("DEBUG: DrawingCanvas - Compass onDrag - center: ${state.compassTool.center}")
                                
                                // Always update compass during drag if we're in compass mode
                                val snapResult = if (state.snapEnabled) {
                                    snapEngine.findBestSnapTarget(point, state.elements)
                                } else null
                                
                                val finalPoint = snapResult?.snappedPoint ?: point
                                
                                if (snapResult != null) {
                                    onAction(DrawingAction.PerformHapticFeedback)
                                }
                                
                                // Use the center from drag start (stored in compassStartPoint)
                                val currentCenter = compassStartPoint ?: point
                                
                                val updatedCompass = CompassTool(
                                    center = currentCenter,
                                    radius = sqrt((finalPoint.x - currentCenter.x).pow(2) + (finalPoint.y - currentCenter.y).pow(2)),
                                    isVisible = true,
                                    isDrawing = true
                                )
                                
                                // Update local state
                                currentCompass = updatedCompass
                                
                                println("DEBUG: DrawingCanvas - Compass onDrag - new radius: ${updatedCompass.radius}")
                                println("DEBUG: DrawingCanvas - Compass onDrag - new center: ${updatedCompass.center}")
                                onAction(DrawingAction.UpdateCompassTool(updatedCompass))
                            }
                            else -> { /* Other tools */ }
                        }
                    },
                    onDragEnd = {
                        when (state.currentTool) {
                            DrawingTool.Freehand -> {
                                currentPath?.let { path ->
                                    // Create both StrokeElement (for display) and StrokeWithPointsElement (for export)
                                    onAction(DrawingAction.AddElement(
                                        DrawingElement.StrokeElement(
                                            com.gunishjain.myapplication.model.Stroke(
                                                path = path,
                                                color = state.strokeColor,
                                                strokeWidth = state.strokeWidth
                                            )
                                        )
                                    ))
                                    
                                    // Also add the points-based version for export
                                    if (currentPathPoints.isNotEmpty()) {
                                        onAction(DrawingAction.AddElement(
                                            DrawingElement.StrokeWithPointsElement(
                                                StrokeWithPoints(
                                                    points = currentPathPoints,
                                                    color = state.strokeColor,
                                                    strokeWidth = state.strokeWidth
                                                )
                                            )
                                        ))
                                    }
                                }
                                currentPath = null
                                currentPathPoints = emptyList()
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
                                
                                // Clear the ruler tool state
                                onAction(DrawingAction.UpdateRulerTool(
                                    RulerTool(isVisible = false)
                                ))
                            }
                            DrawingTool.Compass -> {
                                // End compass drawing and create circle element
                                println("DEBUG: DrawingCanvas - Compass onDragEnd - isDrawing: ${state.compassTool.isDrawing}")
                                println("DEBUG: DrawingCanvas - Compass onDragEnd - radius: ${state.compassTool.radius}")
                                
                                // Use the local compass state from the drag
                                val lastCompass = currentCompass
                                if (lastCompass != null && lastCompass.isDrawing && lastCompass.radius > 0) {
                                    val path = lastCompass.createCirclePath()
                                    println("DEBUG: DrawingCanvas - Compass onDragEnd - Creating circle element with radius: ${lastCompass.radius}")
                                    
                                    onAction(DrawingAction.AddElement(
                                        DrawingElement.StrokeElement(
                                            com.gunishjain.myapplication.model.Stroke(
                                                path = path,
                                                color = state.strokeColor,
                                                strokeWidth = state.strokeWidth
                                            )
                                        )
                                    ))
                                    println("DEBUG: DrawingCanvas - Compass onDragEnd - Circle element added to canvas")
                                } else {
                                    println("DEBUG: DrawingCanvas - Compass onDragEnd - NOT creating circle - isDrawing: ${lastCompass?.isDrawing}, radius: ${lastCompass?.radius}")
                                }
                                
                                // Reset compass
                                onAction(DrawingAction.UpdateCompassTool(
                                    CompassTool(isVisible = false, isDrawing = false)
                                ))
                                onAction(DrawingAction.EndDrawing(Point(0f, 0f)))
                                compassStartPoint = null
                                currentCompass = null
                                println("DEBUG: DrawingCanvas - Compass onDragEnd - Compass reset")
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
                // Draw grid always visible
                snapEngine.drawGrid(this, size)
                
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
                
                // Draw Compass tool only when actively drawing and radius > 0
                if (currentCompass != null && currentCompass!!.isVisible && currentCompass!!.isDrawing && currentCompass!!.radius > 0 && state.isDrawing) {
                    println("DEBUG: DrawingCanvas - Drawing compass - radius: ${currentCompass!!.radius}, center: ${currentCompass!!.center}")
                    // Draw circle outline with current stroke color and width
                    drawCircle(
                        color = state.strokeColor.copy(alpha = 0.5f),
                        radius = currentCompass!!.radius,
                        center = Offset(currentCompass!!.center.x, currentCompass!!.center.y),
                        style = Stroke(
                            width = state.strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                } else {
                    println("DEBUG: DrawingCanvas - NOT drawing compass - isVisible: ${currentCompass?.isVisible}, isDrawing: ${currentCompass?.isDrawing}, radius: ${currentCompass?.radius}, state.isDrawing: ${state.isDrawing}")
                }
                
                // Draw snap indicators if snap is enabled and we're drawing
                if (state.snapEnabled && state.isDrawing) {
                    val currentPoint = if (state.currentTool == DrawingTool.Ruler && rulerStartPoint != null) {
                        val bounds = currentPath?.getBounds()
                        if (bounds != null) Point(bounds.right, bounds.bottom) else rulerStartPoint!!
                    } else {
                        // For freehand, use the last drawing point
                        Point(0f, 0f) // This will be updated with actual current position
                    }
                    
                    val snapTargets = snapEngine.generateSnapTargets(state.elements)
                    snapEngine.drawSnapIndicators(this, currentPoint, snapTargets)
                }
                
                // Update snap targets for precision tools (Ruler, etc.)
                if (state.snapEnabled) {
                    snapEngine.updateSnapTargets(state.elements, size.width, size.height)
                }
            }
        }
        
        // PrecisionHUD is now handled in MainActivity to avoid overlap issues
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

        is DrawingElement.StrokeWithPointsElement -> {
            // Create a path from the points and draw it
            val path = Path()
            if (element.stroke.points.isNotEmpty()) {
                path.moveTo(element.stroke.points[0].x, element.stroke.points[0].y)
                for (i in 1 until element.stroke.points.size) {
                    path.lineTo(element.stroke.points[i].x, element.stroke.points[i].y)
                }
            }
            drawPath(
                path = path,
                color = element.stroke.color,
                style = Stroke(
                    width = element.stroke.strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}
