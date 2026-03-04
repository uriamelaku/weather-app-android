package com.example.weather_check.utils

import com.example.weather_check.models.FavoriteItem
import com.example.weather_check.models.HistoryItem
import com.example.weather_check.models.WeatherResponse

/**
 * Data mapper utilities for converting between API response models and UI models
 */

/**
 * Convert HistoryItem to WeatherResponse for display in RecyclerView
 */
fun HistoryItem.toWeatherResponse(): WeatherResponse {
    return WeatherResponse(
        city = this.city,
        country = this.country,
        temp = this.temp,
        feelsLike = this.feelsLike,
        humidity = this.humidity,
        windSpeed = this.windSpeed,
        description = this.description,
        icon = this.icon,
        timestamp = System.currentTimeMillis()
    )
}

/**
 * Convert FavoriteItem to WeatherResponse for display in RecyclerView
 * Note: FavoriteItem lacks temperature/weather details, so we use defaults
 * In a real app, you might want to fetch fresh weather data for each favorite
 */
fun FavoriteItem.toWeatherResponse(): WeatherResponse {
    return WeatherResponse(
        city = this.city,
        country = this.country,
        temp = 0.0,          // Not available in FavoriteItem
        feelsLike = 0.0,     // Not available in FavoriteItem
        humidity = 0,        // Not available in FavoriteItem
        windSpeed = 0.0,     // Not available in FavoriteItem
        description = "",    // Not available in FavoriteItem
        icon = "",           // Not available in FavoriteItem
        timestamp = System.currentTimeMillis()
    )
}
