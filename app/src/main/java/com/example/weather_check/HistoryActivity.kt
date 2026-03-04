package com.example.weather_check

import android.content.Intent
import android.os.Bundle
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

class HistoryActivity : AppCompatActivity() {
    private var historyItems = mutableListOf<WeatherResponse>()
    private lateinit var listAdapter: WeatherListAdapter
    private lateinit var rvList: RecyclerView
    private lateinit var btnBack: Button
    private lateinit var btnClearHistory: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_history)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val tvTitle = findViewById<TextView>(R.id.tvTitle)
        tvTitle.text = getString(R.string.history_title)

        rvList = findViewById(R.id.rvList)
        rvList.layoutManager = LinearLayoutManager(this)

        // Setup delete callback for individual items
        listAdapter = WeatherListAdapter { item -> deleteHistoryItem(item) }
        rvList.adapter = listAdapter

        btnBack = findViewById(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }

        // Setup Clear All button
        btnClearHistory = findViewById(R.id.btnClearHistory)
        btnClearHistory.setOnClickListener {
            showClearHistoryConfirmation()
        }

        loadHistoryData()
    }

    private fun loadHistoryData() {
        val token = TokenManager.getToken(this)
        if (token == null) {
            Toast.makeText(this, getString(R.string.token_missing_error), Toast.LENGTH_LONG).show()
            redirectToLogin()
            return
        }

        btnBack.isEnabled = false
        btnClearHistory.isEnabled = false
        NetworkRepository.getHistory(
            token = token,
            onSuccess = { response ->
                runOnUiThread {
                    historyItems = response.history.map { it.toWeatherResponse() }.toMutableList()
                    listAdapter.submitItems(historyItems)
                    btnBack.isEnabled = true
                    btnClearHistory.isEnabled = true
                }
            },
            onError = { error, statusCode ->
                runOnUiThread {
                    btnBack.isEnabled = true
                    btnClearHistory.isEnabled = true
                    when (statusCode) {
                        401 -> {
                            Toast.makeText(this, getString(R.string.token_invalid_error), Toast.LENGTH_LONG).show()
                            redirectToLogin()
                        }
                        else -> Toast.makeText(this, "Failed to load history: $error", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    private fun showClearHistoryConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Clear History")
            .setMessage("Remove all history? This action cannot be undone.")
            .setPositiveButton("Yes") { _, _ ->
                clearAllHistory()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun clearAllHistory() {
        val token = TokenManager.getToken(this)
        if (token == null) {
            Toast.makeText(this, getString(R.string.token_missing_error), Toast.LENGTH_LONG).show()
            redirectToLogin()
            return
        }

        btnBack.isEnabled = false
        btnClearHistory.isEnabled = false
        NetworkRepository.clearHistory(
            token = token,
            onSuccess = {
                runOnUiThread {
                    historyItems.clear()
                    listAdapter.submitItems(historyItems)
                    btnBack.isEnabled = true
                    btnClearHistory.isEnabled = true
                    Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show()
                }
            },
            onError = { error, statusCode ->
                runOnUiThread {
                    btnBack.isEnabled = true
                    btnClearHistory.isEnabled = true
                    when (statusCode) {
                        401 -> {
                            Toast.makeText(this, getString(R.string.token_invalid_error), Toast.LENGTH_LONG).show()
                            redirectToLogin()
                        }
                        else -> Toast.makeText(this, "Failed to clear history: $error", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    private fun deleteHistoryItem(item: WeatherResponse) {
        val token = TokenManager.getToken(this)
        if (token == null) {
            Toast.makeText(this, getString(R.string.token_missing_error), Toast.LENGTH_LONG).show()
            redirectToLogin()
            return
        }

        // Remove from local list immediately for instant UI update
        historyItems.removeAll { it.timestamp == item.timestamp }
        listAdapter.submitItems(historyItems)

        btnBack.isEnabled = false
        btnClearHistory.isEnabled = false
        NetworkRepository.removeHistoryItem(
            token = token,
            timestamp = item.timestamp,  // Use timestamp to identify specific item
            onSuccess = { response ->
                runOnUiThread {
                    // Update with server response to ensure sync
                    historyItems = response.history.map { it.toWeatherResponse() }.toMutableList()
                    listAdapter.submitItems(historyItems)
                    btnBack.isEnabled = true
                    btnClearHistory.isEnabled = true
                    Toast.makeText(this, "History item removed", Toast.LENGTH_SHORT).show()
                }
            },
            onError = { error, statusCode ->
                runOnUiThread {
                    btnBack.isEnabled = true
                    btnClearHistory.isEnabled = true
                    when (statusCode) {
                        401 -> {
                            Toast.makeText(this, getString(R.string.token_invalid_error), Toast.LENGTH_LONG).show()
                            redirectToLogin()
                        }
                        else -> {
                            Toast.makeText(this, "Failed to remove history item: $error", Toast.LENGTH_LONG).show()
                            // Reload from server on error to restore state
                            loadHistoryData()
                        }
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

