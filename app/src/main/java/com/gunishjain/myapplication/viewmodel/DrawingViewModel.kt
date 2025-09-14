package com.gunishjain.myapplication.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import com.gunishjain.myapplication.data.DrawingAction
import com.gunishjain.myapplication.data.DrawingState
import com.gunishjain.myapplication.data.UndoRedoManager
import com.gunishjain.myapplication.drawing.SnapEngine
import com.gunishjain.myapplication.drawing.tool.CompassTool
import com.gunishjain.myapplication.model.DrawingElement
import com.gunishjain.myapplication.model.DrawingTool
import com.gunishjain.myapplication.model.Point

/**
 * ViewModel for managing drawing state and actions
 */
class DrawingViewModel : ViewModel() {
    private val _drawingState = mutableStateOf(DrawingState())
    val drawingState: State<DrawingState> = _drawingState
    
    private val undoRedoManager = UndoRedoManager()
    
    // Initialize the SnapEngine with default values
    private val _snapEngine = SnapEngine(
        gridSpacing = _drawingState.value.gridSpacing,
        snapEnabled = _drawingState.value.snapEnabled,
        baseSnapRadius = 20f, // Using a default value instead of SnapTarget.DEFAULT_SNAP_DISTANCE
        currentZoomLevel = _drawingState.value.canvasScale
    )
    
    // Expose the SnapEngine for the UI layer
    val snapEngine: SnapEngine get() = _snapEngine
    
