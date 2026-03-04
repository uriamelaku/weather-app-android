package com.example.weather_check

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.weather_check.utils.TokenManager
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if user is already logged in and redirect to HomeActivity
        if (TokenManager.isLoggedIn(this)) {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnGoToLogin = findViewById<Button>(R.id.btnGoToLogin)
        val btnCheckServer = findViewById<Button>(R.id.btnCheckServer)
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        val tvUserStatus = findViewById<TextView>(R.id.tvUserStatus)

        updateUIBasedOnLoginStatus()

        btnGoToLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        btnCheckServer.setOnClickListener {
            checkServerConnection()
        }

        btnLogout.setOnClickListener {
            TokenManager.clearToken(this)
            Toast.makeText(this, getString(R.string.logout_success), Toast.LENGTH_SHORT).show()
            updateUIBasedOnLoginStatus()
        }
    }

    override fun onResume() {
        super.onResume()
        // No need to redirect again since onCreate handles it on app start
        // If we return from Login after logout, buttons will show correctly
        updateUIBasedOnLoginStatus()
    }

    private fun updateUIBasedOnLoginStatus() {
        val btnGoToLogin = findViewById<Button>(R.id.btnGoToLogin)
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        val tvUserStatus = findViewById<TextView>(R.id.tvUserStatus)

        if (TokenManager.isLoggedIn(this)) {
            val username = TokenManager.getUsername(this)
            tvUserStatus.text = getString(R.string.logged_in_as, username)
            tvUserStatus.visibility = View.VISIBLE
            btnGoToLogin.visibility = View.GONE
            btnLogout.visibility = View.VISIBLE
        } else {
            tvUserStatus.visibility = View.GONE
            btnGoToLogin.visibility = View.VISIBLE
            btnLogout.visibility = View.GONE
        }
    }

    private fun checkServerConnection() {
        val request = Request.Builder()
            .url(ApiConfig.BASE_URL + ApiConfig.PING_ENDPOINT)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "${getString(R.string.server_disconnected)}: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@MainActivity,
                            getString(R.string.server_connected),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "${getString(R.string.server_disconnected)}: ${response.code}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                response.close()
            }
        })
    }
}

