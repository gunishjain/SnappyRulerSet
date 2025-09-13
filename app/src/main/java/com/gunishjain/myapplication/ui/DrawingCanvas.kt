package com.gunishjain.myapplication.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import com.gunishjain.myapplication.data.DrawingAction
import com.gunishjain.myapplication.data.DrawingState
import com.gunishjain.myapplication.model.DrawingElement
import com.gunishjain.myapplication.model.Point
import kotlin.math.abs

/**
 * Main drawing canvas composable that handles all drawing operations
 */
@Composable
fun DrawingCanvas(
    state: DrawingState,
    onAction: (DrawingAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var currentPath by remember { mutableStateOf<Path?>(null) }
    
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        if (state.currentTool == com.gunishjain.myapplication.model.DrawingTool.Freehand) {
                            val point = Point(offset.x, offset.y)
                            onAction(DrawingAction.StartDrawing(point))
                            currentPath = Path().apply { moveTo(offset.x, offset.y) }
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        if (state.currentTool == com.gunishjain.myapplication.model.DrawingTool.Freehand) {
                            val point = Point(offset.x, offset.y)
                            onAction(DrawingAction.StartDrawing(point))
                            currentPath = Path().apply { moveTo(offset.x, offset.y) }
                        }
                    },
                    onDrag = { change, _ ->
                        if (state.currentTool == com.gunishjain.myapplication.model.DrawingTool.Freehand) {
                            val point = Point(change.position.x, change.position.y)
                            onAction(DrawingAction.UpdateDrawing(point))
                            currentPath?.quadraticBezierTo(
                                change.previousPosition.x,
                                change.previousPosition.y,
                                change.position.x,
                                change.position.y
                            )
                        }
                    },
                    onDragEnd = {
                        if (state.currentTool == com.gunishjain.myapplication.model.DrawingTool.Freehand) {
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
