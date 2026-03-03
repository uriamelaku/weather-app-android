package com.example.weather_check

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.weather_check.api.ApiHelper
import com.example.weather_check.models.FavoritesResponse
import com.example.weather_check.models.HistoryResponse
import com.example.weather_check.models.MessageResponse
import com.example.weather_check.models.WeatherResponse
import com.example.weather_check.utils.TokenManager
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class HomeActivity : AppCompatActivity() {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    private lateinit var cityInput: TextInputEditText
    private lateinit var emptyStateCard: CardView
    private lateinit var weatherCard: CardView
    private var currentWeather: WeatherResponse? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        cityInput = findViewById(R.id.etCityInput)
        emptyStateCard = findViewById(R.id.cardEmptyState)
        weatherCard = findViewById(R.id.cardWeather)

        findViewById<Button>(R.id.btnSearch).setOnClickListener {
            val city = cityInput.text.toString().trim()
            if (city.isEmpty()) {
                Toast.makeText(this, getString(R.string.empty_city_error), Toast.LENGTH_SHORT).show()
            } else {
                fetchWeather(city)
            }
        }

        findViewById<Button>(R.id.btnHistory).setOnClickListener { fetchHistory() }
        findViewById<Button>(R.id.btnHistory).setOnLongClickListener {
            clearHistory()
            true
        }

        findViewById<Button>(R.id.btnFavorites).setOnClickListener { fetchFavorites() }
        findViewById<Button>(R.id.btnFavorites).setOnLongClickListener {
            toggleCurrentCityFavorite()
            true
        }
    }

    private fun fetchWeather(city: String) {
        val token = TokenManager.getToken(this)
        if (token == null) {
            Toast.makeText(this, getString(R.string.token_missing_error), Toast.LENGTH_LONG).show()
            return
        }

        val encodedCity = URLEncoder.encode(city, "UTF-8")
        val url = "${ApiConfig.BASE_URL}${ApiConfig.WEATHER_ENDPOINT}?city=$encodedCity"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(
                        this@HomeActivity,
                        "${getString(R.string.network_error)}: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                val code = response.code

                runOnUiThread {
                    when {
                        response.isSuccessful && !responseBody.isNullOrEmpty() -> {
                            try {
                                val weatherResponse = gson.fromJson(responseBody, WeatherResponse::class.java)
                                displayWeather(weatherResponse)
                                Toast.makeText(
                                    this@HomeActivity,
                                    getString(R.string.weather_loaded_success),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } catch (e: Exception) {
                                Toast.makeText(
                                    this@HomeActivity,
                                    "${getString(R.string.parse_error)}: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                        code == 401 -> handleUnauthorized()
                        code == 404 -> Toast.makeText(
                            this@HomeActivity,
                            getString(R.string.city_not_found_error),
                            Toast.LENGTH_LONG
                        ).show()
                        code == 400 || code == 500 -> {
                            val error = ApiHelper.parseError(responseBody)
                            Toast.makeText(this@HomeActivity, error, Toast.LENGTH_LONG).show()
                        }
                        else -> {
                            val error = ApiHelper.parseError(responseBody)
                            Toast.makeText(this@HomeActivity, "${getString(R.string.weather_error)}: $error", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                response.close()
            }
        })
    }

    private fun fetchHistory() {
        val token = TokenManager.getToken(this)
        if (token == null) {
            Toast.makeText(this, getString(R.string.token_missing_error), Toast.LENGTH_LONG).show()
            return
        }

        Thread {
            val response = ApiHelper.getHistoryWithAuth(token)
            val body = response.body?.string()
            val code = response.code
            val success = response.isSuccessful
            response.close()

            runOnUiThread {
                when {
                    success && !body.isNullOrEmpty() -> {
                        try {
                            val history = gson.fromJson(body, HistoryResponse::class.java).history
                            if (history.isEmpty()) {
                                Toast.makeText(this, "אין היסטוריה להצגה", Toast.LENGTH_SHORT).show()
                            } else {
                                val lines = history.mapIndexed { index, item ->
                                    "${index + 1}. ${item.city}, ${item.country} ${item.temp.toInt()}°"
                                }
                                AlertDialog.Builder(this)
                                    .setTitle("היסטוריית חיפושים")
                                    .setMessage(lines.joinToString("\n"))
                                    .setPositiveButton("OK", null)
                                    .setNeutralButton("נקה") { _, _ -> clearHistory() }
                                    .show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this, getString(R.string.parse_error), Toast.LENGTH_LONG).show()
                        }
                    }
                    code == 401 -> handleUnauthorized()
                    code == 400 || code == 404 || code == 500 -> {
                        Toast.makeText(this, ApiHelper.parseError(body), Toast.LENGTH_LONG).show()
                    }
                    else -> Toast.makeText(this, "History error: $code", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun clearHistory() {
        val token = TokenManager.getToken(this)
        if (token == null) {
            Toast.makeText(this, getString(R.string.token_missing_error), Toast.LENGTH_LONG).show()
            return
        }

        Thread {
            val response = ApiHelper.clearHistoryWithAuth(token)
            val body = response.body?.string()
            val code = response.code
            val success = response.isSuccessful
            response.close()

            runOnUiThread {
                when {
                    success && !body.isNullOrEmpty() -> {
                        try {
                            val message = gson.fromJson(body, MessageResponse::class.java).message
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show()
                        }
                    }
                    code == 401 -> handleUnauthorized()
                    code == 404 || code == 500 -> Toast.makeText(this, ApiHelper.parseError(body), Toast.LENGTH_LONG).show()
                    else -> Toast.makeText(this, "History clear error: $code", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun fetchFavorites() {
        val token = TokenManager.getToken(this)
        if (token == null) {
            Toast.makeText(this, getString(R.string.token_missing_error), Toast.LENGTH_LONG).show()
            return
        }

        Thread {
            val response = ApiHelper.getFavoritesWithAuth(token)
            val body = response.body?.string()
            val code = response.code
            val success = response.isSuccessful
            response.close()

            runOnUiThread {
                when {
                    success && !body.isNullOrEmpty() -> {
                        try {
                            val favorites = gson.fromJson(body, FavoritesResponse::class.java).favorites
                            if (favorites.isEmpty()) {
                                Toast.makeText(this, "אין מועדפים להצגה", Toast.LENGTH_SHORT).show()
                            } else {
                                val lines = favorites.mapIndexed { index, item ->
                                    "${index + 1}. ${item.city}, ${item.country}"
                                }
                                AlertDialog.Builder(this)
                                    .setTitle("ערים מועדפות")
                                    .setMessage(lines.joinToString("\n"))
                                    .setPositiveButton("OK", null)
                                    .show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this, getString(R.string.parse_error), Toast.LENGTH_LONG).show()
                        }
                    }
                    code == 401 -> handleUnauthorized()
                    code == 404 || code == 500 -> Toast.makeText(this, ApiHelper.parseError(body), Toast.LENGTH_LONG).show()
                    else -> Toast.makeText(this, "Favorites error: $code", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun toggleCurrentCityFavorite() {
        val weather = currentWeather
        if (weather == null) {
            Toast.makeText(this, "חפש קודם עיר", Toast.LENGTH_SHORT).show()
            return
        }

        val token = TokenManager.getToken(this)
        if (token == null) {
            Toast.makeText(this, getString(R.string.token_missing_error), Toast.LENGTH_LONG).show()
            return
        }

        Thread {
            val addResponse = ApiHelper.addFavoriteWithAuth(token, weather.city, weather.country)
            val addBody = addResponse.body?.string()
            val addCode = addResponse.code
            val addSuccess = addResponse.isSuccessful
            addResponse.close()

            if (addSuccess && addCode == 201) {
                runOnUiThread { Toast.makeText(this, "Added to favorites", Toast.LENGTH_SHORT).show() }
                return@Thread
            }

            if (addCode == 409) {
                val removeResponse = ApiHelper.removeFavoriteWithAuth(token, weather.city)
                val removeBody = removeResponse.body?.string()
                val removeCode = removeResponse.code
                val removeSuccess = removeResponse.isSuccessful
                removeResponse.close()

                runOnUiThread {
                    when {
                        removeSuccess -> Toast.makeText(this, "Removed from favorites", Toast.LENGTH_SHORT).show()
                        removeCode == 401 -> handleUnauthorized()
                        removeCode == 400 || removeCode == 404 || removeCode == 500 -> {
                            Toast.makeText(this, ApiHelper.parseError(removeBody), Toast.LENGTH_LONG).show()
                        }
                        else -> Toast.makeText(this, "Favorites update error: $removeCode", Toast.LENGTH_LONG).show()
                    }
                }
                return@Thread
            }

            runOnUiThread {
                when {
                    addCode == 401 -> handleUnauthorized()
                    addCode == 400 || addCode == 404 || addCode == 500 -> {
                        Toast.makeText(this, ApiHelper.parseError(addBody), Toast.LENGTH_LONG).show()
                    }
                    else -> Toast.makeText(this, "Favorites update error: $addCode", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun handleUnauthorized() {
        TokenManager.clearToken(this)
        Toast.makeText(this, getString(R.string.token_invalid_error), Toast.LENGTH_LONG).show()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun displayWeather(weather: WeatherResponse) {
        emptyStateCard.visibility = View.GONE
        weatherCard.visibility = View.VISIBLE

        currentWeather = weather
        findViewById<TextView>(R.id.tvCityName).text = "${weather.city}, ${weather.country}"
        findViewById<TextView>(R.id.tvTemperature).text = "${weather.temp.toInt()}°"
        findViewById<TextView>(R.id.tvFeelsLike).text = "${getString(R.string.feels_like)}: ${weather.feelsLike.toInt()}°"
        findViewById<TextView>(R.id.tvDescription).text = weather.description
        findViewById<TextView>(R.id.tvHumidity).text = "${weather.humidity}%"
        findViewById<TextView>(R.id.tvWindSpeed).text = "${weather.windSpeed} m/s"
    }
}
