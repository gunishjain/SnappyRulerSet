package com.gunishjain.myapplication.drawing.tool

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.Path
import com.gunishjain.myapplication.model.Point
import kotlin.math.*

/**
 * Compass Tool for drawing circles
 * Works like Paint app - drag to draw circle based on radius
 */
data class CompassTool(
    val center: Point = Point(0f, 0f),
    val radius: Float = 50f,
    val isVisible: Boolean = false,
    val isDrawing: Boolean = false,
    val color: Color = Color.Magenta,
    val strokeWidth: Float = 2f
) {
    /**
     * Update the radius based on distance from center
     */
    fun updateRadius(newPoint: Point): CompassTool {
        val newRadius = sqrt(
            (newPoint.x - center.x).pow(2) + 
            (newPoint.y - center.y).pow(2)
        )
        return copy(radius = newRadius.coerceAtLeast(5f)) // Minimum radius of 5px
    }
    
    /**
     * Update the center position
     */
    fun updateCenter(newCenter: Point): CompassTool {
        return copy(center = newCenter)
    }
    
    /**
     * Create a circle path
     */
    fun createCirclePath(): Path {
        val path = Path()
        path.addOval(
            androidx.compose.ui.geometry.Rect(
                center.x - radius,
                center.y - radius,
                center.x + radius,
                center.y + radius
            )
        )
        return path
    }
    
    /**
     * Draw the compass (simple circle outline)
     */
    fun draw(drawScope: DrawScope) {
        if (!isVisible) return
        
        with(drawScope) {
            // Draw the circle outline
            drawCircle(
                color = color.copy(alpha = 0.3f),
                radius = radius,
                center = Offset(center.x, center.y),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = strokeWidth,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                )
            )
        }
    }
}
