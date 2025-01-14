package com.example.a3balodetector

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class FaceContourView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    // Paint for drawing face contours
    private val contourPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    // Bitmap to draw over detected faces
    private var customBitmap: Bitmap? = null

    // List of detected faces to draw
    var faces: List<FaceContourAnalyzer.TransformedFace> = emptyList()
        set(value) {
            field = value
            invalidate() // Redraw the view when the list of faces is updated
        }

    // Reusable RectF for drawing the bitmap
    private val maskRect = RectF()

    // Reusable Path for drawing face contours
    private val contourPath = Path()

    // Method to set the custom bitmap
    fun setCustomBitmap(bitmap: Bitmap?) {
        this.customBitmap = bitmap
        invalidate() // Redraw the view
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // If no custom bitmap is set, draw face contours
        if (customBitmap == null) {
            drawFaceContours(canvas)
        } else {
            // If a custom bitmap is set, draw it over the detected faces
            drawCustomBitmapOverFaces(canvas)
        }
    }

    // Method to draw face contours
    private fun drawFaceContours(canvas: Canvas) {
        for (face in faces) {
            for (contour in face.contours) {
                contourPath.reset() // Clear the path before reusing it
                if (contour.isNotEmpty()) {
                    contourPath.moveTo(contour[0].x, contour[0].y)
                    for (i in 1 until contour.size) {
                        contourPath.lineTo(contour[i].x, contour[i].y)
                    }
                    contourPath.close()
                }
                canvas.drawPath(contourPath, contourPaint)
            }
        }
    }

    // Method to draw the custom bitmap over detected faces
    private fun drawCustomBitmapOverFaces(canvas: Canvas) {
        for (face in faces) {
            if (face.contours.isNotEmpty()) {
                // Calculate the position and size for the bitmap
                val faceWidth = face.contours[0].maxByOrNull { it.x }?.x?.minus(
                    face.contours[0].minByOrNull { it.x }?.x ?: 0f
                ) ?: 0f
                val minY = face.contours[0].minByOrNull { it.y }?.y ?: 0f
                val maxY = face.contours[0].maxByOrNull { it.y }?.y ?: 0f
                val midY = (minY + maxY) / 2f
                val faceHeight = maxY - minY

                // Calculate the dimensions and position for the bitmap
                val maskWidth = faceWidth * 1.2f
                val maskHeight = customBitmap!!.height * (maskWidth / customBitmap!!.width)
                val maskLeft = face.contours[0].minByOrNull { it.x }?.x?.minus((maskWidth - faceWidth) / 2) ?: 0f
                val maskTop = midY - faceHeight * 0.2f

                // Set the RectF and draw the bitmap
                maskRect.set(maskLeft, maskTop, maskLeft + maskWidth, maskTop + maskHeight)
                canvas.drawBitmap(customBitmap!!, null, maskRect, null)
            }
        }
    }
}