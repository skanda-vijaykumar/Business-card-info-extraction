package com.example.carddetector

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.carddetector.databinding.ActivityLogsBinding
import java.text.SimpleDateFormat
import java.util.*

class LogsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLogsBinding
    private lateinit var responsesDao: ResponsesDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        responsesDao = ResponsesDao(this)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = LogsAdapter(responsesDao.getAllResponses())
    }
}