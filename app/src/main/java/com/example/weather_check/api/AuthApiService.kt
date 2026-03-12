package com.example.weather_check.api

import com.example.weather_check.models.SendOtpRequest
import com.example.weather_check.models.SendOtpResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApiService {
    @POST("/api/auth/send-otp")
    fun sendOtp(
        @Header("Authorization") authorization: String,
        @Body request: SendOtpRequest
    ): Call<SendOtpResponse>
}

