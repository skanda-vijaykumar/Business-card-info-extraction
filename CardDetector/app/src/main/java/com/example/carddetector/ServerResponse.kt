package com.example.carddetector

data class ServerResponse(
    val timestamp: Long = System.currentTimeMillis(),
    val response: String,
    val imageBase64: String
)