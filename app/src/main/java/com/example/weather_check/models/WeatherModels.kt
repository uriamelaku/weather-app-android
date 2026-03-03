package com.example.weather_check.models

/**
 * Weather API Response Model
 * Matches the server's weather endpoint response
 */
data class WeatherResponse(
    val city: String,           // City name from OpenWeather
    val country: String,        // ISO 3166-1 alpha-2 country code
    val temp: Double,           // Temperature in Celsius
    val feelsLike: Double,      // "Feels like" temperature in Celsius
    val humidity: Int,          // Humidity percentage (0-100)
    val windSpeed: Double,      // Wind speed in m/s
    val description: String,    // Weather description (e.g., "clear sky")
    val icon: String,           // OpenWeather icon code (e.g., "01d")
    val timestamp: Long         // Unix timestamp from OpenWeather
)

