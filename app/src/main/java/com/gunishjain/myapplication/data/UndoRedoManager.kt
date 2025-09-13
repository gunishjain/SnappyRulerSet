package com.gunishjain.myapplication.data

import com.gunishjain.myapplication.model.DrawingElement

/**
 * Manages undo/redo operations for drawing elements
 * Supports configurable history limit (default: 20 steps)
 */
class UndoRedoManager(
    private val maxHistorySize: Int = 20
) {
    private val undoStack = mutableListOf<List<DrawingElement>>()
    private val redoStack = mutableListOf<List<DrawingElement>>()
    
    /**
     * Current state of drawing elements
     */
    private var currentState = emptyList<DrawingElement>()
    
    /**
     * Save current state to undo history
     */
    fun saveState(elements: List<DrawingElement>) {
        // Don't save if state hasn't changed
        if (elements == currentState) return
        
        // Add current state to undo stack
        undoStack.add(currentState)
        
        // Limit undo stack size
        if (undoStack.size > maxHistorySize) {
            undoStack.removeAt(0)
        }
        
        // Clear redo stack when new action is performed
        redoStack.clear()
        
        // Update current state
        currentState = elements
    }
    
    /**
     * Undo the last action
     * @return Previous state if available, null if nothing to undo
     */
    fun undo(): List<DrawingElement>? {
        if (undoStack.isEmpty()) return null
        
        // Move current state to redo stack
        redoStack.add(currentState)
        
        // Get previous state from undo stack
        val previousState = undoStack.removeAt(undoStack.size - 1)
        currentState = previousState
        
        return previousState
    }
    
    /**
     * Redo the last undone action
     * @return Next state if available, null if nothing to redo
     */
    fun redo(): List<DrawingElement>? {
        if (redoStack.isEmpty()) return null
        
        // Move current state to undo stack
        undoStack.add(currentState)
        
        // Get next state from redo stack
        val nextState = redoStack.removeAt(redoStack.size - 1)
        currentState = nextState
        
        return nextState
    }
    
    /**
     * Check if undo is available
     */
    fun canUndo(): Boolean = undoStack.isNotEmpty()
    
    /**
     * Check if redo is available
     */
    fun canRedo(): Boolean = redoStack.isNotEmpty()
    
    /**
     * Get current history size
     */
    fun getHistorySize(): Int = undoStack.size
    
    /**
     * Clear all history
     */
    fun clearHistory() {
        undoStack.clear()
        redoStack.clear()
        currentState = emptyList()
    }
    
    /**
     * Update max history size
     */
    fun setMaxHistorySize(size: Int) {
        // Trim undo stack if new size is smaller
        while (undoStack.size > size) {
            undoStack.removeAt(0)
        }
    }
}
