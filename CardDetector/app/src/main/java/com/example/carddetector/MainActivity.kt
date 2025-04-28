package com.example.carddetector
import android.content.ContentValues
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.carddetector.databinding.ActivityMainBinding
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var responsesDao: ResponsesDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences("CardDetector", Context.MODE_PRIVATE)
        responsesDao = ResponsesDao(this)

        val serverIp = sharedPreferences.getString("SERVER_IP", null)
        if (serverIp == null) {
            showIpInput()
        } else {
            showCamera()
        }
    }
    private fun saveImageToGallery(imageBytes: ByteArray) {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        uri?.let { imageUri ->
            try {
                contentResolver.openOutputStream(imageUri)?.use { outputStream ->
                    outputStream.write(imageBytes)
                }
                Toast.makeText(this, "Image saved to gallery", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun showIpInput() {
        val view = layoutInflater.inflate(R.layout.ip_input, null)
        setContentView(view)

        val ipInput = view.findViewById<TextInputEditText>(R.id.ipInput)
        val connectButton = view.findViewById<Button>(R.id.connectButton)

        connectButton.setOnClickListener {
            val ip = ipInput.text.toString()
            if (ip.isNotEmpty()) {
                sharedPreferences.edit().putString("SERVER_IP", ip).apply()
                showCamera()
            }
        }
    }

    private fun showCamera() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        binding.captureButton.setOnClickListener { takePhoto() }
        binding.logsButton.setOnClickListener { showLogs() }
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        binding.progressBar.visibility = View.VISIBLE
        binding.progressText.visibility = View.VISIBLE
        binding.progressText.text = "Processing..."
        binding.captureButton.isEnabled = false

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    val base64Image = Base64.encodeToString(bytes, Base64.DEFAULT)

                    // Save image to gallery
                    saveImageToGallery(bytes)

                    sendImageToServer(base64Image)
                    image.close()
                }

                override fun onError(exc: ImageCaptureException) {
                    handleError("Failed to capture image: ${exc.message}")
                }
            }
        )
    }

    private fun sendImageToServer(base64Image: String) {
        val serverIp = sharedPreferences.getString("SERVER_IP", "") ?: ""
        val client = OkHttpClient.Builder()
            .connectTimeout(150, TimeUnit.SECONDS)
            .readTimeout(150, TimeUnit.SECONDS)
            .writeTimeout(150, TimeUnit.SECONDS) // Added write timeout
            .retryOnConnectionFailure(true)      // Added retry
            .build()

        binding.progressBar.visibility = View.VISIBLE
        binding.progressText.visibility = View.VISIBLE
        binding.progressText.text = "Processing..."
        binding.captureButton.isEnabled = false

        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val json = JSONObject().put("image", base64Image)
                val request = Request.Builder()
                    .url("http://$serverIp:5000/process-card")
                    .post(json.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                withContext(Dispatchers.IO) {
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            throw IOException("Server error: ${response.code}")
                        }

                        val result = response.body?.string()
                        withContext(Dispatchers.Main) {
                            updateUI(result)
                            responsesDao.insertResponse(ServerResponse(
                                response = result ?: "No response",
                                imageBase64 = base64Image
                            ))
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.progressText.visibility = View.GONE
                    binding.captureButton.isEnabled = true
                    Toast.makeText(
                        this@MainActivity,
                        "Server error: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    private fun updateUI(result: String?) {
        binding.progressBar.visibility = View.GONE
        binding.progressText.visibility = View.GONE
        binding.captureButton.isEnabled = true

        try {
            val jsonObject = JSONObject(result)
            val data = jsonObject.optJSONObject("data")
            if (data != null) {
                val formattedText = buildString {
                    data.keys().forEach { key ->
                        append("$key: ${data.get(key)}\n")
                    }
                }
                binding.resultText.text = formattedText
            } else {
                binding.resultText.text = result
            }
        } catch (e: Exception) {
            binding.resultText.text = result
        }

        binding.resultCard.visibility = View.VISIBLE

        // Hide result after 2 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            binding.resultCard.visibility = View.GONE
        }, 2000) // 2000 milliseconds = 2 seconds
    }

    private fun handleError(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.progressText.visibility = View.GONE
        binding.captureButton.isEnabled = true
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun showLogs() {
        val intent = Intent(this, LogsActivity::class.java)
        startActivity(intent)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider?.unbindAll()
                camera = cameraProvider?.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Permissions not granted.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    companion object {
        private const val TAG = "CardDetector"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        } else {
            arrayOf(Manifest.permission.CAMERA)
        }
    }
}