package com.example.weather_check.models

data class HistoryItem(
    val city: String,
    val country: String,
    val temp: Double,
    val feelsLike: Double,
    val humidity: Int,
    val windSpeed: Double,
    val description: String,
    val icon: String,
    val timestamp: Long,
    val searchedAt: String?
)

data class FavoriteItem(
    val city: String,
    val country: String,
    val addedAt: String?
)

data class HistoryResponse(
    val history: List<HistoryItem>
)

data class FavoritesResponse(
    val favorites: List<FavoriteItem>
)

data class AddFavoriteRequest(
    val city: String,
    val country: String
)

data class MessageResponse(
    val message: String
)

data class ApiError(
    val error: String
)
