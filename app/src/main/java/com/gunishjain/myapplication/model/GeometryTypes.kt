package com.gunishjain.myapplication.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

/**
 * Represents a point in 2D space
 */
data class Point(
    val x: Float,
    val y: Float
) {
    fun toOffset(): Offset = Offset(x, y)
    
    companion object {
        fun fromOffset(offset: Offset): Point = Point(offset.x, offset.y)
    }
}

/**
 * Represents a line segment between two points
 */
data class Line(
    val start: Point,
    val end: Point,
    val color: Color = Color.Black,
    val strokeWidth: Float = 2f
)

/**
 * Represents a freehand drawing stroke
 */
data class Stroke(
    val path: Path,
    val color: Color = Color.Black,
    val strokeWidth: Float = 2f
)

/**
 * Represents an angle measurement
 */
data class Angle(
    val vertex: Point,
    val startAngle: Float,
    val endAngle: Float,
    val color: Color = Color.Red
)

/**
 * Represents a circle or arc
 */
data class Circle(
    val center: Point,
    val radius: Float,
    val color: Color = Color.Blue,
    val strokeWidth: Float = 2f
)

/**
 * Represents a drawing element that can be rendered
 */
sealed class DrawingElement {
    data class LineElement(val line: Line) : DrawingElement()
    data class StrokeElement(val stroke: Stroke) : DrawingElement()
    data class AngleElement(val angle: Angle) : DrawingElement()
    data class CircleElement(val circle: Circle) : DrawingElement()
}
