package com.example.weather_check

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.weather_check.models.WeatherResponse
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class WeatherListAdapter(
    private val onDeleteClick: (WeatherResponse) -> Unit
) : RecyclerView.Adapter<WeatherListAdapter.WeatherItemViewHolder>() {

    private val items = mutableListOf<WeatherResponse>()

    fun submitItems(newItems: List<WeatherResponse>) {
        Log.d("WeatherListAdapter", "submitItems called with ${newItems.size} items")
        newItems.forEach {
            Log.d("WeatherListAdapter", "Item: ${it.city}")
        }
        items.clear()
        items.addAll(newItems)
        Log.d("WeatherListAdapter", "Adapter now has ${items.size} items")
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeatherItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_weather_list, parent, false)
        return WeatherItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: WeatherItemViewHolder, position: Int) {
        holder.bind(items[position], onDeleteClick)
    }

    override fun getItemCount(): Int = items.size

    class WeatherItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.tvItemTitle)
        private val subtitle: TextView = itemView.findViewById(R.id.tvItemSubtitle)
        private val dateTime: TextView = itemView.findViewById(R.id.tvItemDateTime)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.btnDeleteItem)

        fun bind(item: WeatherResponse, onDeleteClick: (WeatherResponse) -> Unit) {
            title.text = "${item.city}, ${item.country}"

            if (item.temp > 0.0 || item.description.isNotEmpty()) {
                val emoji = getWeatherEmoji(item.description)
                subtitle.text = "$emoji ${item.temp.toInt()}° - ${item.description}"
                subtitle.visibility = View.VISIBLE
            } else {
                subtitle.visibility = View.GONE
            }

            val timeLabel = formatHistoryDateTime(item)
            if (timeLabel != null) {
                dateTime.text = timeLabel
                dateTime.visibility = View.VISIBLE
            } else {
                dateTime.visibility = View.GONE
            }

            deleteButton.setOnClickListener { onDeleteClick(item) }
        }

        private fun formatHistoryDateTime(item: WeatherResponse): String? {
            // Only history items carry weather details + searchedAt from server.
            if (item.temp <= 0.0 && item.description.isEmpty()) return null

            val israelTz = TimeZone.getTimeZone("Asia/Jerusalem")

            // Prefer server search-time string when available.
            val isoRaw = item.searchedAt
            if (!isoRaw.isNullOrBlank()) {
                val normalized = isoRaw.replace("Z", "+0000")
                val parsePatterns = listOf(
                    "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
                    "yyyy-MM-dd'T'HH:mm:ssZ"
                )

                for (pattern in parsePatterns) {
                    try {
                        val parser = SimpleDateFormat(pattern, Locale.US)
                        val parsedDate = parser.parse(normalized) ?: continue
                        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.forLanguageTag("he-IL"))
                        formatter.timeZone = israelTz
                        return formatter.format(parsedDate)
                    } catch (_: Exception) {
                        // Try next supported pattern
                    }
                }
            }

            // Fallback to numeric timestamp (seconds or millis).
            if (item.timestamp <= 0L) return null
            val millis = if (item.timestamp < 1_000_000_000_000L) item.timestamp * 1000 else item.timestamp
            val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.forLanguageTag("he-IL"))
            formatter.timeZone = israelTz
            return formatter.format(Date(millis))
        }

        private fun getWeatherEmoji(description: String): String {
            val desc = description.lowercase()
            return when {
                desc.contains("clear") || desc.contains("sunny") -> "☀️"
                desc.contains("cloud") && desc.contains("few") -> "🌤️"
                desc.contains("cloud") && desc.contains("scattered") -> "⛅"
                desc.contains("cloud") && desc.contains("broken") -> "☁️"
                desc.contains("cloud") || desc.contains("overcast") -> "☁️"
                desc.contains("rain") && desc.contains("light") -> "🌦️"
                desc.contains("rain") || desc.contains("drizzle") -> "🌧️"
                desc.contains("thunderstorm") || desc.contains("storm") -> "⛈️"
                desc.contains("snow") -> "❄️"
                desc.contains("mist") || desc.contains("fog") || desc.contains("haze") -> "🌫️"
                desc.contains("wind") -> "💨"
                desc.contains("tornado") -> "🌪️"
                else -> "🌡️"
            }
        }
    }
}
