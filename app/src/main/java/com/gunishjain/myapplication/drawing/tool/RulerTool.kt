package com.gunishjain.myapplication.drawing.tool

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.gunishjain.myapplication.model.Point
import kotlin.math.*

/**
 * Ruler tool for drawing straight lines with snapping
 */
data class RulerTool(
    val startPoint: Point = Point(0f, 0f),
    val endPoint: Point = Point(0f, 0f),
    val isVisible: Boolean = false,
    val isDragging: Boolean = false,
    val isRotating: Boolean = false,
    val color: Color = Color.Blue,
    val strokeWidth: Float = 2f,
    val length: Float = 200f, // Default ruler length in pixels
    val angle: Float = 0f // Angle in degrees
) {
    /**
     * Get the current line endpoints
     */
    fun getLineEndpoints(): Pair<Point, Point> = startPoint to endPoint
    
    /**
     * Calculate the current angle of the ruler
     */
    fun calculateAngle(): Float {
        val dx = endPoint.x - startPoint.x
        val dy = endPoint.y - startPoint.y
        return Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    }
    
    /**
     * Snap the angle to common values
     * Prioritizes horizontal (0°, 180°) and vertical (90°, 270°) angles
     */
    fun snapAngle(angle: Float): Float {
        // Normalize angle to 0-359 range
        var normalizedAngle = angle % 360
        if (normalizedAngle < 0) normalizedAngle += 360
        
        // Check for horizontal and vertical angles first with higher threshold
        val horizontalVerticalThreshold = 10f
        
        // Horizontal angles (0° and 180°)
        if (abs(normalizedAngle) <= horizontalVerticalThreshold || 
            abs(normalizedAngle - 180f) <= horizontalVerticalThreshold) {
            return if (normalizedAngle <= horizontalVerticalThreshold) 0f else 180f
        }
        
        // Vertical angles (90° and 270°)
        if (abs(normalizedAngle - 90f) <= horizontalVerticalThreshold || 
            abs(normalizedAngle - 270f) <= horizontalVerticalThreshold) {
            return if (abs(normalizedAngle - 90f) <= horizontalVerticalThreshold) 90f else 270f
        }
        
        // Check other common angles with standard threshold
        val threshold = 5f
        val commonAngles = listOf(0f, 30f, 45f, 60f, 90f, 120f, 135f, 150f, 180f, 
                                 210f, 225f, 240f, 270f, 300f, 315f, 330f)
        
        for (snapAngle in commonAngles) {
            if (abs(normalizedAngle - snapAngle) <= threshold) {
                return snapAngle
            }
        }
        
        // Normalize the result to 0-359 range
        return normalizedAngle
    }
    
    /**
     * Update ruler position based on drag
     */
    fun updatePosition(deltaX: Float, deltaY: Float): RulerTool {
        return copy(
            startPoint = Point(startPoint.x + deltaX, startPoint.y + deltaY),
            endPoint = Point(endPoint.x + deltaX, endPoint.y + deltaY)
        )
    }
    
    /**
     * Set ruler position with snapping
     */
    fun setPosition(start: Point, end: Point): RulerTool {
        val angle = calculateAngle()
        val snappedAngle = snapAngle(angle)
        
        // If angle was snapped, recalculate endpoints
        if (abs(angle - snappedAngle) > 0.1f) {
            val centerX = (start.x + end.x) / 2f
            val centerY = (start.y + end.y) / 2f
            val radians = Math.toRadians(snappedAngle.toDouble()).toFloat()
            val halfLength = length / 2f
            
            val newStart = Point(
                centerX - halfLength * cos(radians),
                centerY - halfLength * sin(radians)
            )
            val newEnd = Point(
                centerX + halfLength * cos(radians),
                centerY + halfLength * sin(radians)
            )
            
            return copy(
                startPoint = newStart,
                endPoint = newEnd,
                angle = snappedAngle
            )
        }
        
        return copy(
            startPoint = start,
            endPoint = end,
            angle = snappedAngle
        )
    }
    
    /**
     * Calculate the distance between the ruler's endpoints in pixels
     */
    fun calculateDistance(): Float {
        val dx = endPoint.x - startPoint.x
        val dy = endPoint.y - startPoint.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
    
    /**
     * Check if the ruler is in horizontal or vertical orientation
     */
    fun isHorizontalOrVertical(): Boolean {
        val currentAngle = calculateAngle()
        return currentAngle % 90f < 5f || currentAngle % 90f > 85f
    }
    
    /**
     * Update the ruler's rotation around a center point
     * 
     * @param centerX X coordinate of the rotation center
     * @param centerY Y coordinate of the rotation center
     * @param angle New angle in degrees
     * @return Updated RulerTool with new position
     */
    fun updateRotation(centerX: Float, centerY: Float, angle: Float): RulerTool {
        val snappedAngle = snapAngle(angle)
        val radians = Math.toRadians(snappedAngle.toDouble()).toFloat()
        val halfLength = length / 2f
        
        val newStart = Point(
            centerX - halfLength * cos(radians),
            centerY - halfLength * sin(radians)
        )
        val newEnd = Point(
            centerX + halfLength * cos(radians),
            centerY + halfLength * sin(radians)
        )
        
        return copy(
            startPoint = newStart,
            endPoint = newEnd,
            angle = snappedAngle
        )
    }
    
    /**
     * Draw the ruler on the canvas
     */
    fun draw(drawScope: DrawScope) {
        if (!isVisible) return
        
        // Draw the main ruler line
        drawScope.drawLine(
            color = color,
            start = Offset(startPoint.x, startPoint.y),
            end = Offset(endPoint.x, endPoint.y),
            strokeWidth = strokeWidth
        )
        
        // Draw measurement markings
        drawMeasurementMarkings(drawScope)
        
        // Draw center point
        val centerX = (startPoint.x + endPoint.x) / 2f
        val centerY = (startPoint.y + endPoint.y) / 2f
        drawScope.drawCircle(
            color = color,
            radius = 4f,
            center = Offset(centerX, centerY)
        )
        
        // Draw distance text
        val distance = calculateDistance()
        val distanceText = String.format("%.1f px", distance)
        
        // Draw text using Compose's drawText with proper parameters
        val textStyle = TextStyle(
            color = Color.Black,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            shadow = Shadow(
                color = Color.White,
                offset = Offset(0f, 1f),
                blurRadius = 1f
            )
        )
        
        // Draw text using a simpler approach
        drawScope.drawCircle(
            color = Color.White,
            radius = 30f,
            center = Offset(centerX, centerY - 20f)
        )
        
        // Draw text as a simple line
        drawScope.drawLine(
            color = Color.Black,
            start = Offset(centerX - 25f, centerY - 20f),
            end = Offset(centerX + 25f, centerY - 20f),
            strokeWidth = 2f
        )
    }
    
    /**
     * Draw measurement markings along the ruler
     */
    private fun drawMeasurementMarkings(drawScope: DrawScope) {
        val dx = endPoint.x - startPoint.x
        val dy = endPoint.y - startPoint.y
        val angle = atan2(dy, dx)
        
        val markingLength = 10f
        val markingSpacing = 20f // pixels between markings
        
        val totalLength = sqrt(dx * dx + dy * dy)
        val numMarkings = (totalLength / markingSpacing).toInt()
        
        for (i in 0..numMarkings) {
            val t = i * markingSpacing / totalLength
            val x = startPoint.x + t * dx
            val y = startPoint.y + t * dy
            
            // Perpendicular offset for markings
            val perpX = -dy / totalLength * markingLength / 2f
            val perpY = dx / totalLength * markingLength / 2f
            
            val startX = x + perpX
            val startY = y + perpY
            val endX = x - perpX
            val endY = y - perpY
            
            drawScope.drawLine(
                color = this.color,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 1f
            )
        }
    }
}
