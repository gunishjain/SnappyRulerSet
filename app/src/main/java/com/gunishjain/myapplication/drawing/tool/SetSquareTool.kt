package com.gunishjain.myapplication.drawing.tool

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.Path
import com.gunishjain.myapplication.model.Point
import kotlin.math.*

/**
 * Set Square Tool for drawing triangles with specific angles
 * Supports 45° and 30°-60° variants
 */
data class SetSquareTool(
    val center: Point = Point(0f, 0f),
    val size: Float = 100f, // Size of the set square
    val angle: Float = 0f, // Rotation angle in degrees
    val variant: SetSquareVariant = SetSquareVariant.FORTY_FIVE,
    val isVisible: Boolean = false,
    val isDragging: Boolean = false,
    val isRotating: Boolean = false,
    val isResizing: Boolean = false,
    val draggedVertexIndex: Int = -1, // Which vertex is being dragged (-1 = none)
    val color: Color = Color.Green,
    val strokeWidth: Float = 2f
) {
    /**
     * Get the three vertices of the set square triangle
     */
    fun getVertices(): List<Point> {
        val halfSize = size / 2f
        val vertices = mutableListOf<Point>()
        
        when (variant) {
            SetSquareVariant.FORTY_FIVE -> {
                // 45°-45°-90° triangle
                // Right angle at center, two 45° angles
                vertices.add(Point(center.x, center.y - halfSize)) // Top vertex
                vertices.add(Point(center.x - halfSize, center.y + halfSize)) // Bottom left
                vertices.add(Point(center.x + halfSize, center.y + halfSize)) // Bottom right
            }
            SetSquareVariant.THIRTY_SIXTY -> {
                // 30°-60°-90° triangle
                // 30° angle at top, 60° at bottom left, 90° at bottom right
                val height = halfSize * sqrt(3f) // Height of equilateral triangle
                vertices.add(Point(center.x, center.y - height * 2f / 3f)) // Top vertex (30°)
                vertices.add(Point(center.x - halfSize, center.y + height / 3f)) // Bottom left (60°)
                vertices.add(Point(center.x + halfSize, center.y + height / 3f)) // Bottom right (90°)
            }
        }
        
        // Apply rotation around center
        return vertices.map { vertex ->
            rotatePoint(vertex, center, angle)
        }
    }
    
    /**
     * Get the three edges of the set square triangle
     */
    fun getEdges(): List<Pair<Point, Point>> {
        val vertices = getVertices()
        return listOf(
            vertices[0] to vertices[1], // Edge 1
            vertices[1] to vertices[2], // Edge 2
            vertices[2] to vertices[0]  // Edge 3
        )
    }
    
    /**
     * Rotate a point around a center point
     */
    private fun rotatePoint(point: Point, center: Point, angleDegrees: Float): Point {
        val angleRadians = Math.toRadians(angleDegrees.toDouble()).toFloat()
        val cos = cos(angleRadians)
        val sin = sin(angleRadians)
        
        val dx = point.x - center.x
        val dy = point.y - center.y
        
        return Point(
            x = center.x + dx * cos - dy * sin,
            y = center.y + dx * sin + dy * cos
        )
    }
    
    /**
     * Update the position of the set square
     */
    fun updatePosition(deltaX: Float, deltaY: Float): SetSquareTool {
        return copy(
            center = Point(center.x + deltaX, center.y + deltaY)
        )
    }
    
    /**
     * Update the rotation of the set square
     */
    fun updateRotation(centerX: Float, centerY: Float, rotationDegrees: Float): SetSquareTool {
        val newAngle = (angle + rotationDegrees) % 360f
        return copy(angle = if (newAngle < 0) newAngle + 360f else newAngle)
    }
    
    /**
     * Snap the angle to common values
     */
    fun snapAngle(angle: Float): Float {
        var normalizedAngle = angle % 360f
        if (normalizedAngle < 0) normalizedAngle += 360f
        
        val snapAngles = listOf(0f, 15f, 30f, 45f, 60f, 75f, 90f, 105f, 120f, 135f, 150f, 165f, 180f)
        val threshold = 5f
        
        for (snapAngle in snapAngles) {
            val diff = abs(normalizedAngle - snapAngle)
            if (diff <= threshold || diff >= (360f - threshold)) {
                return snapAngle
            }
        }
        
        return normalizedAngle
    }
    
    /**
     * Check if a point is near any edge of the set square
     */
    fun isPointNearEdge(point: Point, threshold: Float = 10f): Boolean {
        val edges = getEdges()
        return edges.any { (start, end) ->
            distanceToLineSegment(point, start, end) <= threshold
        }
    }
    
    /**
     * Check if a point is near any vertex of the set square
     */
    fun isPointNearVertex(point: Point, threshold: Float = 40f): Int {
        val vertices = getVertices()
        println("DEBUG: SetSquareTool - Checking vertex detection for point: $point, threshold: $threshold")
        vertices.forEachIndexed { index, vertex ->
            val distance = sqrt((point.x - vertex.x).pow(2) + (point.y - vertex.y).pow(2))
            println("DEBUG: SetSquareTool - Vertex $index at $vertex, distance: $distance")
            if (distance <= threshold) {
                println("DEBUG: SetSquareTool - Found vertex $index within threshold")
                return index
            }
        }
        println("DEBUG: SetSquareTool - No vertex found within threshold")
        return -1
    }
    
    /**
     * Resize the set square by dragging a vertex
     */
    fun resizeByVertex(vertexIndex: Int, newPosition: Point): SetSquareTool {
        if (vertexIndex < 0 || vertexIndex >= 3) return this
        
        val vertices = getVertices()
        val draggedVertex = vertices[vertexIndex]
        
        // Calculate the distance from center to the new position
        val newDistance = sqrt((newPosition.x - center.x).pow(2) + (newPosition.y - center.y).pow(2))
        
        // Calculate the scale factor based on the original distance
        val originalDistance = sqrt((draggedVertex.x - center.x).pow(2) + (draggedVertex.y - center.y).pow(2))
        val scaleFactor = if (originalDistance > 0) newDistance / originalDistance else 1f
        
        // Apply minimum and maximum size constraints
        val newSize = (size * scaleFactor).coerceIn(50f, 300f)
        
        return copy(size = newSize)
    }
    
    /**
     * Start resizing by dragging a vertex
     */
    fun startResizing(vertexIndex: Int): SetSquareTool {
        return copy(
            isResizing = true,
            draggedVertexIndex = vertexIndex
        )
    }
    
    /**
     * Stop resizing
     */
    fun stopResizing(): SetSquareTool {
        return copy(
            isResizing = false,
            draggedVertexIndex = -1
        )
    }
    
    /**
     * Reset the resizing state (useful when switching between vertices)
     */
    fun resetResizingState(): SetSquareTool {
        return copy(
            isResizing = false,
            draggedVertexIndex = -1
        )
    }
    
    /**
     * Calculate distance from a point to a line segment
     */
    private fun distanceToLineSegment(point: Point, lineStart: Point, lineEnd: Point): Float {
        val A = point.x - lineStart.x
        val B = point.y - lineStart.y
        val C = lineEnd.x - lineStart.x
        val D = lineEnd.y - lineStart.y
        
        val dot = A * C + B * D
        val lenSq = C * C + D * D
        
        if (lenSq == 0f) return sqrt(A * A + B * B)
        
        val param = dot / lenSq
        
        val xx: Float
        val yy: Float
        
        if (param < 0) {
            xx = lineStart.x
            yy = lineStart.y
        } else if (param > 1) {
            xx = lineEnd.x
            yy = lineEnd.y
        } else {
            xx = lineStart.x + param * C
            yy = lineStart.y + param * D
        }
        
        val dx = point.x - xx
        val dy = point.y - yy
        return sqrt(dx * dx + dy * dy)
    }
    
    /**
     * Draw the set square
     */
    fun draw(drawScope: DrawScope) {
        if (!isVisible) return
        
        val vertices = getVertices()
        val path = Path()
        
        // Create triangle path
        path.moveTo(vertices[0].x, vertices[0].y)
        path.lineTo(vertices[1].x, vertices[1].y)
        path.lineTo(vertices[2].x, vertices[2].y)
        path.close()
        
        // Draw the triangle
        drawScope.drawPath(
            path = path,
            color = color,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = strokeWidth,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )
        
        // Draw vertices for better visibility
        vertices.forEach { vertex ->
            drawScope.drawCircle(
                color = Color.White,
                radius = 4f,
                center = Offset(vertex.x, vertex.y)
            )
            drawScope.drawCircle(
                color = color,
                radius = 3f,
                center = Offset(vertex.x, vertex.y)
            )
        }
        
        // Draw center point
        drawScope.drawCircle(
            color = Color.White,
            radius = 6f,
            center = Offset(center.x, center.y)
        )
        drawScope.drawCircle(
            color = color,
            radius = 4f,
            center = Offset(center.x, center.y)
        )
    }
}

/**
 * Set Square variants
 */
enum class SetSquareVariant {
    FORTY_FIVE,    // 45°-45°-90° triangle
    THIRTY_SIXTY   // 30°-60°-90° triangle
}
