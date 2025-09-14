package com.gunishjain.myapplication.drawing.tool

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.gunishjain.myapplication.model.Point
import kotlin.math.*

/**
 * Represents a protractor tool for measuring angles
 */
data class ProtractorTool(
    val vertex: Point = Point(0f, 0f),
    val firstEndpoint: Point = Point(0f, 0f),
    val secondEndpoint: Point = Point(0f, 0f),
    val isVisible: Boolean = false,
    val isDrawing: Boolean = false,
    val isDrawingFirstLine: Boolean = false,
    val isDrawingSecondLine: Boolean = false,
    val firstLineComplete: Boolean = false,
    val secondLineComplete: Boolean = false,
    val angle: Float = 0f,
    val isDraggingFirst: Boolean = false,
    val isDraggingSecond: Boolean = false
) {
    /**
     * Calculate the angle between the two lines
     */
    fun calculateAngle(): Float {
        if (vertex == firstEndpoint || vertex == secondEndpoint) return 0f
        
        val v1 = Offset(firstEndpoint.x - vertex.x, firstEndpoint.y - vertex.y)
        val v2 = Offset(secondEndpoint.x - vertex.x, secondEndpoint.y - vertex.y)
        
        val dot = v1.x * v2.x + v1.y * v2.y
        val mag1 = sqrt(v1.x * v1.x + v1.y * v1.y)
        val mag2 = sqrt(v2.x * v2.x + v2.y * v2.y)
        
        if (mag1 == 0f || mag2 == 0f) return 0f
        
        val cosAngle = dot / (mag1 * mag2)
        val clampedCos = cosAngle.coerceIn(-1f, 1f)
        val angleRad = acos(clampedCos)
        val angleDeg = Math.toDegrees(angleRad.toDouble()).toFloat()
        
        return angleDeg
    }
    
    /**
     * Get the distance from vertex to first endpoint
     */
    fun getFirstLineLength(): Float {
        return sqrt((firstEndpoint.x - vertex.x).pow(2) + (firstEndpoint.y - vertex.y).pow(2))
    }
    
    /**
     * Get the distance from vertex to second endpoint
     */
    fun getSecondLineLength(): Float {
        return sqrt((secondEndpoint.x - vertex.x).pow(2) + (secondEndpoint.y - vertex.y).pow(2))
    }
    
    /**
     * Check if a point is near the first endpoint (for dragging)
     */
    fun isNearFirstEndpoint(point: Point, threshold: Float = 30f): Boolean {
        val distance = sqrt((point.x - firstEndpoint.x).pow(2) + (point.y - firstEndpoint.y).pow(2))
        return distance <= threshold
    }
    
    /**
     * Check if a point is near the second endpoint (for dragging)
     */
    fun isNearSecondEndpoint(point: Point, threshold: Float = 30f): Boolean {
        val distance = sqrt((point.x - secondEndpoint.x).pow(2) + (point.y - secondEndpoint.y).pow(2))
        return distance <= threshold
    }
    
    /**
     * Check if a point is near the vertex (for moving the entire protractor)
     */
    fun isNearVertex(point: Point, threshold: Float = 30f): Boolean {
        val distance = sqrt((point.x - vertex.x).pow(2) + (point.y - vertex.y).pow(2))
        return distance <= threshold
    }
    
    /**
     * Update the first endpoint
     */
    fun updateFirstEndpoint(newEndpoint: Point): ProtractorTool {
        return copy(firstEndpoint = newEndpoint, angle = calculateAngle())
    }
    
    /**
     * Update the second endpoint
     */
    fun updateSecondEndpoint(newEndpoint: Point): ProtractorTool {
        return copy(secondEndpoint = newEndpoint, angle = calculateAngle())
    }
    
    /**
     * Update the vertex position
     */
    fun updateVertex(newVertex: Point): ProtractorTool {
        val offsetX = newVertex.x - vertex.x
        val offsetY = newVertex.y - vertex.y
        return copy(
            vertex = newVertex,
            firstEndpoint = Point(firstEndpoint.x + offsetX, firstEndpoint.y + offsetY),
            secondEndpoint = Point(secondEndpoint.x + offsetX, secondEndpoint.y + offsetY),
            angle = calculateAngle()
        )
    }
    
    /**
     * Start drawing the protractor (place vertex)
     */
    fun startDrawing(vertex: Point): ProtractorTool {
        return copy(
            vertex = vertex,
            firstEndpoint = vertex,
            secondEndpoint = vertex,
            isVisible = true,
            isDrawing = true,
            isDrawingFirstLine = true,
            isDrawingSecondLine = false,
            firstLineComplete = false,
            secondLineComplete = false,
            angle = 0f
        )
    }
    
    /**
     * Start drawing the first line
     */
    fun startFirstLine(): ProtractorTool {
        return copy(
            isDrawingFirstLine = true,
            isDrawingSecondLine = false,
            isDrawing = true
        )
    }
    
    /**
     * Start drawing the second line
     */
    fun startSecondLine(): ProtractorTool {
        return copy(
            isDrawingFirstLine = false,
            isDrawingSecondLine = true,
            isDrawing = true
        )
    }
    
    /**
     * Finish drawing the first line
     */
    fun finishFirstLine(): ProtractorTool {
        return copy(
            isDrawingFirstLine = false,
            firstLineComplete = true,
            isDrawing = false,
            angle = calculateAngle()
        )
    }
    
    /**
     * Finish drawing the second line
     */
    fun finishSecondLine(): ProtractorTool {
        return copy(
            isDrawingSecondLine = false,
            secondLineComplete = true,
            isDrawing = false,
            angle = calculateAngle()
        )
    }
    
    /**
     * Finish drawing the protractor completely
     */
    fun finishDrawing(): ProtractorTool {
        return copy(
            isDrawing = false,
            isDrawingFirstLine = false,
            isDrawingSecondLine = false,
            angle = calculateAngle()
        )
    }
    
    /**
     * Hide the protractor
     */
    fun hide(): ProtractorTool {
        return copy(isVisible = false, isDrawing = false)
    }
    
    /**
     * Create a finished protractor state (both lines complete)
     */
    fun createFinishedState(): ProtractorTool {
        return copy(
            isVisible = false,
            isDrawing = false,
            isDrawingFirstLine = false,
            isDrawingSecondLine = false,
            firstLineComplete = true,
            secondLineComplete = true,
            angle = calculateAngle()
        )
    }
}
