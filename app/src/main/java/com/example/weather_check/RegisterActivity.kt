package com.example.weather_check

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.weather_check.models.RegisterRequest
import com.example.weather_check.models.RegisterResponse
import com.example.weather_check.models.ErrorResponse
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

class RegisterActivity : AppCompatActivity() {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val etNewUsername = findViewById<EditText>(R.id.etNewUsername)
        val etNewPassword = findViewById<EditText>(R.id.etNewPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        btnRegister.setOnClickListener {
            val username = etNewUsername.text.toString().trim()
            val password = etNewPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, getString(R.string.empty_fields_error), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            registerUser(username, password)
        }
    }

    private fun registerUser(username: String, password: String) {
        val registerRequest = RegisterRequest(username, password)
        val json = gson.toJson(registerRequest)

        // לוגינג לבדיקה
        android.util.Log.d("RegisterActivity", "Request URL: ${ApiConfig.BASE_URL}${ApiConfig.REGISTER_ENDPOINT}")
        android.util.Log.d("RegisterActivity", "Request JSON: $json")

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = json.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(ApiConfig.BASE_URL + ApiConfig.REGISTER_ENDPOINT)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(
                        this@RegisterActivity,
                        "${getString(R.string.network_error)}: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                // לוגינג לבדיקה
                android.util.Log.d("RegisterActivity", "Response code: ${response.code}")
                android.util.Log.d("RegisterActivity", "Response body: $responseBody")

                runOnUiThread {
                    when {
                        response.isSuccessful -> {
                            try {
                                gson.fromJson(responseBody, RegisterResponse::class.java)
                                Toast.makeText(
                                    this@RegisterActivity,
                                    getString(R.string.registration_success),
                                    Toast.LENGTH_LONG
                                ).show()
                                // Contract flow: register -> login (token is returned only from /login)
                                val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                                startActivity(intent)
                                finish()
                            } catch (e: Exception) {
                                android.util.Log.e("RegisterActivity", "Parse error", e)
                                Toast.makeText(
                                    this@RegisterActivity,
                                    "${getString(R.string.registration_error)}: ${e.message}\nתגובה: $responseBody",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                        response.code == 400 -> {
                            try {
                                val errorResponse = gson.fromJson(responseBody, ErrorResponse::class.java)
                                val errorMessage = if (errorResponse.error.contains("already exists", ignoreCase = true)) {
                                    getString(R.string.user_exists_error)
                                } else {
                                    errorResponse.error
                                }
                                Toast.makeText(
                                    this@RegisterActivity,
                                    errorMessage,
                                    Toast.LENGTH_LONG
                                ).show()
                            } catch (e: Exception) {
                                android.util.Log.e("RegisterActivity", "Error parse error", e)
                                Toast.makeText(
                                    this@RegisterActivity,
                                    "${getString(R.string.registration_error)}: ${response.code}\nתגובה: $responseBody",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                        response.code == 409 -> {
                            Toast.makeText(
                                this@RegisterActivity,
                                getString(R.string.user_exists_error),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        else -> {
                            android.util.Log.e("RegisterActivity", "Unknown error code: ${response.code}")
                            Toast.makeText(
                                this@RegisterActivity,
                                "${getString(R.string.registration_error)}: ${response.code}\nתגובה: $responseBody",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
                response.close()
            }
        })
    }
}
