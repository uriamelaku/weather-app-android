package com.example.weather_check.api

import android.content.Context
import com.example.weather_check.ApiConfig
import com.example.weather_check.models.*
import com.example.weather_check.utils.TokenManager
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * מחלקת עזר לביצוע בקשות API
 * מטפלת בבניית בקשות, שליחת הטוקן לשרת, ופענוח תשובות
 */
object ApiHelper {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    /**
     * שליחת בקשת POST עם JSON
     */
    fun postJson(endpoint: String, body: Any, token: String? = null): Response {
        val json = gson.toJson(body)
        val requestBody = json.toRequestBody(JSON_MEDIA_TYPE)

        val requestBuilder = Request.Builder()
            .url(ApiConfig.BASE_URL + endpoint)
            .post(requestBody)

        // הוספת הטוקן אם קיים
        if (token != null) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        return client.newCall(requestBuilder.build()).execute()
    }

    /**
     * שליחת בקשת GET
     */
    fun get(endpoint: String, token: String? = null): Response {
        val requestBuilder = Request.Builder()
            .url(ApiConfig.BASE_URL + endpoint)
            .get()

        // הוספת הטוקן אם קיים
        if (token != null) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        return client.newCall(requestBuilder.build()).execute()
    }

    /**
     * פענוח תגובת שגיאה
     */
    fun parseError(response: Response): String {
        return try {
            val errorResponse = gson.fromJson(response.body?.string(), ErrorResponse::class.java)
            errorResponse.error
        } catch (e: Exception) {
            "Error code: ${response.code}"
        }
    }

    /**
     * דוגמה: קבלת מזג אויר עם אימות
     */
    fun getWeatherWithAuth(context: Context, city: String): Response? {
        val token = TokenManager.getToken(context) ?: return null
        return get("${ApiConfig.WEATHER_ENDPOINT}?city=$city", token)
    }
}

