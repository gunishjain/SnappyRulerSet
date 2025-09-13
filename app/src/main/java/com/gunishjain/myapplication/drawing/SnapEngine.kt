package com.gunishjain.myapplication.drawing

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.gunishjain.myapplication.model.*
import kotlin.math.*

/**
 * Extension function to convert Point to Offset
 */
private fun Point.toOffset(): Offset = Offset(x, y)

/**
 * Engine responsible for magnetic snapping functionality
 */
class SnapEngine(
    private var gridSpacing: Float = 20f, // Default grid spacing in pixels
    private var snapEnabled: Boolean = true,
    private var baseSnapRadius: Float = SnapTarget.DEFAULT_SNAP_DISTANCE,
    private var currentZoomLevel: Float = 1f
) {
    // Current snap targets in the drawing
    private var snapTargets: List<SnapTarget> = emptyList()
    
    // Last snap result for visual feedback
    private var lastSnapResult: SnapResult? = null
    
    /**
     * Calculate the dynamic snap radius based on zoom level
     * - Larger at low zoom
     * - Smaller at high zoom
     */
    fun calculateSnapRadius(): Float {
        return baseSnapRadius / currentZoomLevel
    }
    
    /**
     * Update the current zoom level
     */
    fun updateZoomLevel(zoomLevel: Float) {
        currentZoomLevel = zoomLevel
    }
    
    /**
     * Enable or disable snapping
     */
    fun setSnapEnabled(enabled: Boolean) {
        snapEnabled = enabled
    }
    
    /**
     * Set the grid spacing
     */
    fun setGridSpacing(spacing: Float) {
        gridSpacing = spacing
    }
    
    /**
     * Generate grid snap targets
     */
    private fun generateGridSnapTargets(canvasWidth: Float, canvasHeight: Float): List<SnapTarget> {
        val targets = mutableListOf<SnapTarget>()
        
        // Calculate grid points based on canvas dimensions and grid spacing
        val rows = (canvasHeight / gridSpacing).toInt() + 1
        val cols = (canvasWidth / gridSpacing).toInt() + 1
        
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val x = col * gridSpacing
                val y = row * gridSpacing
                targets.add(
                    SnapTarget(
                        point = Point(x, y),
                        type = SnapTargetType.GRID_POINT,
                        priority = 0, // Grid points have lowest priority
                        color = Color.LightGray
                    )
                )
            }
        }
        
        return targets
    }
    
    /**
     * Generate snap targets from drawing elements
     */
    private fun generateElementSnapTargets(elements: List<DrawingElement>): List<SnapTarget> {
        val targets = mutableListOf<SnapTarget>()
        
        elements.forEach { element ->
            when (element) {
                is DrawingElement.LineElement -> {
                    val line = element.line
                    
                    // Add endpoints
                    targets.add(
                        SnapTarget(
                            point = line.start,
                            type = SnapTargetType.ENDPOINT,
                            priority = 2,
                            sourceElement = element
                        )
                    )
                    targets.add(
                        SnapTarget(
                            point = line.end,
                            type = SnapTargetType.ENDPOINT,
                            priority = 2,
                            sourceElement = element
                        )
                    )
                    
                    // Add midpoint
                    val midpoint = Point(
                        (line.start.x + line.end.x) / 2f,
                        (line.start.y + line.end.y) / 2f
                    )
                    targets.add(
                        SnapTarget(
                            point = midpoint,
                            type = SnapTargetType.MIDPOINT,
                            priority = 1,
                            sourceElement = element
                        )
                    )
                }
                
                is DrawingElement.CircleElement -> {
                    val circle = element.circle
                    
                    // Add center point
                    targets.add(
                        SnapTarget(
                            point = circle.center,
                            type = SnapTargetType.CENTER,
                            priority = 2,
                            sourceElement = element
                        )
                    )
                }
                
                else -> { /* Ignore other element types for now */ }
            }
        }
        
        // Add intersection points between lines
        addIntersectionPoints(elements, targets)
        
        return targets
    }
    
    /**
     * Generate snap targets for the current drawing state
     * This is used by the DrawingCanvas to get targets for visual feedback
     */
    fun generateSnapTargets(elements: List<DrawingElement>): List<SnapTarget> {
        return snapTargets + generateElementSnapTargets(elements)
    }
    
    /**
     * Draw a grid on the canvas for visual feedback
     */
    fun drawGrid(drawScope: DrawScope, size: androidx.compose.ui.geometry.Size) {
        if (!snapEnabled) {
            return
        }
        
        val width = size.width
        val height = size.height
        
        with(drawScope) {
            // Draw vertical grid lines
            for (x in 0..(width.toInt() / gridSpacing.toInt())) {
                val xPos = x * gridSpacing
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    start = Offset(xPos, 0f),
                    end = Offset(xPos, height),
                    strokeWidth = 0.5.dp.toPx()
                )
            }
            
            // Draw horizontal grid lines
            for (y in 0..(height.toInt() / gridSpacing.toInt())) {
                val yPos = y * gridSpacing
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    start = Offset(0f, yPos),
                    end = Offset(width, yPos),
                    strokeWidth = 0.5.dp.toPx()
                )
            }
        }
    }
    
    /**
     * Find and add intersection points between lines
     */
    private fun addIntersectionPoints(
        elements: List<DrawingElement>,
        targets: MutableList<SnapTarget>
    ) {
        val lines = elements.filterIsInstance<DrawingElement.LineElement>().map { it.line }
        
        // Check each pair of lines for intersection
        for (i in lines.indices) {
            for (j in i + 1 until lines.size) {
                val line1 = lines[i]
                val line2 = lines[j]
                
                val intersection = findIntersection(line1, line2)
                if (intersection != null) {
                    targets.add(
                        SnapTarget(
                            point = intersection,
                            type = SnapTargetType.INTERSECTION,
                            priority = 3, // Highest priority
                            color = Color.Red
                        )
                    )
                }
            }
        }
    }
    
    /**
     * Find the intersection point between two lines
     */
    private fun findIntersection(line1: Line, line2: Line): Point? {
        // Line 1 represented as a1x + b1y = c1
        val a1 = line1.end.y - line1.start.y
        val b1 = line1.start.x - line1.end.x
        val c1 = a1 * line1.start.x + b1 * line1.start.y
        
        // Line 2 represented as a2x + b2y = c2
        val a2 = line2.end.y - line2.start.y
        val b2 = line2.start.x - line2.end.x
        val c2 = a2 * line2.start.x + b2 * line2.start.y
        
        val determinant = a1 * b2 - a2 * b1
        
        // If lines are parallel, no intersection
        if (abs(determinant) < 0.001f) {
            return null
        }
        
        val x = (b2 * c1 - b1 * c2) / determinant
        val y = (a1 * c2 - a2 * c1) / determinant
        
        // Check if intersection point is within both line segments
        if (isPointOnLineSegment(x, y, line1) && isPointOnLineSegment(x, y, line2)) {
            return Point(x, y)
        }
        
        return null
    }
    
    /**
     * Check if a point is on a line segment
     */
    private fun isPointOnLineSegment(x: Float, y: Float, line: Line): Boolean {
        val minX = min(line.start.x, line.end.x) - 0.1f
        val maxX = max(line.start.x, line.end.x) + 0.1f
        val minY = min(line.start.y, line.end.y) - 0.1f
        val maxY = max(line.start.y, line.end.y) + 0.1f
        
        return x in minX..maxX && y in minY..maxY
    }
    
    /**
     * Update snap targets based on current drawing elements and canvas size
     */
    fun updateSnapTargets(elements: List<DrawingElement>, canvasWidth: Float, canvasHeight: Float) {
        val gridTargets = generateGridSnapTargets(canvasWidth, canvasHeight)
        val elementTargets = generateElementSnapTargets(elements)
        
        snapTargets = gridTargets + elementTargets
    }
    
    /**
     * Find the nearest snap target to a given point
     */
    fun findNearestSnapTarget(point: Point): SnapResult? {
        if (!snapEnabled || snapTargets.isEmpty()) {
            return null
        }
        
        val snapRadius = calculateSnapRadius()
        var nearestTarget: SnapTarget? = null
        var minDistance = Float.MAX_VALUE
        
        // Find the nearest snap target within the snap radius
        for (target in snapTargets) {
            val distance = calculateDistance(point, target.point)
            
            if (distance <= snapRadius && (nearestTarget == null || 
                distance < minDistance || 
                (abs(distance - minDistance) < 0.1f && target.priority > nearestTarget.priority))) {
                nearestTarget = target
                minDistance = distance
            }
        }
        
        return nearestTarget?.let { 
            SnapResult(
                originalPoint = point,
                snappedPoint = it.point,
                snapTarget = it,
                distance = minDistance
            )
        }.also { 
            lastSnapResult = it 
        }
    }
    
    /**
     * Find the best snap target for a point, considering existing elements
     */
    fun findBestSnapTarget(point: Point, elements: List<DrawingElement>): SnapResult? {
        if (!snapEnabled) {
            return null
        }
        
        // Update snap targets with current elements
        val elementTargets = generateElementSnapTargets(elements)
        val allTargets = snapTargets + elementTargets
        
        val snapRadius = calculateSnapRadius()
        var nearestTarget: SnapTarget? = null
        var minDistance = Float.MAX_VALUE
        
        // Find the nearest snap target within the snap radius
        for (target in allTargets) {
            val distance = calculateDistance(point, target.point)
            
            if (distance <= snapRadius && (nearestTarget == null || 
                distance < minDistance || 
                (abs(distance - minDistance) < 0.1f && target.priority > nearestTarget.priority))) {
                nearestTarget = target
                minDistance = distance
            }
        }
        
        return nearestTarget?.let { 
            SnapResult(
                originalPoint = point,
                snappedPoint = it.point,
                snapTarget = it,
                distance = minDistance
            )
        }.also { 
            lastSnapResult = it 
        }
    }
    
    /**
     * Snap a point to the nearest target
     */
    fun snapPoint(point: Point): Point {
        val result = findNearestSnapTarget(point)
        return result?.snappedPoint ?: point
    }
    
    /**
     * Snap an angle to common angles
     */
    fun snapAngle(angle: Float): Float {
        if (!snapEnabled) {
            return angle
        }
        
        var normalizedAngle = angle % 360
        if (normalizedAngle < 0) normalizedAngle += 360
        
        for (snapAngle in SnapTarget.COMMON_ANGLES) {
            val diff = abs(normalizedAngle - snapAngle)
            if (diff <= SnapTarget.ANGLE_SNAP_THRESHOLD || 
                diff >= (360f - SnapTarget.ANGLE_SNAP_THRESHOLD)) {
                return snapAngle
            }
        }
        
        return angle
    }
    
    /**
     * Calculate distance between two points
     */
    private fun calculateDistance(p1: Point, p2: Point): Float {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        return sqrt(dx * dx + dy * dy)
    }
    
    /**
     * Draw snap indicators
     */
    fun drawSnapIndicators(drawScope: DrawScope, currentPoint: Point, snapTargets: List<SnapTarget>) {
        if (!snapEnabled) {
            return
        }
        
        // Find the nearest snap target to the current point
        val result = findBestSnapTarget(currentPoint, emptyList())
        if (result == null) {
            return
        }
        
        val target = result.snapTarget
        
        with(drawScope) {
            // Draw a circle around the snap point
            drawCircle(
                color = target.color,
                radius = 8.dp.toPx(),
                center = target.point.toOffset(),
                alpha = 0.7f
            )
            
            // Draw a line from original point to snapped point
            drawLine(
                color = target.color,
                start = result.originalPoint.toOffset(),
                end = result.snappedPoint.toOffset(),
                strokeWidth = 1.dp.toPx(),
                alpha = 0.5f
            )
        }
    }
    
    /**
     * Draw snap indicators using the last snap result
     */
    fun drawSnapIndicators(drawScope: DrawScope) {
        if (!snapEnabled || lastSnapResult == null) {
            return
        }
        
        val result = lastSnapResult ?: return
        val target = result.snapTarget
        
        with(drawScope) {
            // Draw a circle around the snap point
            drawCircle(
                color = target.color,
                radius = 8.dp.toPx(),
                center = target.point.toOffset(),
                alpha = 0.7f
            )
            
            // Draw a line from original point to snapped point
            drawLine(
                color = target.color,
                start = result.originalPoint.toOffset(),
                end = result.snappedPoint.toOffset(),
                strokeWidth = 1.dp.toPx(),
                alpha = 0.5f
            )
        }
    }
}