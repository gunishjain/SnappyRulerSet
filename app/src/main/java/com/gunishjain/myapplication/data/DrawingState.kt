package com.gunishjain.myapplication.data

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.gunishjain.myapplication.drawing.tool.RulerTool
import com.gunishjain.myapplication.drawing.tool.CompassTool
import com.gunishjain.myapplication.drawing.tool.ProtractorTool
import com.gunishjain.myapplication.model.DrawingElement
import com.gunishjain.myapplication.model.DrawingTool
import com.gunishjain.myapplication.model.Point

/**
 * Centralized state management for the drawing canvas
 */
data class DrawingState(
    val currentTool: DrawingTool = DrawingTool.Freehand,
    val elements: List<DrawingElement> = emptyList(),
    val currentPath: Path? = null,
    val isDrawing: Boolean = false,
    val canvasOffset: Offset = Offset.Zero,
    val canvasScale: Float = 1f,
    val canvasRotation: Float = 0f,
    val strokeColor: Color = Color.Black,
    val strokeWidth: Float = 2f,
    val snapEnabled: Boolean = true,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val rulerTool: RulerTool = RulerTool(),
    val compassTool: CompassTool = CompassTool(),
    val protractorTool: ProtractorTool = ProtractorTool(),
    val gridSpacing: Float = 20f, // Default grid spacing in pixels
    val lastAction: DrawingAction? = null
)

/**
 * Actions that can modify the drawing state
 */
sealed class DrawingAction {
    data class SetTool(val tool: DrawingTool) : DrawingAction()
    data class AddElement(val element: DrawingElement) : DrawingAction()
    data class StartDrawing(val point: Point) : DrawingAction()
    data class UpdateDrawing(val point: Point) : DrawingAction()
    data class EndDrawing(val point: Point) : DrawingAction()
    data object ClearCanvas : DrawingAction()
    data class SetCanvasTransform(
        val offset: Offset,
        val scale: Float,
        val rotation: Float
    ) : DrawingAction()
    data class SetStrokeColor(val color: Color) : DrawingAction()
    data class SetStrokeWidth(val width: Float) : DrawingAction()
    data class ToggleSnap(val enabled: Boolean) : DrawingAction()
    data class SetGridSpacing(val spacing: Float) : DrawingAction()
    data class UpdateRulerTool(val rulerTool: RulerTool) : DrawingAction()
    data class StartRulerDrag(val point: Point) : DrawingAction()
    data class UpdateRulerDrag(val point: Point) : DrawingAction()
    data class EndRulerDrag(val point: Point) : DrawingAction()
    data class StartRulerRotation(val point: Point) : DrawingAction()
    data class UpdateRulerRotation(val point: Point) : DrawingAction()
    data class EndRulerRotation(val point: Point) : DrawingAction()
    data class UpdateCompassTool(val compassTool: CompassTool) : DrawingAction()
    data class UpdateProtractorTool(val protractorTool: ProtractorTool) : DrawingAction()
    data object Undo : DrawingAction()
    data object Redo : DrawingAction()
    data object PerformHapticFeedback : DrawingAction()
}
