package com.example.weather_check

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.weather_check.models.LoginRequest
import com.example.weather_check.models.LoginResponse
import com.example.weather_check.models.ErrorResponse
import com.example.weather_check.utils.TokenManager
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

class LoginActivity : AppCompatActivity() {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvRegisterLink = findViewById<TextView>(R.id.tvRegisterLink)

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            when {
                username.isEmpty() || password.isEmpty() -> {
                    Toast.makeText(this, getString(R.string.empty_fields_error), Toast.LENGTH_SHORT).show()
                }
                else -> {
                    loginUser(username, password)
                }
            }
        }

        tvRegisterLink.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loginUser(username: String, password: String) {
        val loginRequest = LoginRequest(username, password)
        val json = gson.toJson(loginRequest)

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = json.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(ApiConfig.BASE_URL + ApiConfig.LOGIN_ENDPOINT)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(
                        this@LoginActivity,
                        "${getString(R.string.network_error)}: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                runOnUiThread {
                    try {
                        if (responseBody.isNullOrEmpty()) {
                            Toast.makeText(
                                this@LoginActivity,
                                "${getString(R.string.login_error)}: Empty response from server",
                                Toast.LENGTH_LONG
                            ).show()
                            return@runOnUiThread
                        }

                        when {
                            response.isSuccessful -> {
                                try {
                                    val loginResponse = gson.fromJson(responseBody, LoginResponse::class.java)

                                    if (!loginResponse.loginOk || loginResponse.otpToken.isEmpty()) {
                                        Toast.makeText(
                                            this@LoginActivity,
                                            "${getString(R.string.login_error)}: Invalid response",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        return@runOnUiThread
                                    }

                                    TokenManager.saveOtpToken(
                                        this@LoginActivity,
                                        loginResponse.otpToken,
                                        loginResponse.email
                                    )

                                    Toast.makeText(
                                        this@LoginActivity,
                                        "Login successful. Choose verification method.",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    val intent = Intent(this@LoginActivity, AuthMethodSelectionActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(intent)
                                    finish()
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        this@LoginActivity,
                                        "${getString(R.string.login_error)}: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                            response.code == 401 -> {
                                Toast.makeText(
                                    this@LoginActivity,
                                    "Invalid credentials",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            else -> {
                                try {
                                    val errorResponse = gson.fromJson(responseBody, ErrorResponse::class.java)
                                    Toast.makeText(
                                        this@LoginActivity,
                                        "${getString(R.string.login_error)}: ${errorResponse.error}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } catch (_: Exception) {
                                    Toast.makeText(
                                        this@LoginActivity,
                                        "${getString(R.string.login_error)}: ${response.code}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    } finally {
                        response.close()
                    }
                }
            }
        })
    }
}
