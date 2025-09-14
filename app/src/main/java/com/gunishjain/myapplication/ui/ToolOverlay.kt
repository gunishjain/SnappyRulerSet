package com.gunishjain.myapplication.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gunishjain.myapplication.data.DrawingAction
import com.gunishjain.myapplication.model.DrawingTool
import com.gunishjain.myapplication.model.ALL_DRAWING_TOOLS

/**
 * Tool selection overlay with chips for each drawing tool
 */
@Composable
fun ToolOverlay(
    currentTool: DrawingTool,
    onToolSelected: (DrawingTool) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Drawing Tools",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(ALL_DRAWING_TOOLS) { tool ->
                    ToolChip(
                        tool = tool,
                        isSelected = tool == currentTool,
                        onClick = { 
                            println("DEBUG: ToolOverlay - Tool selected: ${tool.name}")
                            onToolSelected(tool) 
                        }
                    )
                }
            }
        }
    }
}

/**
 * Individual tool chip component
 */
@Composable
private fun ToolChip(
    tool: DrawingTool,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        tool.color.copy(alpha = 0.2f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    val borderColor = if (isSelected) {
        tool.color
    } else {
        MaterialTheme.colorScheme.outline
    }
    
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = tool.icon,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = tool.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) tool.color else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
