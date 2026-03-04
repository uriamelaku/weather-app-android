package com.example.weather_check

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.weather_check.models.WeatherResponse
import com.example.weather_check.repository.NetworkRepository
import com.example.weather_check.utils.TokenManager
import com.example.weather_check.utils.toWeatherResponse

class FavoritesActivity : AppCompatActivity() {
    private var favoriteItems = mutableListOf<WeatherResponse>()
    private lateinit var listAdapter: WeatherListAdapter
    private lateinit var rvList: RecyclerView
    private lateinit var btnBack: Button
    private lateinit var btnClearFavorites: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_favorites)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val tvTitle = findViewById<TextView>(R.id.tvTitle)
        tvTitle.text = getString(R.string.favorites_title)

        rvList = findViewById(R.id.rvList)
        rvList.layoutManager = LinearLayoutManager(this)

        listAdapter = WeatherListAdapter { item -> onDeleteItem(item) }
        rvList.adapter = listAdapter

        btnBack = findViewById(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }

        // Setup Clear All button
        btnClearFavorites = findViewById(R.id.btnClearFavorites)
        btnClearFavorites.setOnClickListener {
            showClearFavoritesConfirmation()
        }

        loadFavoritesData()
    }

    private fun loadFavoritesData() {
        val token = TokenManager.getToken(this)
        if (token == null) {
            Toast.makeText(this, getString(R.string.token_missing_error), Toast.LENGTH_LONG).show()
            redirectToLogin()
            return
        }

        btnBack.isEnabled = false
        btnClearFavorites.isEnabled = false
        NetworkRepository.getFavorites(
            token = token,
            onSuccess = { response ->
                runOnUiThread {
                    favoriteItems = response.favorites.map { it.toWeatherResponse() }.toMutableList()
                    listAdapter.submitItems(favoriteItems)
                    btnBack.isEnabled = true
                    btnClearFavorites.isEnabled = true
                }
            },
            onError = { error, statusCode ->
                runOnUiThread {
                    btnBack.isEnabled = true
                    btnClearFavorites.isEnabled = true
                    when (statusCode) {
                        401 -> {
                            Toast.makeText(this, getString(R.string.token_invalid_error), Toast.LENGTH_LONG).show()
                            redirectToLogin()
                        }
                        else -> Toast.makeText(this, "Failed to load favorites: $error", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    private fun onDeleteItem(item: WeatherResponse) {
        val token = TokenManager.getToken(this)
        if (token == null) {
            Toast.makeText(this, getString(R.string.token_missing_error), Toast.LENGTH_LONG).show()
            redirectToLogin()
            return
        }

        // Show confirmation dialog before deleting
        AlertDialog.Builder(this)
            .setTitle("Remove Favorite")
            .setMessage("Remove ${item.city} from favorites?")
            .setPositiveButton("Yes") { _, _ ->
                removeFavoriteFromServer(token, item)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun removeFavoriteFromServer(token: String, item: WeatherResponse) {
        btnBack.isEnabled = false
        btnClearFavorites.isEnabled = false
        Log.d("FavoritesActivity", "Removing favorite: ${item.city}")
        Log.d("FavoritesActivity", "Current favorites count: ${favoriteItems.size}")

        NetworkRepository.removeFavorite(
            token = token,
            city = item.city,
            onSuccess = { response ->
                runOnUiThread {
                    Log.d("FavoritesActivity", "Server returned ${response.favorites.size} favorites")
                    response.favorites.forEach {
                        Log.d("FavoritesActivity", "Favorite from server: ${it.city}")
                    }

                    // Create completely new list from server response
                    favoriteItems.clear()
                    favoriteItems.addAll(response.favorites.map { it.toWeatherResponse() })
                    Log.d("FavoritesActivity", "New favorites count: ${favoriteItems.size}")

                    listAdapter.submitItems(favoriteItems)
                    btnBack.isEnabled = true
                    btnClearFavorites.isEnabled = true
                    Toast.makeText(this, "Removed from favorites", Toast.LENGTH_SHORT).show()
                }
            },
            onError = { error, statusCode ->
                runOnUiThread {
                    Log.e("FavoritesActivity", "Failed to remove favorite: $error")
                    btnBack.isEnabled = true
                    btnClearFavorites.isEnabled = true
                    when (statusCode) {
                        401 -> {
                            Toast.makeText(this, getString(R.string.token_invalid_error), Toast.LENGTH_LONG).show()
                            redirectToLogin()
                        }
                        else -> {
                            Toast.makeText(this, "Failed to remove favorite: $error", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        )
    }


    private fun showClearFavoritesConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Clear Favorites")
            .setMessage("Remove all favorites? This action cannot be undone.")
            .setPositiveButton("Yes") { _, _ ->
                clearAllFavorites()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun clearAllFavorites() {
        val token = TokenManager.getToken(this)
        if (token == null) {
            Toast.makeText(this, getString(R.string.token_missing_error), Toast.LENGTH_LONG).show()
            redirectToLogin()
            return
        }

        btnBack.isEnabled = false
        btnClearFavorites.isEnabled = false
        NetworkRepository.clearFavorites(
            token = token,
            onSuccess = {
                runOnUiThread {
                    favoriteItems.clear()
                    listAdapter.submitItems(favoriteItems)
                    btnBack.isEnabled = true
                    btnClearFavorites.isEnabled = true
                    Toast.makeText(this, "Favorites cleared", Toast.LENGTH_SHORT).show()
                }
            },
            onError = { error, statusCode ->
                runOnUiThread {
                    btnBack.isEnabled = true
                    btnClearFavorites.isEnabled = true
                    when (statusCode) {
                        401 -> {
                            Toast.makeText(this, getString(R.string.token_invalid_error), Toast.LENGTH_LONG).show()
                            redirectToLogin()
                        }
                        else -> Toast.makeText(this, "Failed to clear favorites: $error", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    private fun redirectToLogin() {
        TokenManager.clearToken(this)
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

