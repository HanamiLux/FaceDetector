package com.example.a3balodetector

import android.graphics.PointF
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.a3balodetector.databinding.ActivityMainBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetector

class FaceContourAnalyzer(
    private val faceDetector: FaceDetector,
    private val binding: ActivityMainBinding,
    private val isFrontCamera: Boolean
) : ImageAnalysis.Analyzer {

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            // Convert the media image to InputImage for ML Kit
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            // Process the image to detect faces
            faceDetector.process(image)
                .addOnSuccessListener { faces ->
                    Log.d("FaceDetection", "Number of faces detected: ${faces.size}")
                    // Transform face contours to screen coordinates
                    val transformedFaces = faces.map { face ->
                        transformFaceContours(face, imageProxy.width, imageProxy.height)
                    }
                    // Draw contours on the detected faces
                    drawFaceContours(transformedFaces)
                }
                .addOnFailureListener { e ->
                    Log.e("FaceDetection", "Face detection failed", e)
                }
                .addOnCompleteListener {
                    // Close the image proxy after processing
                    imageProxy.close()
                }
        }
    }

    // Transform face contours to screen coordinates
    private fun transformFaceContours(face: Face, imageWidth: Int, imageHeight: Int): TransformedFace {
        val previewView = binding.previewView
        val scaleX = previewView.width.toFloat() / (imageWidth/2)
        val scaleY = previewView.height.toFloat() / imageHeight - 1
        val frontOffsetX = 800
        val frontOffsetY = 50
        val backOffsetX = 250
        // Scale the face contours
        val scaledContours = face.allContours.map { contour ->
            contour.points.map { point ->
                if (isFrontCamera) {
                    // Invert the contour points horizontally for front camera
                    PointF((imageWidth - point.x) * scaleX - frontOffsetX, point.y * scaleY - frontOffsetY)
                } else {
                    PointF(point.x * scaleX - backOffsetX, point.y * scaleY )
                }
            }
        }

        // Return a custom object with scaled contours
        return TransformedFace(scaledContours)
    }

    // Update the FaceContourView with the detected faces
    private fun drawFaceContours(faces: List<TransformedFace>) {
        binding.faceContourView.faces = faces
        binding.faceContourView.invalidate()
    }

    // Custom data class to hold transformed face data
    data class TransformedFace(
        val contours: List<List<PointF>>
    )
}