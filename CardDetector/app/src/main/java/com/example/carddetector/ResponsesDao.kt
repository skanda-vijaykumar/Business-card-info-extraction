package com.example.carddetector
import android.util.Base64
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ResponsesDao(context: Context) {
    private val sharedPreferences = context.applicationContext.getSharedPreferences("responses", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val maxResponses = 10

    fun insertResponse(response: ServerResponse) {
        try {
            // Read existing responses
            val responses = getAllResponses().toMutableList()

            // Compress image before storing
            val compressedImage = compressImage(response.imageBase64)
            responses.add(0, response.copy(imageBase64 = compressedImage))

            // Keep only last N responses
            val trimmedResponses = responses.take(maxResponses)

            sharedPreferences.edit()
                .putString("responses", gson.toJson(trimmedResponses))
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun compressImage(base64Image: String): String {
        try {
            val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

            // Scale down the image
            val maxDimension = 800
            val scaleFactor = minOf(
                maxDimension.toFloat() / bitmap.width,
                maxDimension.toFloat() / bitmap.height
            )

            val scaledBitmap = if (scaleFactor < 1) {
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * scaleFactor).toInt(),
                    (bitmap.height * scaleFactor).toInt(),
                    true
                )
            } else {
                bitmap
            }

            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)

            return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            return base64Image
        }
    }

    fun getAllResponses(): List<ServerResponse> {
        return try {
            val json = sharedPreferences.getString("responses", "[]")
            val type = object : TypeToken<List<ServerResponse>>() {}.type
            gson.fromJson(json, type) ?: listOf()
        } catch (e: Exception) {
            e.printStackTrace()
            listOf()
        }
    }
}