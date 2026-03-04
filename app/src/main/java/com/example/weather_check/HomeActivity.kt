package com.example.weather_check

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.weather_check.api.ApiHelper
import com.example.weather_check.models.WeatherResponse
import com.example.weather_check.repository.NetworkRepository
import com.example.weather_check.utils.TokenManager
import com.example.weather_check.utils.toWeatherResponse
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
    private enum class SelectedMode { NONE, HISTORY, FAVORITES }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    private lateinit var cityInput: TextInputEditText
    private lateinit var emptyStateCard: CardView
    private lateinit var weatherCard: CardView

    private lateinit var listSection: LinearLayout
    private lateinit var tvListTitle: TextView
    private lateinit var tvListEmpty: TextView
    private lateinit var rvList: RecyclerView
    private lateinit var btnToggleFavorite: Button

    private lateinit var listAdapter: WeatherListAdapter

    private var currentWeather: WeatherResponse? = null
    private var selectedMode: SelectedMode = SelectedMode.NONE
    private var historyItems = mutableListOf<WeatherResponse>()
    private var favoriteItems = mutableListOf<WeatherResponse>()

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

        listSection = findViewById(R.id.listSection)
        tvListTitle = findViewById(R.id.tvListTitle)
        tvListEmpty = findViewById(R.id.tvListEmpty)
        rvList = findViewById(R.id.rvList)

        btnToggleFavorite = findViewById(R.id.btnToggleFavorite)

        // Display greeting with username
        val tvGreeting = findViewById<TextView>(R.id.tvGreeting)
        val username = TokenManager.getUsername(this)
        tvGreeting.text = if (username.isNullOrEmpty()) {
            getString(R.string.greeting_without_name)
        } else {
            getString(R.string.greeting, username)
        }

        // Setup logout button
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        btnLogout.setOnClickListener {
            TokenManager.clearToken(this)
            Toast.makeText(this, getString(R.string.logout_success), Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        listAdapter = WeatherListAdapter { item -> onDeleteListItem(item) }
        rvList.layoutManager = LinearLayoutManager(this)
        rvList.adapter = listAdapter

        // Keep list section hidden until explicit History/Favorites tap.
        selectedMode = SelectedMode.NONE
        listSection.visibility = View.GONE
        tvListEmpty.visibility = View.GONE
        rvList.visibility = View.GONE

        findViewById<Button>(R.id.btnSearch).setOnClickListener {
            val city = cityInput.text.toString().trim()
            if (city.isEmpty()) {
                Toast.makeText(this, getString(R.string.empty_city_error), Toast.LENGTH_SHORT).show()
            } else {
                fetchWeather(city)
            }
        }

        findViewById<Button>(R.id.btnHistory).setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnFavorites).setOnClickListener {
            val intent = Intent(this, FavoritesActivity::class.java)
            startActivity(intent)
        }

        btnToggleFavorite.setOnClickListener { toggleCurrentFavorite() }

        updateToggleButtons()
    }

    override fun onResume() {
        super.onResume()
        // Reload favorites from server when returning to this activity
        // This ensures the favorite button state is up-to-date
        val token = TokenManager.getToken(this)
        if (token != null) {
            loadFavoritesFromServer(token)
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
                                addToHistoryFromSearch(weatherResponse)
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

    private fun addToHistoryFromSearch(weather: WeatherResponse) {
        historyItems.removeAll { isSameLocation(it, weather) }
        historyItems.add(0, weather)
        if (selectedMode == SelectedMode.HISTORY) {
            renderSelectedList()
        }
    }

    private fun toggleCurrentFavorite() {
        val weather = currentWeather ?: run {
            Toast.makeText(this, getString(R.string.select_weather_first), Toast.LENGTH_SHORT).show()
            return
        }

        val token = TokenManager.getToken(this)
        if (token == null) {
            Toast.makeText(this, getString(R.string.token_missing_error), Toast.LENGTH_LONG).show()
            return
        }

        val isFavorite = favoriteItems.any { isSameLocation(it, weather) }

        if (isFavorite) {
            // Remove from favorites
            btnToggleFavorite.isEnabled = false
            NetworkRepository.removeFavorite(
                token = token,
                city = weather.city,
                onSuccess = { response ->
                    runOnUiThread {
                        favoriteItems = response.favorites.map { it.toWeatherResponse() }.toMutableList()
                        updateToggleButtons()
                        btnToggleFavorite.isEnabled = true
                        Toast.makeText(this, "Removed from favorites", Toast.LENGTH_SHORT).show()
                    }
                },
                onError = { error, statusCode ->
                    runOnUiThread {
                        btnToggleFavorite.isEnabled = true
                        when (statusCode) {
                            401 -> {
                                Toast.makeText(this, getString(R.string.token_invalid_error), Toast.LENGTH_LONG).show()
                                handleUnauthorized()
                            }
                            else -> {
                                Toast.makeText(this, "Failed to remove favorite: $error", Toast.LENGTH_LONG).show()
                                // Reload favorites to sync state
                                loadFavoritesFromServer(token)
                            }
                        }
                    }
                }
            )
        } else {
            // Add to favorites
            btnToggleFavorite.isEnabled = false
            NetworkRepository.addFavorite(
                token = token,
                city = weather.city,
                country = weather.country,
                onSuccess = { response ->
                    runOnUiThread {
                        favoriteItems = response.favorites.map { it.toWeatherResponse() }.toMutableList()
                        updateToggleButtons()
                        btnToggleFavorite.isEnabled = true
                        Toast.makeText(this, "Added to favorites", Toast.LENGTH_SHORT).show()
                    }
                },
                onError = { error, statusCode ->
                    runOnUiThread {
                        btnToggleFavorite.isEnabled = true
                        when (statusCode) {
                            401 -> {
                                Toast.makeText(this, getString(R.string.token_invalid_error), Toast.LENGTH_LONG).show()
                                handleUnauthorized()
                            }
                            409 -> {
                                Toast.makeText(this, "City already in favorites", Toast.LENGTH_SHORT).show()
                                // Reload favorites to sync state
                                loadFavoritesFromServer(token)
                            }
                            400 -> Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                            else -> {
                                Toast.makeText(this, "Failed to add favorite: $error", Toast.LENGTH_LONG).show()
                                // Reload favorites to sync state
                                loadFavoritesFromServer(token)
                            }
                        }
                    }
                }
            )
        }
    }

    private fun loadFavoritesFromServer(token: String) {
        NetworkRepository.getFavorites(
            token = token,
            onSuccess = { response ->
                runOnUiThread {
                    favoriteItems = response.favorites.map { it.toWeatherResponse() }.toMutableList()
                    updateToggleButtons()
                    if (selectedMode == SelectedMode.FAVORITES) {
                        renderSelectedList()
                    }
                }
            },
            onError = { error, statusCode ->
                // Silent fail - just log the issue
                if (statusCode == 401) {
                    runOnUiThread { handleUnauthorized() }
                }
            }
        )
    }

    private fun onDeleteListItem(item: WeatherResponse) {
        val token = TokenManager.getToken(this)
        if (token == null) {
            Toast.makeText(this, getString(R.string.token_missing_error), Toast.LENGTH_LONG).show()
            return
        }

        when (selectedMode) {
            SelectedMode.HISTORY -> {
                // Call server API to delete history item
                NetworkRepository.removeHistoryItem(
                    token = token,
                    city = item.city,
                    onSuccess = { response ->
                        runOnUiThread {
                            historyItems = response.history.map { it.toWeatherResponse() }.toMutableList()
                            renderSelectedList()
                            Toast.makeText(this, "History item removed", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onError = { error, statusCode ->
                        runOnUiThread {
                            when (statusCode) {
                                401 -> {
                                    Toast.makeText(this, getString(R.string.token_invalid_error), Toast.LENGTH_LONG).show()
                                    handleUnauthorized()
                                }
                                else -> Toast.makeText(this, "Failed to remove history item: $error", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )
            }
            SelectedMode.FAVORITES -> {
                // Call server API to delete favorite item
                NetworkRepository.removeFavorite(
                    token = token,
                    city = item.city,
                    onSuccess = { response ->
                        runOnUiThread {
                            favoriteItems = response.favorites.map { it.toWeatherResponse() }.toMutableList()
                            updateToggleButtons()
                            renderSelectedList()
                            Toast.makeText(this, "Removed from favorites", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onError = { error, statusCode ->
                        runOnUiThread {
                            when (statusCode) {
                                401 -> {
                                    Toast.makeText(this, getString(R.string.token_invalid_error), Toast.LENGTH_LONG).show()
                                    handleUnauthorized()
                                }
                                else -> Toast.makeText(this, "Failed to remove favorite: $error", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )
            }
            SelectedMode.NONE -> return
        }
    }

    private fun renderSelectedList() {
        val itemsToShow = when (selectedMode) {
            SelectedMode.HISTORY -> historyItems
            SelectedMode.FAVORITES -> favoriteItems
            SelectedMode.NONE -> emptyList()
        }

        if (selectedMode == SelectedMode.NONE) {
            listSection.visibility = View.GONE
            return
        }

        tvListTitle.text = when (selectedMode) {
            SelectedMode.HISTORY -> getString(R.string.history_title)
            SelectedMode.FAVORITES -> getString(R.string.favorites_title)
            SelectedMode.NONE -> ""
        }

        listSection.visibility = View.VISIBLE
        listAdapter.submitItems(itemsToShow)
        tvListEmpty.visibility = if (itemsToShow.isEmpty()) View.VISIBLE else View.GONE
        rvList.visibility = if (itemsToShow.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun updateToggleButtons() {
        val weather = currentWeather
        if (weather == null) {
            btnToggleFavorite.text = getString(R.string.add_to_favorites)
            return
        }

        val isFavorite = favoriteItems.any { isSameLocation(it, weather) }

        btnToggleFavorite.text = if (isFavorite) {
            getString(R.string.remove_from_favorites)
        } else {
            getString(R.string.add_to_favorites)
        }
    }

    private fun isSameLocation(first: WeatherResponse, second: WeatherResponse): Boolean {
        return first.city.equals(second.city, ignoreCase = true) &&
            first.country.equals(second.country, ignoreCase = true)
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

        updateToggleButtons()
    }
}
