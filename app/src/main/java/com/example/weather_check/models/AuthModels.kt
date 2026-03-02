package com.example.weather_check.models

// Request models
data class RegisterRequest(
    val username: String,
    val password: String
)

data class LoginRequest(
    val username: String,
    val password: String
)

// Response models
data class RegisterResponse(
    val message: String
)

data class LoginResponse(
    val message: String,
    val token: String,
    val user: User
)

data class User(
    val id: String,  // השרת שולח "id" ולא "_id"
    val username: String
)

data class ErrorResponse(
    val error: String
)

