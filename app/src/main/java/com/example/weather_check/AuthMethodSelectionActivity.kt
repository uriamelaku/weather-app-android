package com.example.weather_check

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.weather_check.api.RetrofitProvider
import com.example.weather_check.models.SendOtpRequest
import com.example.weather_check.models.SendOtpResponse
import com.example.weather_check.models.ErrorResponse
import com.example.weather_check.models.DevLoginRequest
import com.example.weather_check.models.DevLoginResponse
import com.example.weather_check.utils.TokenManager
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import retrofit2.Response as RetrofitResponse
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

class AuthMethodSelectionActivity : AppCompatActivity() {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private var isProcessing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_auth_method_selection)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val tvEmail = findViewById<TextView>(R.id.tvEmailAddress)
        val btnEmailVerification = findViewById<Button>(R.id.btnEmailVerification)
        val btnDevMode = findViewById<Button>(R.id.btnDevMode)

        val email = TokenManager.getUserEmail(this)
        tvEmail.text = "Code will be sent to: $email"

        btnEmailVerification.setOnClickListener {
            if (!isProcessing) sendOtp()
        }

        btnDevMode.setOnClickListener {
            if (!isProcessing) devLogin()
        }
    }

    private fun sendOtp() {
        val otpToken = TokenManager.getOtpToken(this) ?: run {
            Toast.makeText(this, "OTP token expired. Please login again.", Toast.LENGTH_LONG).show()
            navigateToLogin()
            return
        }

        isProcessing = true
        val sendOtpRequest = SendOtpRequest(otpToken)

        RetrofitProvider.authApi
            .sendOtp("Bearer $otpToken", sendOtpRequest)
            .enqueue(object : retrofit2.Callback<SendOtpResponse> {
                override fun onResponse(
                    call: retrofit2.Call<SendOtpResponse>,
                    response: RetrofitResponse<SendOtpResponse>
                ) {
                    isProcessing = false
                    val responseCode = response.code()
                    val responseBody = response.body()
                    val rawErrorBody = try {
                        response.errorBody()?.string()
                    } catch (_: Exception) {
                        null
                    }

                    android.util.Log.d("SendOtp", "response.code=$responseCode")
                    android.util.Log.d("SendOtp", "response.body=${gson.toJson(responseBody)}")
                    android.util.Log.d("SendOtp", "raw.error.body=$rawErrorBody")

                    runOnUiThread {
                        if (response.isSuccessful && responseBody != null && responseBody.otpSent) {
                            navigateToOtpScreen()
                            return@runOnUiThread
                        }

                        if (responseCode == 401) {
                            Toast.makeText(
                                this@AuthMethodSelectionActivity,
                                "OTP token expired. Please login again.",
                                Toast.LENGTH_LONG
                            ).show()
                            navigateToLogin()
                            return@runOnUiThread
                        }

                        if (response.isSuccessful && responseBody != null && !responseBody.otpSent) {
                            Toast.makeText(
                                this@AuthMethodSelectionActivity,
                                "OTP response received but otpSent=false",
                                Toast.LENGTH_LONG
                            ).show()
                            return@runOnUiThread
                        }

                        val parsedError = try {
                            if (!rawErrorBody.isNullOrBlank()) {
                                gson.fromJson(rawErrorBody, ErrorResponse::class.java).error
                            } else null
                        } catch (_: Exception) {
                            null
                        }

                        Toast.makeText(
                            this@AuthMethodSelectionActivity,
                            parsedError ?: "Failed to send OTP ($responseCode)",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(call: retrofit2.Call<SendOtpResponse>, t: Throwable) {
                    isProcessing = false
                    android.util.Log.e("SendOtp", "onFailure exception=${t.message}", t)

                    runOnUiThread {
                        if (t is SocketTimeoutException || t.message?.contains("timeout", ignoreCase = true) == true) {
                            showTimeoutRecoveryDialog()
                        } else {
                            Toast.makeText(
                                this@AuthMethodSelectionActivity,
                                "Network error: ${t.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            })
    }

    private fun showTimeoutRecoveryDialog() {
        AlertDialog.Builder(this)
            .setTitle("OTP request timeout")
            .setMessage(
                "The server response timed out. If the email already arrived, you can continue to the OTP screen."
            )
            .setPositiveButton("Continue to OTP") { _, _ ->
                navigateToOtpScreen()
            }
            .setNegativeButton("Try again", null)
            .show()
    }

    private fun navigateToOtpScreen() {
        val intent = Intent(this@AuthMethodSelectionActivity, OTPVerificationActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun devLogin() {
        val otpToken = TokenManager.getOtpToken(this) ?: run {
            Toast.makeText(this, "OTP token expired. Please login again.", Toast.LENGTH_LONG).show()
            navigateToLogin()
            return
        }

        isProcessing = true
        val devLoginRequest = DevLoginRequest(otpToken)
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = gson.toJson(devLoginRequest).toRequestBody(mediaType)

        val request = Request.Builder()
            .url(ApiConfig.BASE_URL + ApiConfig.DEV_LOGIN_ENDPOINT)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                isProcessing = false
                runOnUiThread {
                    Toast.makeText(this@AuthMethodSelectionActivity, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                isProcessing = false
                val responseBody = response.body?.string()

                runOnUiThread {
                    try {
                        when {
                            response.isSuccessful -> {
                                try {
                                    val devLoginResponse = gson.fromJson(responseBody, DevLoginResponse::class.java)
                                    TokenManager.saveJwtToken(this@AuthMethodSelectionActivity, devLoginResponse.token, "", devLoginResponse.username)
                                    TokenManager.clearOtpToken(this@AuthMethodSelectionActivity)

                                    val intent = Intent(this@AuthMethodSelectionActivity, HomeActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(intent)
                                    finish()
                                } catch (e: Exception) {
                                    Toast.makeText(this@AuthMethodSelectionActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                            response.code == 403 -> {
                                Toast.makeText(this@AuthMethodSelectionActivity, "Dev login is disabled on this server", Toast.LENGTH_LONG).show()
                            }
                            response.code == 401 -> {
                                Toast.makeText(this@AuthMethodSelectionActivity, "OTP token expired. Please login again.", Toast.LENGTH_LONG).show()
                                navigateToLogin()
                            }
                            else -> {
                                try {
                                    val error = gson.fromJson(responseBody, ErrorResponse::class.java)
                                    Toast.makeText(this@AuthMethodSelectionActivity, "Error: ${error.error}", Toast.LENGTH_LONG).show()
                                } catch (_: Exception) {
                                    Toast.makeText(this@AuthMethodSelectionActivity, "Error: ${response.code}", Toast.LENGTH_LONG).show()
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

    private fun navigateToLogin() {
        TokenManager.clearAllTokens(this)
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
