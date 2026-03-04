package com.example.weather_check.repository

import android.util.Log
import com.example.weather_check.api.ApiHelper
import com.example.weather_check.models.FavoritesResponse
import com.example.weather_check.models.HistoryResponse
import com.example.weather_check.models.MessageResponse
import com.google.gson.Gson

/**
 * Network Repository - Handles all API communication with server
 * Provides async methods with success/error callbacks
 */
object NetworkRepository {
    private val gson = Gson()
    private const val TAG = "NetworkRepository"

    /**
     * Get history with authentication
     * @param token JWT token from login
     * @param onSuccess Called with HistoryResponse on success
     * @param onError Called with error message on failure
     */
    fun getHistory(token: String, onSuccess: (HistoryResponse) -> Unit, onError: (String, Int?) -> Unit) {
        Thread {
            try {
                val response = ApiHelper.getHistoryWithAuth(token)
                val responseBody = response.body?.string()

                when {
                    response.isSuccessful && !responseBody.isNullOrEmpty() -> {
                        try {
                            val historyResponse = gson.fromJson(responseBody, HistoryResponse::class.java)
                            onSuccess(historyResponse)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing history response", e)
                            onError("Failed to parse history data", response.code)
                        }
                    }
                    response.code == 401 -> {
                        Log.w(TAG, "Unauthorized: Token invalid")
                        onError("Unauthorized - please login again", 401)
                    }
                    response.code == 404 -> {
                        Log.w(TAG, "User not found")
                        onError("User not found", 404)
                    }
                    else -> {
                        val error = ApiHelper.parseError(responseBody)
                        Log.e(TAG, "Error response: $error (${response.code})")
                        onError(error, response.code)
                    }
                }
                response.close()
            } catch (e: Exception) {
                Log.e(TAG, "Network error in getHistory", e)
                onError("Network error: ${e.message}", null)
            }
        }.start()
    }

    /**
     * Clear all history with authentication
     * @param token JWT token from login
     * @param onSuccess Called with MessageResponse on success
     * @param onError Called with error message on failure
     */
    fun clearHistory(token: String, onSuccess: (MessageResponse) -> Unit, onError: (String, Int?) -> Unit) {
        Thread {
            try {
                val response = ApiHelper.clearHistoryWithAuth(token)
                val responseBody = response.body?.string()

                when {
                    response.isSuccessful && !responseBody.isNullOrEmpty() -> {
                        try {
                            val messageResponse = gson.fromJson(responseBody, MessageResponse::class.java)
                            onSuccess(messageResponse)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing clear history response", e)
                            onError("Failed to clear history", response.code)
                        }
                    }
                    response.code == 401 -> {
                        Log.w(TAG, "Unauthorized: Token invalid")
                        onError("Unauthorized - please login again", 401)
                    }
                    else -> {
                        val error = ApiHelper.parseError(responseBody)
                        Log.e(TAG, "Error response: $error (${response.code})")
                        onError(error, response.code)
                    }
                }
                response.close()
            } catch (e: Exception) {
                Log.e(TAG, "Network error in clearHistory", e)
                onError("Network error: ${e.message}", null)
            }
        }.start()
    }

    /**
     * Remove single history item by city with authentication
     * @param token JWT token from login
     * @param city City name (will be URL encoded)
     * @param onSuccess Called with HistoryResponse on success
     * @param onError Called with error message on failure
     */
    fun removeHistoryItem(
        token: String,
        city: String,
        onSuccess: (HistoryResponse) -> Unit,
        onError: (String, Int?) -> Unit
    ) {
        Thread {
            try {
                val response = ApiHelper.removeHistoryItemWithAuth(token, city)
                val responseBody = response.body?.string()

                when {
                    response.isSuccessful -> {
                        // Handle both empty and non-empty success responses
                        if (responseBody.isNullOrEmpty()) {
                            // If response is empty but successful, return empty history list
                            val emptyResponse = HistoryResponse(emptyList())
                            onSuccess(emptyResponse)
                        } else {
                            try {
                                val historyResponse = gson.fromJson(responseBody, HistoryResponse::class.java)
                                onSuccess(historyResponse)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing remove history response", e)
                                onError("Failed to remove history item", response.code)
                            }
                        }
                    }
                    response.code == 401 -> {
                        Log.w(TAG, "Unauthorized: Token invalid")
                        onError("Unauthorized - please login again", 401)
                    }
                    response.code == 404 -> {
                        Log.w(TAG, "History item not found")
                        onError("History item not found", 404)
                    }
                    else -> {
                        val error = ApiHelper.parseError(responseBody)
                        Log.e(TAG, "Error response: $error (${response.code})")
                        onError(error, response.code)
                    }
                }
                response.close()
            } catch (e: Exception) {
                Log.e(TAG, "Network error in removeHistoryItem", e)
                onError("Network error: ${e.message}", null)
            }
        }.start()
    }

    /**
     * Get favorites with authentication
     * @param token JWT token from login
     * @param onSuccess Called with FavoritesResponse on success
     * @param onError Called with error message on failure
     */
    fun getFavorites(token: String, onSuccess: (FavoritesResponse) -> Unit, onError: (String, Int?) -> Unit) {
        Thread {
            try {
                val response = ApiHelper.getFavoritesWithAuth(token)
                val responseBody = response.body?.string()

                when {
                    response.isSuccessful && !responseBody.isNullOrEmpty() -> {
                        try {
                            val favoritesResponse = gson.fromJson(responseBody, FavoritesResponse::class.java)
                            onSuccess(favoritesResponse)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing favorites response", e)
                            onError("Failed to parse favorites data", response.code)
                        }
                    }
                    response.code == 401 -> {
                        Log.w(TAG, "Unauthorized: Token invalid")
                        onError("Unauthorized - please login again", 401)
                    }
                    response.code == 404 -> {
                        Log.w(TAG, "User not found")
                        onError("User not found", 404)
                    }
                    else -> {
                        val error = ApiHelper.parseError(responseBody)
                        Log.e(TAG, "Error response: $error (${response.code})")
                        onError(error, response.code)
                    }
                }
                response.close()
            } catch (e: Exception) {
                Log.e(TAG, "Network error in getFavorites", e)
                onError("Network error: ${e.message}", null)
            }
        }.start()
    }

