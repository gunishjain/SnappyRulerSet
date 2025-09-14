package com.gunishjain.myapplication.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import com.gunishjain.myapplication.data.DrawingState
import com.gunishjain.myapplication.drawing.SnapEngine
import com.gunishjain.myapplication.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.graphics.createBitmap
import com.gunishjain.myapplication.drawing.tool.CompassTool
import com.gunishjain.myapplication.drawing.tool.RulerTool
import com.gunishjain.myapplication.drawing.tool.ProtractorTool
import kotlin.math.atan2

/**
 * Bitmap-based exporter that renders the Compose Canvas to a bitmap
 */
object BitmapExporter {
    
    /**
     * Export format options
     */
    enum class ExportFormat(val extension: String, val mimeType: String) {
        PNG("png", "image/png"),
        JPEG("jpg", "image/jpeg")
    }
    
    /**
     * Export the drawing state to an image file using Compose Canvas
     */
    suspend fun exportDrawing(
        context: Context,
        drawingState: DrawingState,
        snapEngine: SnapEngine,
        canvasWidth: Int,
        canvasHeight: Int,
        format: ExportFormat,
        quality: Int = 90
    ): Result<String> = withContext(Dispatchers.Main) {
        try {
            // Create bitmap
            val bitmap = createBitmapFromComposeCanvas(
                drawingState = drawingState,
                snapEngine = snapEngine,
                width = canvasWidth,
                height = canvasHeight
            )
            
            // Save to storage
            val fileName = generateFileName(format)
            val uri = saveBitmapToStorage(context, bitmap, fileName, format, quality)
            
            Result.success("Drawing exported successfully: $fileName")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create a bitmap by rendering the Compose Canvas
     */
    private suspend fun createBitmapFromComposeCanvas(
        drawingState: DrawingState,
        snapEngine: SnapEngine,
        width: Int,
        height: Int
    ): Bitmap = withContext(Dispatchers.Main) {
        val bitmap = createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val imageBitmap = bitmap.asImageBitmap()
        val canvas = androidx.compose.ui.graphics.Canvas(imageBitmap)
        
        // Render the drawing state to the bitmap
        renderDrawingState(canvas, drawingState, snapEngine, width, height)
        
        bitmap
    }
    
    /**
     * Render the drawing state to a Compose Canvas
     */
    private fun renderDrawingState(
        canvas: androidx.compose.ui.graphics.Canvas,
        state: DrawingState,
        snapEngine: SnapEngine,
        width: Int,
        height: Int
    ) {
        // Fill with white background
        canvas.drawRect(
            rect = Rect(0f, 0f, width.toFloat(), height.toFloat()),
            paint = androidx.compose.ui.graphics.Paint().apply {
                color = Color.White
            }
        )
        
        // Draw grid (always visible in export)
        drawGrid(canvas, state.gridSpacing, width, height)
        
        // Draw all elements
        println("BitmapExporter: Drawing ${state.elements.size} elements")
        state.elements.forEachIndexed { index, element ->
            println("BitmapExporter: Drawing element $index: ${element::class.simpleName}")
            drawElement(canvas, element)
        }
        
        // Draw active tools
        if (state.rulerTool.isVisible) {
            drawRulerTool(canvas, state.rulerTool)
        }
        
        if (state.compassTool.isVisible) {
            drawCompassTool(canvas, state.compassTool)
        }
        
        if (state.protractorTool.isVisible) {
            drawProtractorTool(canvas, state.protractorTool)
        }
    }
    
    /**
     * Draw grid lines
     */
    private fun drawGrid(
        canvas: androidx.compose.ui.graphics.Canvas,
        gridSpacing: Float,
        width: Int,
        height: Int
    ) {
        val paint = androidx.compose.ui.graphics.Paint().apply {
            color = Color.Gray.copy(alpha = 0.3f)
            strokeWidth = 1f
        }
        
        // Draw vertical lines
        var x = 0f
        while (x <= width) {
            canvas.drawLine(
                p1 = Offset(x, 0f),
                p2 = Offset(x, height.toFloat()),
                paint = paint
            )
            x += gridSpacing
        }
        
        // Draw horizontal lines
        var y = 0f
        while (y <= height) {
            canvas.drawLine(
                p1 = Offset(0f, y),
                p2 = Offset(width.toFloat(), y),
                paint = paint
            )
            y += gridSpacing
        }
    }
    
    /**
     * Draw a single element
     */
    private fun drawElement(canvas: androidx.compose.ui.graphics.Canvas, element: DrawingElement) {
        when (element) {
            is DrawingElement.LineElement -> {
                val paint = androidx.compose.ui.graphics.Paint().apply {
                    color = element.line.color
                    strokeWidth = element.line.strokeWidth
                    style = androidx.compose.ui.graphics.PaintingStyle.Stroke
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    strokeJoin = androidx.compose.ui.graphics.StrokeJoin.Round
                }
                canvas.drawLine(
                    p1 = Offset(element.line.start.x, element.line.start.y),
                    p2 = Offset(element.line.end.x, element.line.end.y),
                    paint = paint
                )
            }
            is DrawingElement.StrokeElement -> {
                println("BitmapExporter: Drawing StrokeElement with color: ${element.stroke.color}, width: ${element.stroke.strokeWidth}")
                val paint = androidx.compose.ui.graphics.Paint().apply {
                    color = element.stroke.color
                    strokeWidth = element.stroke.strokeWidth
                    style = androidx.compose.ui.graphics.PaintingStyle.Stroke
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    strokeJoin = androidx.compose.ui.graphics.StrokeJoin.Round
                }
                canvas.drawPath(element.stroke.path, paint)
            }
            is DrawingElement.StrokeWithPointsElement -> {
                val paint = androidx.compose.ui.graphics.Paint().apply {
                    color = element.stroke.color
                    strokeWidth = element.stroke.strokeWidth
                    style = androidx.compose.ui.graphics.PaintingStyle.Stroke
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    strokeJoin = androidx.compose.ui.graphics.StrokeJoin.Round
                }
                val path = createPathFromPoints(element.stroke.points)
                canvas.drawPath(path, paint)
            }
            is DrawingElement.CircleElement -> {
                val paint = androidx.compose.ui.graphics.Paint().apply {
                    color = element.circle.color
                    strokeWidth = element.circle.strokeWidth
                    style = androidx.compose.ui.graphics.PaintingStyle.Stroke
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                }
                canvas.drawCircle(
                    center = Offset(element.circle.center.x, element.circle.center.y),
                    radius = element.circle.radius,
                    paint = paint
                )
            }
            is DrawingElement.AngleElement -> {
                // TODO: Implement angle drawing if needed
            }
        }
    }
    
    /**
     * Draw ruler tool
     */
    private fun drawRulerTool(canvas: androidx.compose.ui.graphics.Canvas, rulerTool: RulerTool) {
        val paint = androidx.compose.ui.graphics.Paint().apply {
            color = Color.Blue.copy(alpha = 0.5f)
            strokeWidth = 2f
            style = androidx.compose.ui.graphics.PaintingStyle.Stroke
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            strokeJoin = androidx.compose.ui.graphics.StrokeJoin.Round
        }
        canvas.drawLine(
            p1 = Offset(rulerTool.startPoint.x, rulerTool.startPoint.y),
            p2 = Offset(rulerTool.endPoint.x, rulerTool.endPoint.y),
            paint = paint
        )
    }
    
    /**
     * Draw compass tool
     */
    private fun drawCompassTool(canvas: androidx.compose.ui.graphics.Canvas, compassTool: CompassTool) {
        val paint = androidx.compose.ui.graphics.Paint().apply {
            color = Color.Red.copy(alpha = 0.5f)
            strokeWidth = 2f
            style = androidx.compose.ui.graphics.PaintingStyle.Stroke
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        }
        canvas.drawCircle(
            center = Offset(compassTool.center.x, compassTool.center.y),
            radius = compassTool.radius,
            paint = paint
        )
    }
    
    /**
     * Draw protractor tool
     */
    private fun drawProtractorTool(canvas: androidx.compose.ui.graphics.Canvas, protractorTool: ProtractorTool) {
        val vertex = Offset(protractorTool.vertex.x, protractorTool.vertex.y)
        val firstEndpoint = Offset(protractorTool.firstEndpoint.x, protractorTool.firstEndpoint.y)
        val secondEndpoint = Offset(protractorTool.secondEndpoint.x, protractorTool.secondEndpoint.y)
        
        // Draw the two lines from vertex to endpoints
        val linePaint = androidx.compose.ui.graphics.Paint().apply {
            color = Color.Red
            strokeWidth = 4f
            style = androidx.compose.ui.graphics.PaintingStyle.Stroke
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        }
        
        if (protractorTool.firstEndpoint != protractorTool.vertex) {
            canvas.drawLine(
                p1 = vertex,
                p2 = firstEndpoint,
                paint = linePaint
            )
        }
        
        if (protractorTool.secondEndpoint != protractorTool.vertex) {
            canvas.drawLine(
                p1 = vertex,
                p2 = secondEndpoint,
                paint = linePaint
            )
        }
        
        // Draw draggable endpoint circles
        val circlePaint = androidx.compose.ui.graphics.Paint().apply {
            color = Color.Blue
            strokeWidth = 2f
            style = androidx.compose.ui.graphics.PaintingStyle.Stroke
        }
        
        if (protractorTool.firstEndpoint != protractorTool.vertex) {
            canvas.drawCircle(
                center = firstEndpoint,
                radius = 8f,
                paint = circlePaint
            )
        }
        
        if (protractorTool.secondEndpoint != protractorTool.vertex) {
            canvas.drawCircle(
                center = secondEndpoint,
                radius = 8f,
                paint = circlePaint
            )
        }
        
        // Draw angle arc
        if (protractorTool.firstEndpoint != protractorTool.vertex && protractorTool.secondEndpoint != protractorTool.vertex) {
            val angle = protractorTool.angle
            if (angle > 0) {
                val arcPaint = androidx.compose.ui.graphics.Paint().apply {
                    color = Color(0xFFFF6600) // Orange color
                    strokeWidth = 3f
                    style = androidx.compose.ui.graphics.PaintingStyle.Stroke
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                }
                
                val arcRadius = 30f
                val startAngle = atan2(firstEndpoint.y - vertex.y, firstEndpoint.x - vertex.x)
                val sweepAngle = atan2(secondEndpoint.y - vertex.y, secondEndpoint.x - vertex.x) - startAngle
                
                canvas.drawArc(
                    left = vertex.x - arcRadius,
                    top = vertex.y - arcRadius,
                    right = vertex.x + arcRadius,
                    bottom = vertex.y + arcRadius,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    paint = arcPaint
                )
            }
        }
    }
    
    /**
     * Create Compose Path from points
     */
    private fun createPathFromPoints(points: List<Point>): Path {
        val path = Path()
        
        if (points.isEmpty()) return path
        
        // Move to first point
        path.moveTo(points[0].x, points[0].y)
        
        // Draw lines to subsequent points
        for (i in 1 until points.size) {
            path.lineTo(points[i].x, points[i].y)
        }
        
        return path
    }
    
    /**
     * Save bitmap to device storage
     */
    private fun saveBitmapToStorage(
        context: Context,
        bitmap: Bitmap,
        fileName: String,
        format: ExportFormat,
        quality: Int
    ): android.net.Uri {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, format.mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SnappyRulerSet")
            }
        }
        
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw Exception("Failed to create file")
        
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            bitmap.compress(
                if (format == ExportFormat.PNG) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG,
                if (format == ExportFormat.PNG) 100 else quality,
                outputStream
            )
        }
        
        return uri
    }
    
    /**
     * Generate a unique filename with timestamp
     */
    private fun generateFileName(format: ExportFormat): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "SnappyRulerSet_$timestamp.${format.extension}"
    }
}