    fun handleAction(action: DrawingAction) {
        // Update the drawing state with the new action and its result
        val newState = when (action) {
            is DrawingAction.SetTool -> {
                println("DEBUG: DrawingViewModel - Setting tool to ${action.tool.name}")
                println("DEBUG: DrawingViewModel - Previous tool: ${_drawingState.value.currentTool.name}")
                var newState = _drawingState.value.copy(currentTool = action.tool)
                println("DEBUG: DrawingViewModel - New tool after copy: ${newState.currentTool.name}")
                
                // If switching away from Ruler tool, clear ruler state
                if (action.tool != DrawingTool.Ruler) {
                    newState = newState.copy(
                        rulerTool = newState.rulerTool.copy(isVisible = false)
                    )
                }
                
                // Clear Compass state when switching to other tools
                if (action.tool != DrawingTool.Compass) {
                    newState = newState.copy(
                        compassTool = newState.compassTool.copy(isVisible = false)
                    )
                }
                
                println("DEBUG: DrawingViewModel - Final tool: ${newState.currentTool.name}")
                newState
            }
            is DrawingAction.AddElement -> {
                val newElements = _drawingState.value.elements + action.element
                undoRedoManager.saveState(newElements)
                _drawingState.value.copy(
                    elements = newElements,
                    canUndo = undoRedoManager.canUndo(),
                    canRedo = undoRedoManager.canRedo()
                )
            }
            is DrawingAction.StartDrawing -> {
                _drawingState.value.copy(isDrawing = true)
            }
            is DrawingAction.UpdateDrawing -> {
                _drawingState.value
            }
            is DrawingAction.EndDrawing -> {
                _drawingState.value.copy(isDrawing = false)
            }
            is DrawingAction.ClearCanvas -> {
                // If canvas is already empty, clear all history
                if (_drawingState.value.elements.isEmpty()) {
                    undoRedoManager.clearHistory()
                    _drawingState.value.copy(
                        elements = emptyList(),
                        canUndo = false,
                        canRedo = false
                    )
                } else {
                    // If canvas has content, save current state before clearing
                    undoRedoManager.saveState(emptyList())
                    _drawingState.value.copy(
                        elements = emptyList(),
                        canUndo = undoRedoManager.canUndo(),
                        canRedo = undoRedoManager.canRedo()
                    )
                }
            }
            is DrawingAction.SetCanvasTransform -> {
                // Update the SnapEngine's zoom level for dynamic snap radius calculation
                _snapEngine.updateZoomLevel(action.scale)
                
                _drawingState.value.copy(
                    canvasOffset = action.offset,
                    canvasScale = action.scale,
                    canvasRotation = action.rotation
                )
            }
            is DrawingAction.SetStrokeColor -> {
                _drawingState.value.copy(strokeColor = action.color)
            }
            is DrawingAction.SetStrokeWidth -> {
                _drawingState.value.copy(strokeWidth = action.width)
            }
            is DrawingAction.ToggleSnap -> {
                _snapEngine.setSnapEnabled(action.enabled)
                _drawingState.value.copy(snapEnabled = action.enabled)
            }
            is DrawingAction.SetGridSpacing -> {
                _snapEngine.setGridSpacing(action.spacing)
                _drawingState.value.copy(gridSpacing = action.spacing)
            }
            is DrawingAction.PerformHapticFeedback -> {
                // This will be handled by the UI layer
                _drawingState.value
            }
            is DrawingAction.UpdateRulerTool -> {
                println("DEBUG: Updating ruler tool - visible: ${action.rulerTool.isVisible}")
                _drawingState.value.copy(rulerTool = action.rulerTool)
            }
            is DrawingAction.StartRulerDrag -> {
                println("DEBUG: Starting ruler drag")
                _drawingState.value.copy(
                    rulerTool = _drawingState.value.rulerTool.copy(isDragging = true)
                )
            }
            is DrawingAction.UpdateRulerDrag -> {
                if (_drawingState.value.rulerTool.isDragging) {
                    val deltaX = action.point.x - _drawingState.value.rulerTool.startPoint.x
                    val deltaY = action.point.y - _drawingState.value.rulerTool.startPoint.y
                    val updatedRuler = _drawingState.value.rulerTool.updatePosition(deltaX, deltaY)
                    _drawingState.value.copy(rulerTool = updatedRuler)
                } else {
                    _drawingState.value
                }
            }
            is DrawingAction.EndRulerDrag -> {
                println("DEBUG: Ending ruler drag")
                _drawingState.value.copy(
                    rulerTool = _drawingState.value.rulerTool.copy(isDragging = false)
                )
            }
            is DrawingAction.StartRulerRotation -> {
                _drawingState.value.copy(
                    rulerTool = _drawingState.value.rulerTool.copy(isRotating = true)
                )
            }
            is DrawingAction.UpdateRulerRotation -> {
                if (_drawingState.value.rulerTool.isRotating) {
                    val centerX = (_drawingState.value.rulerTool.startPoint.x + _drawingState.value.rulerTool.endPoint.x) / 2f
                    val centerY = (_drawingState.value.rulerTool.startPoint.y + _drawingState.value.rulerTool.endPoint.y) / 2f
                    val angle = Math.toDegrees(
                        Math.atan2(
                            (action.point.y - centerY).toDouble(),
                            (action.point.x - centerX).toDouble()
                        )
                    ).toFloat()
                    val updatedRuler = _drawingState.value.rulerTool.updateRotation(centerX, centerY, angle)
                    _drawingState.value.copy(rulerTool = updatedRuler)
                } else {
                    _drawingState.value
                }
            }
            is DrawingAction.EndRulerRotation -> {
                _drawingState.value.copy(
                    rulerTool = _drawingState.value.rulerTool.copy(isRotating = false)
                )
            }
            is DrawingAction.Undo -> {
                val previousElements = undoRedoManager.undo()
                if (previousElements != null) {
                    _drawingState.value.copy(
                        elements = previousElements,
                        canUndo = undoRedoManager.canUndo(),
                        canRedo = undoRedoManager.canRedo()
                    )
                } else {
                    _drawingState.value
                }
            }
            is DrawingAction.Redo -> {
                val nextElements = undoRedoManager.redo()
                if (nextElements != null) {
                    _drawingState.value.copy(
                        elements = nextElements,
                        canUndo = undoRedoManager.canUndo(),
                        canRedo = undoRedoManager.canRedo()
                    )
                } else {
                    _drawingState.value
                }
            }
            is DrawingAction.UpdateCompassTool -> {
                _drawingState.value.copy(compassTool = action.compassTool)
            }
        }
        
        // Set the new state with the lastAction property
        _drawingState.value = newState.copy(lastAction = action)
    }
}