package com.example.carddetector
import android.content.ContentValues
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.carddetector.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import java.io.ByteArrayOutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var responsesDao: ResponsesDao

    // Replace this with your actual API key
    private val API_KEY = "eKQduRkL3J0Fc2hvtJPbRjirNj26nIclgEIVNd"
    private val API_URL = "https://api.nicomind.com/api/chat"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        responsesDao = ResponsesDao(this)
        showCamera()
    }

    private fun compressImage(imageBytes: ByteArray): ByteArray {
        return try {
            // Decode the image
            val originalBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

            // High quality dimensions (max 800px for better text recognition)
            val maxDimension = 800
            val scale = minOf(
                maxDimension.toFloat() / originalBitmap.width,
                maxDimension.toFloat() / originalBitmap.height,
                1.0f // Don't upscale
            )

            val newWidth = (originalBitmap.width * scale).toInt()
            val newHeight = (originalBitmap.height * scale).toInt()

            Log.d(TAG, "Original: ${originalBitmap.width}x${originalBitmap.height}, New: ${newWidth}x${newHeight}")

            // Resize the bitmap
            val resizedBitmap = Bitmap.createScaledBitmap(
                originalBitmap, newWidth, newHeight, true
            )

            // High quality compression for better text recognition
            val outputStream = ByteArrayOutputStream()
            val quality = 90  // High quality for text clarity
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)

            val finalBytes = outputStream.toByteArray()

            // Clean up
            if (resizedBitmap != originalBitmap) {
                resizedBitmap.recycle()
            }
            originalBitmap.recycle()

            Log.d(TAG, "Final compressed size: ${finalBytes.size/1024}KB at ${quality}% quality")
            finalBytes
        } catch (e: Exception) {
            Log.e(TAG, "Error compressing image: ${e.message}")
            imageBytes
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

                    // Compress image before sending to API
                    val compressedBytes = compressImage(bytes)
                    val base64Image = Base64.encodeToString(compressedBytes, Base64.DEFAULT)

                    // Save original image to gallery
                    saveImageToGallery(bytes)

                    Log.d(TAG, "Original image size: ${bytes.size / 1024}KB")
                    Log.d(TAG, "Compressed image size: ${compressedBytes.size / 1024}KB")
                    Log.d(TAG, "Base64 size: ${base64Image.length / 1024}KB")

                    sendImageToAPI(base64Image)
                    image.close()
                }

                override fun onError(exc: ImageCaptureException) {
                    handleError("Failed to capture image: ${exc.message}")
                }
            }
        )
    }

    private fun sendImageToAPI(base64Image: String) {
        // Check if image is reasonable size for high quality processing
        val imageSizeKB = (base64Image.length * 0.75) / 1024 // Base64 is ~33% larger than binary
        Log.d(TAG, "Final image size: ${String.format("%.1f", imageSizeKB)}KB")
        Log.d(TAG, "Base64 length: ${base64Image.length} characters")

        // Basic validation - just check it's not empty and has reasonable length
        if (base64Image.isEmpty() || base64Image.length < 100) {
            handleError("Image encoding failed - image too small or empty")
            return
        }

        // More generous limit for high quality images, but add progressive sizing
        when {
            imageSizeKB > 800 -> {
                handleError("Image too large (${String.format("%.0f", imageSizeKB)}KB). Try taking photo closer to card.")
                return
            }
            imageSizeKB > 400 -> {
                Log.w(TAG, "Large image (${imageSizeKB}KB) - may cause issues")
            }
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(150, TimeUnit.SECONDS)
            .readTimeout(150, TimeUnit.SECONDS)
            .writeTimeout(150, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        binding.progressBar.visibility = View.VISIBLE
        binding.progressText.visibility = View.VISIBLE
        binding.progressText.text = "Processing..."
        binding.captureButton.isEnabled = false

        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                // Simplified prompt first to test if image is being received
                val testPrompt = "Can you see this image? If yes, describe what you see."

                // Use the original detailed prompt for better extraction
                val detailedPrompt = """
You are an expert OCR AI specializing in extracting structured data from business cards, even with complex layouts.
Your task is to accurately extract information while strictly adhering to the following JSON format.

Instructions: 

1. Start by extracting all the text on the card. 
2. Classify all the data into the respective fields given in the JSON format accurately.
3. If there is not data related to a particular field just give out an empty string "".  

JSON Format:

{
    "company_name": "string",
    "first_name": "string",
    "last_name": "string",
    "job_title": "string",
    "email_address": "string",
    "complete_address": "string",
    "street": "string",
    "state": "string",
    "country": "string",
    "postal_code": "string",
    "fax_detail": "string",
    "mobile_phone": "string",
    "phone": "string",
    "website_link": "string"
}
Note: add direct contact to phone. 
Notice that email, first name and last name exists in all the cards. Make sure to identify these.
""".trimIndent()

                // Try simple test first, then detailed extraction
                val prompt = if (imageSizeKB < 200) detailedPrompt else testPrompt

                Log.d(TAG, "Using llama3.2-vision with high quality image")
                Log.d(TAG, "Image size: ${imageSizeKB}KB")
                Log.d(TAG, "Base64 preview: ${base64Image.take(50)}...${base64Image.takeLast(10)}")
                Log.d(TAG, "Using ${if (prompt == testPrompt) "TEST" else "DETAILED"} prompt")

                // Ollama format with separate images array for llama3.2-vision
                val messages = JSONArray()
                val userMessage = JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                    put("images", JSONArray().put(base64Image)) // Clean base64, no prefix
                }
                messages.put(userMessage)

                val requestBody = JSONObject().apply {
                    put("model", "llama3.2-vision")
                    put("messages", messages)
                    put("stream", false)
                }

                Log.d(TAG, "Request body size: ${requestBody.toString().length} characters")

                val request = Request.Builder()
                    .url(API_URL)
                    .addHeader("Authorization", "Bearer $API_KEY")
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("API error: ${response.code} - ${response.message}")
                    }

                    val result = response.body?.string()
                    Log.d(TAG, "Full llama3.2-vision response: $result")

                    // Check if the model is saying it can't see the image
                    if (result?.contains("can't extract") == true || result?.contains("can't see") == true || result?.contains("unable to") == true) {
                        Log.e(TAG, "❌ Model can't see the image! Response: $result")
                    } else {
                        Log.d(TAG, "✅ Model processed the image successfully")
                    }

                    withContext(Dispatchers.Main) {
                        parseAndDisplayResult(result, base64Image)
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.progressText.visibility = View.GONE
                    binding.captureButton.isEnabled = true
                    Toast.makeText(
                        this@MainActivity,
                        "API error: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun parseAndDisplayResult(result: String?, base64Image: String) {
        binding.progressBar.visibility = View.GONE
        binding.progressText.visibility = View.GONE
        binding.captureButton.isEnabled = true

        Log.d(TAG, "llama3.2-vision Response: $result")

        if (result.isNullOrEmpty()) {
            binding.resultText.text = "No response from API"
            binding.resultCard.visibility = View.VISIBLE
            return
        }

        try {
            val jsonResponse = JSONObject(result)

            // Get the actual AI response
            val message = jsonResponse.optJSONObject("message")
            val content = message?.optString("content") ?: ""

            Log.d(TAG, "AI Response Content: $content")

            if (content.isEmpty()) {
                binding.resultText.text = "Empty AI response"
                binding.resultCard.visibility = View.VISIBLE
                return
            }

            // Check if this is a Base64 explanation (bad) or actual business card info (good)
            val isBase64Explanation = content.contains("base64") ||
                    content.contains("Base64") ||
                    content.contains("encoded data") ||
                    content.contains("binary data") ||
                    content.contains("difficult to interpret")

            if (isBase64Explanation) {
                binding.resultText.text = "❌ Vision processing failed\n\nThe AI is seeing Base64 data instead of the image. Response:\n\n${content.take(300)}..."
                binding.resultCard.visibility = View.VISIBLE
                return
            }

            // Try to parse the structured JSON response from detailed prompt
            val businessCardData = try {
                // Look for JSON in the response
                val jsonStart = content.indexOf("{")
                val jsonEnd = content.lastIndexOf("}") + 1
                if (jsonStart != -1 && jsonEnd > jsonStart) {
                    val jsonString = content.substring(jsonStart, jsonEnd)
                    Log.d(TAG, "Extracted JSON: ${jsonString.take(200)}")
                    JSONObject(jsonString)
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing business card JSON: ${e.message}")
                null
            }

            val displayText = if (businessCardData != null) {
                // Format the structured business card data
                val formattedText = buildString {
                    append("✅ Business Card Extracted:\n\n")

                    val fields = mapOf(
                        "first_name" to "First Name",
                        "last_name" to "Last Name",
                        "company_name" to "Company",
                        "job_title" to "Job Title",
                        "email_address" to "Email",
                        "phone" to "Phone",
                        "mobile_phone" to "Mobile",
                        "website_link" to "Website",
                        "complete_address" to "Address",
                        "street" to "Street",
                        "state" to "State",
                        "country" to "Country",
                        "postal_code" to "Postal Code",
                        "fax_detail" to "Fax"
                    )

                    fields.forEach { (key, label) ->
                        val value = businessCardData.optString(key, "")
                        if (value.isNotEmpty()) {
                            append("$label: $value\n")
                        }
                    }
                }

                // Save structured data
                responsesDao.insertResponse(ServerResponse(
                    response = businessCardData.toString(),
                    imageBase64 = base64Image
                ))

                formattedText
            } else {
                // Fallback to raw content if JSON parsing failed
                val displayContent = "✅ Business Card Text:\n\n$content"

                responsesDao.insertResponse(ServerResponse(
                    response = content,
                    imageBase64 = base64Image
                ))

                displayContent
            }

            binding.resultText.text = displayText

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing API response: ${e.message}")
            binding.resultText.text = "Parse Error: ${e.message}\n\nRaw response:\n${result.take(500)}"
        }

        binding.resultCard.visibility = View.VISIBLE

        // Hide result after 15 seconds (more time to read)
        Handler(Looper.getMainLooper()).postDelayed({
            binding.resultCard.visibility = View.GONE
        }, 15000)
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
                .setJpegQuality(85) // Set JPEG quality
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
