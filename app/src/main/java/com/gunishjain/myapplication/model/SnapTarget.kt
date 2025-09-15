package com.gunishjain.myapplication.model

import androidx.compose.ui.graphics.Color

/**
 * Represents a point that can be snapped to
 */
data class SnapTarget(
    val point: Point,
    val type: SnapTargetType,
    val priority: Int = 1, // Higher priority targets are preferred when multiple targets are in range
    val color: Color = Color.Blue,
    val sourceElement: DrawingElement? = null // The element this snap target is derived from, if any
) {
    companion object {
        // Common snap angles in degrees
        val COMMON_ANGLES = listOf(0f, 30f, 45f, 60f, 90f, 120f, 135f, 150f, 180f, 
                                  210f, 225f, 240f, 270f, 300f, 315f, 330f)
        
        // Default snap threshold in degrees
        const val ANGLE_SNAP_THRESHOLD = 5f
        
        // Default snap distance threshold in pixels
        const val DEFAULT_SNAP_DISTANCE = 20f
    }
}

/**
 * Types of snap targets
 */
enum class SnapTargetType {
    // Grid points
    GRID_POINT,
    
    // Points on existing elements
    ENDPOINT,       // End of a line
    MIDPOINT,       // Middle of a line
    INTERSECTION,   // Intersection of two lines
    CENTER,         // Center of a circle
}

/**
 * Represents a snap result
 */
data class SnapResult(
    val originalPoint: Point,
    val snappedPoint: Point,
    val snapTarget: SnapTarget,
    val distance: Float // Distance from original point to snapped point
) {
    val isSnapped: Boolean get() = distance <= SnapTarget.DEFAULT_SNAP_DISTANCE
}