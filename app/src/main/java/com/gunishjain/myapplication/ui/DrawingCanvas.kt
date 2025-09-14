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
import com.gunishjain.myapplication.drawing.tool.ProtractorTool
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
    var protractorStartPoint by remember { mutableStateOf<Point?>(null) }
    var currentProtractor by remember { mutableStateOf<ProtractorTool?>(null) }
    
    // Clear ruler state when switching away from ruler tool
    LaunchedEffect(state.currentTool) {
        if (state.currentTool != DrawingTool.Ruler) {
            rulerStartPoint = null
        }
        if (state.currentTool != DrawingTool.Compass) {
            compassStartPoint = null
            currentCompass = null
        }
        if (state.currentTool != DrawingTool.Protractor) {
            // If protractor has both lines complete, add them as permanent drawing elements
            val currentProtractorState = currentProtractor
            if (currentProtractorState != null && 
                currentProtractorState.firstLineComplete && 
                currentProtractorState.secondLineComplete) {
                
                // Create first line element
                val firstLinePath = Path().apply {
                    moveTo(currentProtractorState.vertex.x, currentProtractorState.vertex.y)
                    lineTo(currentProtractorState.firstEndpoint.x, currentProtractorState.firstEndpoint.y)
                }
                
                // Create second line element
                val secondLinePath = Path().apply {
                    moveTo(currentProtractorState.vertex.x, currentProtractorState.vertex.y)
                    lineTo(currentProtractorState.secondEndpoint.x, currentProtractorState.secondEndpoint.y)
                }
                
                // Add both lines as drawing elements
                onAction(DrawingAction.AddElement(
                    DrawingElement.StrokeElement(
                        com.gunishjain.myapplication.model.Stroke(
                            path = firstLinePath,
                            color = Color.Black, // Black color for persisted lines
                            strokeWidth = state.strokeWidth
                        )
                    )
                ))
                
                onAction(DrawingAction.AddElement(
                    DrawingElement.StrokeElement(
                        com.gunishjain.myapplication.model.Stroke(
                            path = secondLinePath,
                            color = Color.Black, // Black color for persisted lines
                            strokeWidth = state.strokeWidth
                        )
                    )
                ))
                
                println("DEBUG: DrawingCanvas - Protractor lines added as permanent elements")
            }
            
            protractorStartPoint = null
            currentProtractor = null
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
                            DrawingTool.Protractor -> {
                                println("DEBUG: DrawingCanvas - Protractor tool tap handling")
                                val currentProtractorState = currentProtractor
                                
                                if (currentProtractorState == null) {
                                    // First tap - place vertex and start first line
                                    onAction(DrawingAction.StartDrawing(point))
                                    protractorStartPoint = point
                                    val initialProtractor = ProtractorTool().startDrawing(point)
                                    currentProtractor = initialProtractor
                                    onAction(DrawingAction.UpdateProtractorTool(initialProtractor))
                                } else if (currentProtractorState.firstLineComplete && !currentProtractorState.isDrawing) {
                                    // Second tap - start second line
                                    onAction(DrawingAction.StartDrawing(point))
                                    val updatedProtractor = currentProtractorState.startSecondLine()
                                    currentProtractor = updatedProtractor
                                    onAction(DrawingAction.UpdateProtractorTool(updatedProtractor))
                                } else if (currentProtractorState.firstLineComplete && currentProtractorState.secondLineComplete) {
                                    // Both lines complete - start new measurement
                                    onAction(DrawingAction.StartDrawing(point))
                                    protractorStartPoint = point
                                    val newProtractor = ProtractorTool().startDrawing(point)
                                    currentProtractor = newProtractor
                                    onAction(DrawingAction.UpdateProtractorTool(newProtractor))
                                    println("DEBUG: DrawingCanvas - Starting new protractor measurement")
                                }
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
                            DrawingTool.Protractor -> {
                                println("DEBUG: DrawingCanvas - onDragStart - Protractor tool drag")
                                val currentProtractorState = currentProtractor
                                
                                if (currentProtractorState == null) {
                                    // First drag - start drawing first line
                                    onAction(DrawingAction.StartDrawing(point))
                                    protractorStartPoint = point
                                    val initialProtractor = ProtractorTool().startDrawing(point)
                                    currentProtractor = initialProtractor
                                    onAction(DrawingAction.UpdateProtractorTool(initialProtractor))
                                } else if (currentProtractorState.firstLineComplete && !currentProtractorState.isDrawing) {
                                    // Second drag - start drawing second line
                                    onAction(DrawingAction.StartDrawing(point))
                                    val updatedProtractor = currentProtractorState.startSecondLine()
                                    currentProtractor = updatedProtractor
                                    onAction(DrawingAction.UpdateProtractorTool(updatedProtractor))
                                } else if (currentProtractorState.firstLineComplete && currentProtractorState.secondLineComplete) {
                                    // Both lines complete - start new measurement
                                    onAction(DrawingAction.StartDrawing(point))
                                    protractorStartPoint = point
                                    val newProtractor = ProtractorTool().startDrawing(point)
                                    currentProtractor = newProtractor
                                    onAction(DrawingAction.UpdateProtractorTool(newProtractor))
                                    println("DEBUG: DrawingCanvas - Starting new protractor measurement via drag")
                                }
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
                            DrawingTool.Protractor -> {
                                // Protractor tool - drag to set endpoint
                                println("DEBUG: DrawingCanvas - Protractor onDrag")
                                val snapResult = if (state.snapEnabled) {
                                    snapEngine.findBestSnapTarget(point, state.elements)
                                } else null
                                
                                val finalPoint = snapResult?.snappedPoint ?: point
                                
                                if (snapResult != null) {
                                    onAction(DrawingAction.PerformHapticFeedback)
                                }
                                
                                val currentProtractorState = currentProtractor
                                if (currentProtractorState != null) {
                                    val updatedProtractor = if (currentProtractorState.isDrawingFirstLine) {
                                        currentProtractorState.updateFirstEndpoint(finalPoint)
                                    } else if (currentProtractorState.isDrawingSecondLine) {
                                        // Check if user is forcing override
                                        val isForcingOverride = currentProtractorState.isForcingOverride(finalPoint)
                                        
                                        // Apply angle snapping for second line with force override
                                        val snappedPoint = currentProtractorState.snapSecondEndpointToCommonAngle(finalPoint, isForcingOverride)
                                        val tempProtractor = currentProtractorState.updateSecondEndpoint(snappedPoint)
                                        
                                        // Check if we snapped to a common angle (and not forcing override)
                                        val currentAngle = tempProtractor.calculateAngle()
                                        val nearestAngle = tempProtractor.findNearestCommonAngle(currentAngle)
                                        
                                        if (nearestAngle != null && !isForcingOverride) {
                                            // Provide haptic feedback for angle snapping
                                            onAction(DrawingAction.PerformHapticFeedback)
                                            println("DEBUG: DrawingCanvas - Protractor angle snapped to ${nearestAngle}°")
                                        } else if (isForcingOverride) {
                                            println("DEBUG: DrawingCanvas - Protractor angle override forced by user")
                                        }
                                        
                                        tempProtractor
                                    } else {
                                        currentProtractorState
                                    }
                                    
                                    currentProtractor = updatedProtractor
                                    onAction(DrawingAction.UpdateProtractorTool(updatedProtractor))
                                }
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
                            DrawingTool.Protractor -> {
                                // End protractor line drawing
                                println("DEBUG: DrawingCanvas - Protractor onDragEnd")
                                val lastProtractor = currentProtractor
                                if (lastProtractor != null && lastProtractor.isDrawing) {
                                    val finishedProtractor = if (lastProtractor.isDrawingFirstLine) {
                                        lastProtractor.finishFirstLine()
                                    } else if (lastProtractor.isDrawingSecondLine) {
                                        lastProtractor.finishSecondLine()
                                    } else {
                                        lastProtractor.finishDrawing()
                                    }
                                    currentProtractor = finishedProtractor
                                    onAction(DrawingAction.UpdateProtractorTool(finishedProtractor))
                                    println("DEBUG: DrawingCanvas - Protractor onDragEnd - Line finished, angle: ${finishedProtractor.angle}")
                                    
                                    // If both lines are now complete, automatically persist them
                                    if (finishedProtractor.firstLineComplete && finishedProtractor.secondLineComplete) {
                                        // Create first line element
                                        val firstLinePath = Path().apply {
                                            moveTo(finishedProtractor.vertex.x, finishedProtractor.vertex.y)
                                            lineTo(finishedProtractor.firstEndpoint.x, finishedProtractor.firstEndpoint.y)
                                        }
                                        
                                        // Create second line element
                                        val secondLinePath = Path().apply {
                                            moveTo(finishedProtractor.vertex.x, finishedProtractor.vertex.y)
                                            lineTo(finishedProtractor.secondEndpoint.x, finishedProtractor.secondEndpoint.y)
                                        }
                                        
                                        // Add both lines as drawing elements
                                        onAction(DrawingAction.AddElement(
                                            DrawingElement.StrokeElement(
                                                com.gunishjain.myapplication.model.Stroke(
                                                    path = firstLinePath,
                                                    color = Color.Black, // Black color for persisted lines
                                                    strokeWidth = state.strokeWidth
                                                )
                                            )
                                        ))
                                        
                                        onAction(DrawingAction.AddElement(
                                            DrawingElement.StrokeElement(
                                                com.gunishjain.myapplication.model.Stroke(
                                                    path = secondLinePath,
                                                    color = Color.Black, // Black color for persisted lines
                                                    strokeWidth = state.strokeWidth
                                                )
                                            )
                                        ))
                                        
                                        // Reset the protractor tool for next measurement
                                        currentProtractor = null
                                        onAction(DrawingAction.UpdateProtractorTool(ProtractorTool()))
                                        
                                        println("DEBUG: DrawingCanvas - Protractor lines automatically persisted as permanent elements, tool reset for next measurement")
                                    }
                                }
                                onAction(DrawingAction.EndDrawing(Point(0f, 0f)))
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
                
                // Draw Protractor tool
                if (currentProtractor != null && currentProtractor!!.isVisible) {
                    println("DEBUG: DrawingCanvas - Drawing protractor - angle: ${currentProtractor!!.angle}")
                    drawProtractor(currentProtractor!!, state.strokeColor, state.strokeWidth)
                } else {
                    println("DEBUG: DrawingCanvas - NOT drawing protractor - isVisible: ${currentProtractor?.isVisible}")
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

/**
 * Draw the protractor tool with angle measurement
 */
private fun DrawScope.drawProtractor(
    protractor: ProtractorTool,
    strokeColor: androidx.compose.ui.graphics.Color,
    strokeWidth: Float
) {
    val vertex = Offset(protractor.vertex.x, protractor.vertex.y)
    val firstEndpoint = Offset(protractor.firstEndpoint.x, protractor.firstEndpoint.y)
    val secondEndpoint = Offset(protractor.secondEndpoint.x, protractor.secondEndpoint.y)
    
    // Draw the first line from vertex to first endpoint (if completed or being drawn)
    if (protractor.firstLineComplete || protractor.isDrawingFirstLine) {
        drawLine(
            color = Color.Red,
            start = vertex,
            end = firstEndpoint,
            strokeWidth = strokeWidth * 2f,
            cap = StrokeCap.Round
        )
    }
    
    // Draw the second line from vertex to second endpoint (if completed or being drawn)
    if (protractor.secondLineComplete || protractor.isDrawingSecondLine) {
        drawLine(
            color = Color.Red,
            start = vertex,
            end = secondEndpoint,
            strokeWidth = strokeWidth * 2f,
            cap = StrokeCap.Round
        )
    }
    
    // Draw draggable endpoint circles (only for completed lines)
    if (protractor.firstLineComplete && protractor.firstEndpoint != protractor.vertex) {
        drawCircle(
            color = Color.Blue,
            radius = 8f,
            center = firstEndpoint,
            style = Stroke(width = 2f)
        )
    }
    
    if (protractor.secondLineComplete && protractor.secondEndpoint != protractor.vertex) {
        drawCircle(
            color = Color.Blue,
            radius = 8f,
            center = secondEndpoint,
            style = Stroke(width = 2f)
        )
    }
    
    // Draw angle arc (only when both lines are complete)
    if (protractor.firstLineComplete && protractor.secondLineComplete && 
        protractor.firstEndpoint != protractor.vertex && protractor.secondEndpoint != protractor.vertex) {
        val angle = protractor.angle
        if (angle > 0) {
            // Check if angle is snapped to a common angle
            val nearestAngle = protractor.findNearestCommonAngle(angle)
            val isSnapped = nearestAngle != null
            
            // Draw angle arc with different color for snapped angles
            val arcRadius = 30f
            val startAngle = atan2(firstEndpoint.y - vertex.y, firstEndpoint.x - vertex.x)
            val sweepAngle = atan2(secondEndpoint.y - vertex.y, secondEndpoint.x - vertex.x) - startAngle
            
            drawArc(
                color = if (isSnapped) Color.Green else Color(0xFFFF6600), // Green for snapped, orange for free
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(vertex.x - arcRadius, vertex.y - arcRadius),
                size = androidx.compose.ui.geometry.Size(arcRadius * 2, arcRadius * 2),
                style = Stroke(width = if (isSnapped) 4f else 3f, cap = StrokeCap.Round)
            )
            
            // Draw angle text (simplified - just draw a circle for now)
            val textOffset = Offset(vertex.x + 20f, vertex.y - 20f)
            drawCircle(
                color = if (isSnapped) Color.Green else Color.Blue,
                radius = 12f,
                center = textOffset,
                style = Stroke(width = 2f)
            )
        }
    }
    
    // Draw angle arc while drawing second line (for real-time feedback)
    if (protractor.isDrawingSecondLine && protractor.firstLineComplete && 
        protractor.firstEndpoint != protractor.vertex && protractor.secondEndpoint != protractor.vertex) {
        val angle = protractor.angle
        if (angle > 0) {
            // Check if angle is snapped to a common angle
            val nearestAngle = protractor.findNearestCommonAngle(angle)
            val isSnapped = nearestAngle != null
            
            // Draw angle arc with different color for snapped angles
            val arcRadius = 30f
            val startAngle = atan2(firstEndpoint.y - vertex.y, firstEndpoint.x - vertex.x)
            val sweepAngle = atan2(secondEndpoint.y - vertex.y, secondEndpoint.x - vertex.x) - startAngle
            
            drawArc(
                color = if (isSnapped) Color.Green.copy(alpha = 0.7f) else Color(0xFFFF6600).copy(alpha = 0.7f),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(vertex.x - arcRadius, vertex.y - arcRadius),
                size = androidx.compose.ui.geometry.Size(arcRadius * 2, arcRadius * 2),
                style = Stroke(width = if (isSnapped) 4f else 3f, cap = StrokeCap.Round)
            )
        }
    }
}
