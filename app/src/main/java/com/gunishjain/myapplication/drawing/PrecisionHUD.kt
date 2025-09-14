package com.gunishjain.myapplication.drawing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gunishjain.myapplication.drawing.tool.RulerTool
import com.gunishjain.myapplication.drawing.tool.CompassTool
import com.gunishjain.myapplication.drawing.tool.ProtractorTool

/**
 * A composable that displays precision measurements for the ruler tool
 */
@Composable
fun PrecisionHUD(
    rulerTool: RulerTool?,
    isVisible: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (!isVisible || rulerTool == null) return
    
    val distance = rulerTool.calculateDistance()
    val isHorizontalOrVertical = rulerTool.isHorizontalOrVertical()
    val angle = rulerTool.calculateAngle()
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0x99000000))
                .padding(12.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "Distance: ${String.format("%.1f", distance * 0.0264583333)} cm",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Angle: ${String.format("%.1f", angle)}°",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            
            if (isHorizontalOrVertical) {
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = if (angle % 180 < 45 || angle % 180 > 135) "Horizontal" else "Vertical",
                    color = Color.Green,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * A composable that displays precision measurements for the compass tool
 */
@Composable
fun CompassPrecisionHUD(
    compassTool: CompassTool?,
    isVisible: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (!isVisible || compassTool == null) return
    
    val radius = compassTool.radius
    val radiusInCm = radius * 0.0264583333 // Convert pixels to cm (same conversion as ruler)
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0x99000000))
                .padding(12.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "Radius: ${String.format("%.1f", radiusInCm)} cm",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Diameter: ${String.format("%.1f", radiusInCm * 2)} cm",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Center: (${String.format("%.0f", compassTool.center.x)}, ${String.format("%.0f", compassTool.center.y)})",
                color = Color.Green,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * A composable that displays precision measurements for the protractor tool
 */
@Composable
fun ProtractorPrecisionHUD(
    protractorTool: ProtractorTool?,
    isVisible: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (!isVisible || protractorTool == null) return
    
    val angle = protractorTool.angle
    val firstLineLength = protractorTool.getFirstLineLength()
    val secondLineLength = protractorTool.getSecondLineLength()
    val firstLineLengthInCm = firstLineLength * 0.0264583333 // Convert pixels to cm
    val secondLineLengthInCm = secondLineLength * 0.0264583333 // Convert pixels to cm
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0x99000000))
                .padding(12.dp),
            horizontalAlignment = Alignment.End
        ) {
            if (angle > 0) {
                val nearestAngle = protractorTool.findNearestCommonAngle(angle)
                val isSnapped = nearestAngle != null
                
                Text(
                    text = "Angle: ${String.format("%.1f", angle)}°${if (isSnapped) " (Snapped)" else ""}",
                    color = if (isSnapped) Color.Green else Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                
                if (isSnapped) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Snapped to: ${nearestAngle}°",
                        color = Color.Green,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
            }
            
            if (protractorTool.firstLineComplete) {
                Text(
                    text = "Line 1: ${String.format("%.1f", firstLineLengthInCm)} cm",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(2.dp))
            }
            
            if (protractorTool.secondLineComplete) {
                Text(
                    text = "Line 2: ${String.format("%.1f", secondLineLengthInCm)} cm",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(2.dp))
            }
            
            Text(
                text = "Vertex: (${String.format("%.0f", protractorTool.vertex.x)}, ${String.format("%.0f", protractorTool.vertex.y)})",
                color = Color.Green,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}