package com.example.weather_check.api

import com.example.weather_check.ApiConfig
import com.example.weather_check.models.AddFavoriteRequest
import com.example.weather_check.models.ApiError
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

/**
 * מחלקת עזר לביצוע בקשות API
 * מטפלת בבניית בקשות, שליחת הטוקן לשרת, ופענוח תשובות
 */
object ApiHelper {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)  // Extended for cold start on Render
        .readTimeout(60, TimeUnit.SECONDS)      // Extended for cold start on Render
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    internal fun buildGetRequest(endpoint: String, token: String): Request {
        return Request.Builder()
            .url(ApiConfig.BASE_URL + endpoint)
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()
    }

    internal fun buildDeleteRequest(endpoint: String, token: String): Request {
        return Request.Builder()
            .url(ApiConfig.BASE_URL + endpoint)
            .addHeader("Authorization", "Bearer $token")
            .delete()
            .build()
    }

    internal fun buildPostJsonRequest(endpoint: String, body: Any, token: String): Request {
        val json = gson.toJson(body)
        return Request.Builder()
            .url(ApiConfig.BASE_URL + endpoint)
            .addHeader("Authorization", "Bearer $token")
            .post(json.toRequestBody(jsonMediaType))
            .build()
    }

    /**
     * פענוח תגובת שגיאה
     */
    fun parseError(responseBody: String?): String {
        if (responseBody.isNullOrBlank()) return "Unknown error"
        return try {
            gson.fromJson(responseBody, ApiError::class.java).error
        } catch (e: Exception) {
            "Unknown error"
        }
    }

    /**
     * דוגמה: קבלת היסטוריית חיפושים עם אימות
     */
    fun getHistoryWithAuth(token: String): Response {
        return client.newCall(buildGetRequest(ApiConfig.HISTORY_ENDPOINT, token)).execute()
    }

    /**
     * ניקוי היסטוריית חיפושים עם אימות
     */
    fun clearHistoryWithAuth(token: String): Response {
        return client.newCall(buildDeleteRequest(ApiConfig.HISTORY_ENDPOINT, token)).execute()
    }

    /**
     * הסרת עיר בודדת מההיסטוריה עם אימות
     */
    fun removeHistoryItemWithAuth(token: String, city: String): Response {
        val encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8.name())
        val endpoint = ApiConfig.HISTORY_BY_CITY_ENDPOINT_TEMPLATE.format(encodedCity)
        return client.newCall(buildDeleteRequest(endpoint, token)).execute()
    }

    /**
     * דוגמה: קבלת מועדפים עם אימות
     */
    fun getFavoritesWithAuth(token: String): Response {
        return client.newCall(buildGetRequest(ApiConfig.FAVORITES_ENDPOINT, token)).execute()
    }

    /**
     * הוספת עיר למועדפים עם אימות
     */
    fun addFavoriteWithAuth(token: String, city: String, country: String): Response {
        val request = buildPostJsonRequest(ApiConfig.FAVORITES_ENDPOINT, AddFavoriteRequest(city, country), token)
        return client.newCall(request).execute()
    }

    /**
     * הסרת עיר מהמועדפים עם אימות
     */
    fun removeFavoriteWithAuth(token: String, city: String): Response {
        val encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8.name())
        val endpoint = ApiConfig.FAVORITE_BY_CITY_ENDPOINT_TEMPLATE.format(encodedCity)
        return client.newCall(buildDeleteRequest(endpoint, token)).execute()
    }

    /**
     * ניקוי כל המועדפים עם אימות
     */
    fun clearFavoritesWithAuth(token: String): Response {
        return client.newCall(buildDeleteRequest(ApiConfig.FAVORITES_ENDPOINT, token)).execute()
    }
}
