package com.gunishjain.myapplication.model

import androidx.compose.ui.graphics.Color

/**
 * Sealed class defining all available drawing tools
 */
sealed class DrawingTool(
    val name: String,
    val icon: String,
    val color: Color
) {
    data object Freehand : DrawingTool(
        name = "Freehand",
        icon = "✏️",
        color = Color.Black
    )
    
    data object Ruler : DrawingTool(
        name = "Ruler",
        icon = "📏",
        color = Color.Blue
    )
    
    data object SetSquare45 : DrawingTool(
        name = "Set Square 45°",
        icon = "📐",
        color = Color.Green
    )
    
    data object SetSquare30 : DrawingTool(
        name = "Set Square 30°",
        icon = "📐",
        color = Color.Green
    )
    
    data object Protractor : DrawingTool(
        name = "Protractor",
        icon = "📊",
        color = Color.Red
    )
    
    data object Compass : DrawingTool(
        name = "Compass",
        icon = "⭕",
        color = Color.Magenta
    )
}

/**
 * All available drawing tools
 */
val ALL_DRAWING_TOOLS = listOf(
    DrawingTool.Freehand,
    DrawingTool.Ruler,
    DrawingTool.SetSquare45,
    DrawingTool.SetSquare30,
    DrawingTool.Protractor,
    DrawingTool.Compass
)
