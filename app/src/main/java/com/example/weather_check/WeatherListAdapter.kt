package com.example.weather_check

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.weather_check.models.WeatherResponse

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
        private val deleteButton: ImageButton = itemView.findViewById(R.id.btnDeleteItem)

        fun bind(item: WeatherResponse, onDeleteClick: (WeatherResponse) -> Unit) {
            title.text = "${item.city}, ${item.country}"
            // Show weather data only if available (temp > 0)
            if (item.temp > 0.0 || item.description.isNotEmpty()) {
                subtitle.text = "${item.temp.toInt()}° - ${item.description}"
                subtitle.visibility = View.VISIBLE
            } else {
                // Hide subtitle if no weather data
                subtitle.visibility = View.GONE
            }
            deleteButton.setOnClickListener { onDeleteClick(item) }
        }
    }
}

