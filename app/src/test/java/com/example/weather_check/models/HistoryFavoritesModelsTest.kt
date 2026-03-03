package com.example.weather_check.models

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryFavoritesModelsTest {
    private val gson = Gson()

    @Test
    fun historyResponse_deserializesHistoryWrapper() {
        val json = """
            {
              "history": [
                {
                  "city": "Tel Aviv",
                  "country": "IL",
                  "temp": 22.5,
                  "feelsLike": 21.8,
                  "humidity": 65,
                  "windSpeed": 3.5,
                  "description": "clear sky",
                  "icon": "01d",
                  "timestamp": 1709380800,
                  "searchedAt": "2026-03-03T09:30:00.000Z"
                }
              ]
            }
        """.trimIndent()

        val response = gson.fromJson(json, HistoryResponse::class.java)

        assertEquals(1, response.history.size)
        assertEquals("Tel Aviv", response.history.first().city)
    }

    @Test
    fun favoritesResponse_deserializesFavoritesWrapper() {
        val json = """
            {
              "favorites": [
                {
                  "city": "London",
                  "country": "GB",
                  "addedAt": "2026-03-03T09:20:00.000Z"
                }
              ]
            }
        """.trimIndent()

        val response = gson.fromJson(json, FavoritesResponse::class.java)

        assertEquals(1, response.favorites.size)
        assertEquals("London", response.favorites.first().city)
    }

    @Test
    fun apiError_deserializesErrorField() {
        val json = "{" + "\"error\":\"Unauthorized\"}"
        val error = gson.fromJson(json, ApiError::class.java)

        assertEquals("Unauthorized", error.error)
    }
}

