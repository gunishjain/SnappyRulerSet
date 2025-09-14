package com.gunishjain.myapplication.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gunishjain.myapplication.model.DrawingElement
import com.gunishjain.myapplication.model.Point
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility class for exporting drawings as PNG or JPEG images
 */
object ImageExporter {
    
    /**
     * Export format options
     */
    enum class ExportFormat(val extension: String, val mimeType: String) {
        PNG("png", "image/png"),
        JPEG("jpg", "image/jpeg")
    }
    
    /**
     * Export the drawing elements to an image file
     * @param context Android context
     * @param elements List of drawing elements to export
     * @param canvasWidth Width of the canvas in pixels
     * @param canvasHeight Height of the canvas in pixels
     * @param format Export format (PNG or JPEG)
     * @param quality JPEG quality (1-100, ignored for PNG)
     * @return Result indicating success or failure
     */
    suspend fun exportDrawing(
        context: Context,
        elements: List<DrawingElement>,
        canvasWidth: Int,
        canvasHeight: Int,
        format: ExportFormat,
        quality: Int = 90
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Create bitmap
            val bitmap = createBitmapFromElements(
                elements = elements,
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
     * Create a bitmap from drawing elements
     */
    private fun createBitmapFromElements(
        elements: List<DrawingElement>,
        width: Int,
        height: Int
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Fill with white background
        canvas.drawColor(Color.WHITE)
        
        // Debug: Log the number of elements
        println("ImageExporter: Drawing ${elements.size} elements")
        
        // Draw all elements
        elements.forEachIndexed { index, element ->
            println("ImageExporter: Drawing element $index: ${element::class.simpleName}")
            drawElementOnCanvas(canvas, element)
        }
        
        return bitmap
    }
    
    /**
     * Draw a single element on the Android Canvas
     */
    private fun drawElementOnCanvas(canvas: Canvas, element: DrawingElement) {
        when (element) {
            is DrawingElement.LineElement -> {
                println("ImageExporter: Drawing line from (${element.line.start.x}, ${element.line.start.y}) to (${element.line.end.x}, ${element.line.end.y})")
                val paint = Paint().apply {
                    color = element.line.color.toArgb()
                    strokeWidth = element.line.strokeWidth
                    style = Paint.Style.STROKE
                    isAntiAlias = true
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }
                canvas.drawLine(
                    element.line.start.x,
                    element.line.start.y,
                    element.line.end.x,
                    element.line.end.y,
                    paint
                )
            }
            is DrawingElement.StrokeElement -> {
                println("ImageExporter: Drawing stroke with width ${element.stroke.strokeWidth}")
                val paint = Paint().apply {
                    color = element.stroke.color.toArgb()
                    strokeWidth = element.stroke.strokeWidth
                    style = Paint.Style.STROKE
                    isAntiAlias = true
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }
                val path = convertPathToAndroid(element.stroke.path)
                canvas.drawPath(path, paint)
            }
            is DrawingElement.StrokeWithPointsElement -> {
                println("ImageExporter: Drawing stroke with ${element.stroke.points.size} points")
                val paint = Paint().apply {
                    color = element.stroke.color.toArgb()
                    strokeWidth = element.stroke.strokeWidth
                    style = Paint.Style.STROKE
                    isAntiAlias = true
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }
                val path = createPathFromPoints(element.stroke.points)
                canvas.drawPath(path, paint)
            }
            is DrawingElement.CircleElement -> {
                println("ImageExporter: Drawing circle at (${element.circle.center.x}, ${element.circle.center.y}) with radius ${element.circle.radius}")
                val paint = Paint().apply {
                    color = element.circle.color.toArgb()
                    strokeWidth = element.circle.strokeWidth
                    style = Paint.Style.STROKE
                    isAntiAlias = true
                    strokeCap = Paint.Cap.ROUND
                }
                canvas.drawCircle(
                    element.circle.center.x,
                    element.circle.center.y,
                    element.circle.radius,
                    paint
                )
            }
            is DrawingElement.AngleElement -> {
                println("ImageExporter: Drawing angle element")
                // TODO: Implement angle drawing if needed
            }
        }
    }
    
    /**
     * Convert Compose Path to Android Path
     * This is a simplified conversion that approximates the path
     */
    private fun convertPathToAndroid(composePath: androidx.compose.ui.graphics.Path): Path {
        val androidPath = Path()
        
        // Since we can't directly access the internal path data from Compose Path,
        // we'll create a simple approximation by sampling points along the path
        // This is a workaround - in a real implementation, you'd need to store
        // the path points separately or use a different approach
        
        // For now, we'll create a simple line as a placeholder
        // In a real app, you'd want to store the actual path points
        androidPath.moveTo(0f, 0f)
        androidPath.lineTo(100f, 100f)
        
        return androidPath
    }
    
    /**
     * Create Android Path from a list of points
     */
    private fun createPathFromPoints(points: List<Point>): Path {
        val androidPath = Path()
        
        if (points.isEmpty()) return androidPath
        
        // Move to first point
        androidPath.moveTo(points[0].x, points[0].y)
        
        // Draw lines to subsequent points
        for (i in 1 until points.size) {
            androidPath.lineTo(points[i].x, points[i].y)
        }
        
        return androidPath
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
