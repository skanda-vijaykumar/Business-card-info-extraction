package com.example.carddetector
import org.json.JSONObject
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class LogsAdapter(private val responses: List<ServerResponse>) :
    RecyclerView.Adapter<LogsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageView)
        val responseText: TextView = view.findViewById(R.id.responseText)
        val timestamp: TextView = view.findViewById(R.id.timestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.log_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val response = responses[position]
        try {
            val imageBytes = Base64.decode(response.imageBase64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            holder.imageView.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Parse JSON and format the data
        try {
            val jsonObject = JSONObject(response.response)
            val data = jsonObject.optJSONObject("data")
            if (data != null) {
                val formattedText = buildString {
                    data.keys().forEach { key ->
                        append("$key: ${data.get(key)}\n")
                    }
                }
                holder.responseText.text = formattedText
            } else {
                holder.responseText.text = response.response
            }
        } catch (e: Exception) {
            holder.responseText.text = response.response
        }

        holder.timestamp.text = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(Date(response.timestamp))
    }

    override fun getItemCount() = responses.size
}