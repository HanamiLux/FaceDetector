package com.example.a3balodetector

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.animation.AnimationUtils
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.a3balodetector.databinding.ActivityMainBinding
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

// Request code for camera permission
private const val CAMERA_PERMISSION_REQUEST_CODE = 100

// Lazy initialization of the face detector
private val faceDetector by lazy {
    val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
        .setMinFaceSize(0.1f)
        .build()
    FaceDetection.getClient(options)
}

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var isMasked = false

    // Flag to track if the front camera is in use
    private var isFrontCamera: Boolean = true // Default to front camera

    // Lazy initialization of the graphical elements (e.g., masks)
    private val uwuBitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(this.resources, R.drawable.uwu)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize view binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Enable edge-to-edge display
        enableEdgeToEdge()

        // Handle window insets (e.g., status bar, navigation bar)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Check and request camera permission
        checkCameraPermission()

        // Set up the switch camera button
        binding.switchCameraButton.setOnClickListener {
            // Play scale animations
            val scaleUp = AnimationUtils.loadAnimation(this, R.anim.scale_up)
            val scaleDown = AnimationUtils.loadAnimation(this, R.anim.scale_down)

            scaleUp.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                override fun onAnimationStart(animation: android.view.animation.Animation?) {}
                override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                    // After scale up, start scale down
                    binding.switchCameraButton.startAnimation(scaleDown)
                }
                override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
            })

            scaleDown.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                override fun onAnimationStart(animation: android.view.animation.Animation?) {}
                override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                    // After scale down, toggle the camera with a slight delay
                    Handler(Looper.getMainLooper()).postDelayed({
                        isFrontCamera = !isFrontCamera
                        updateCameraIcon() // Update the icon and gradient
                        startCamera() // Restart the camera with the new selection
                    }, 0) // 0ms delay
                }
                override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
            })

            binding.switchCameraButton.startAnimation(scaleUp)
        }

        // Set up the mask button
        binding.maskButton.setOnClickListener {
            if(!isMasked)
                binding.faceContourView.setCustomBitmap(uwuBitmap)
            else
                binding.faceContourView.setCustomBitmap(null)
                isMasked = !isMasked
        }
    }

    // Update the camera icon and gradient based on the current camera mode
    private fun updateCameraIcon() {
        if (isFrontCamera) {
            binding.switchCameraButton.setImageResource(R.drawable.ic_camera_front)
            binding.switchCameraButton.background = ContextCompat.getDrawable(this, R.drawable.gradient_button_bg)
        } else {
            binding.switchCameraButton.setImageResource(R.drawable.ic_camera_back)
            binding.switchCameraButton.background = ContextCompat.getDrawable(this, R.drawable.gradient_button_bg2)
        }

        // Play fade animations
        val fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out)
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        binding.switchCameraButton.startAnimation(fadeOut)
        binding.switchCameraButton.startAnimation(fadeIn)
    }

    // Check if the app has camera permission
    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // Request camera permission if not granted
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
        } else {
            // Start the camera if permission is already granted
            startCamera()
        }
    }

    // Handle the result of the permission request
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Start the camera if permission is granted
            startCamera()
        }
    }

    // Start the camera and set up the preview and analysis
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                // Set up the preview use case
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }

                // Set up the image analysis use case
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        // Pass the isFrontCamera flag to FaceContourAnalyzer
                        it.setAnalyzer(ContextCompat.getMainExecutor(this), FaceContourAnalyzer(faceDetector, binding, isFrontCamera))
                    }

                // Select the camera based on the isFrontCamera flag
                val cameraSelector = if (isFrontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                try {
                    // Unbind all use cases before rebinding
                    cameraProvider.unbindAll()

                    // Bind the camera to the lifecycle
                    cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
                } catch (e: Exception) {
                    Log.e("CameraX", "Use case binding failed", e)
                    // Show a message to the user if camera binding fails
                }
            } catch (e: Exception) {
                Log.e("CameraX", "Camera provider failed", e)
                // Show a message to the user if camera provider fails
            }
        }, ContextCompat.getMainExecutor(this))
    }
}