    /**
     * Add city to favorites with authentication
     * @param token JWT token from login
     * @param city City name
     * @param country Country code
     * @param onSuccess Called with FavoritesResponse on success
     * @param onError Called with error message on failure
     */
    fun addFavorite(
        token: String,
        city: String,
        country: String,
        onSuccess: (FavoritesResponse) -> Unit,
        onError: (String, Int?) -> Unit
    ) {
        Thread {
            try {
                val response = ApiHelper.addFavoriteWithAuth(token, city, country)
                val responseBody = response.body?.string()

                when {
                    (response.isSuccessful || response.code == 201) && !responseBody.isNullOrEmpty() -> {
                        try {
                            val favoritesResponse = gson.fromJson(responseBody, FavoritesResponse::class.java)
                            onSuccess(favoritesResponse)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing add favorite response", e)
                            onError("Failed to add favorite", response.code)
                        }
                    }
                    response.code == 400 -> {
                        val error = ApiHelper.parseError(responseBody)
                        Log.w(TAG, "Bad request: $error")
                        onError(error, 400)
                    }
                    response.code == 401 -> {
                        Log.w(TAG, "Unauthorized: Token invalid")
                        onError("Unauthorized - please login again", 401)
                    }
                    response.code == 404 -> {
                        Log.w(TAG, "User not found")
                        onError("User not found", 404)
                    }
                    response.code == 409 -> {
                        Log.w(TAG, "City already in favorites")
                        onError("City already in favorites", 409)
                    }
                    else -> {
                        val error = ApiHelper.parseError(responseBody)
                        Log.e(TAG, "Error response: $error (${response.code})")
                        onError(error, response.code)
                    }
                }
                response.close()
            } catch (e: Exception) {
                Log.e(TAG, "Network error in addFavorite", e)
                onError("Network error: ${e.message}", null)
            }
        }.start()
    }

    /**
     * Remove city from favorites with authentication
     * @param token JWT token from login
     * @param city City name (will be URL encoded)
     * @param onSuccess Called with FavoritesResponse on success
     * @param onError Called with error message on failure
     */
    fun removeFavorite(
        token: String,
        city: String,
        onSuccess: (FavoritesResponse) -> Unit,
        onError: (String, Int?) -> Unit
    ) {
        Thread {
            try {
                val response = ApiHelper.removeFavoriteWithAuth(token, city)
                val responseBody = response.body?.string()

                when {
                    response.isSuccessful -> {
                        // Handle both empty and non-empty success responses
                        if (responseBody.isNullOrEmpty()) {
                            // If response is empty but successful, return empty favorites list
                            val emptyResponse = FavoritesResponse(emptyList())
                            onSuccess(emptyResponse)
                        } else {
                            try {
                                val favoritesResponse = gson.fromJson(responseBody, FavoritesResponse::class.java)
                                onSuccess(favoritesResponse)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing remove favorite response", e)
                                onError("Failed to remove favorite", response.code)
                            }
                        }
                    }
                    response.code == 401 -> {
                        Log.w(TAG, "Unauthorized: Token invalid")
                        onError("Unauthorized - please login again", 401)
                    }
                    response.code == 404 -> {
                        Log.w(TAG, "Favorite not found")
                        onError("Favorite not found", 404)
                    }
                    else -> {
                        val error = ApiHelper.parseError(responseBody)
                        Log.e(TAG, "Error response: $error (${response.code})")
                        onError(error, response.code)
                    }
                }
                response.close()
            } catch (e: Exception) {
                Log.e(TAG, "Network error in removeFavorite", e)
                onError("Network error: ${e.message}", null)
            }
        }.start()
    }

    /**
     * Clear all favorites with authentication
     * @param token JWT token from login
     * @param onSuccess Called with FavoritesResponse on success
     * @param onError Called with error message on failure
     */
    fun clearFavorites(
        token: String,
        onSuccess: (FavoritesResponse) -> Unit,
        onError: (String, Int?) -> Unit
    ) {
        Thread {
            try {
                val response = ApiHelper.clearFavoritesWithAuth(token)
                val responseBody = response.body?.string()

                when {
                    response.isSuccessful -> {
                        // Handle both empty and non-empty success responses
                        if (responseBody.isNullOrEmpty()) {
                            // If response is empty but successful, return empty favorites list
                            val emptyResponse = FavoritesResponse(emptyList())
                            onSuccess(emptyResponse)
                        } else {
                            try {
                                val favoritesResponse = gson.fromJson(responseBody, FavoritesResponse::class.java)
                                onSuccess(favoritesResponse)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing clear favorites response", e)
                                onError("Failed to clear favorites", response.code)
                            }
                        }
                    }
                    response.code == 401 -> {
                        Log.w(TAG, "Unauthorized: Token invalid")
                        onError("Unauthorized - please login again", 401)
                    }
                    else -> {
                        val error = ApiHelper.parseError(responseBody)
                        Log.e(TAG, "Error response: $error (${response.code})")
                        onError(error, response.code)
                    }
                }
                response.close()
            } catch (e: Exception) {
                Log.e(TAG, "Network error in clearFavorites", e)
                onError("Network error: ${e.message}", null)
            }
        }.start()
    }
}

