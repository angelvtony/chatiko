package com.example.chatiko.ui.login

data class LoginResponse(
    val token: String?,
    val error: String?,
    val user: User
)

data class User(
    val latitude: String? = null,
    val longitude: String? = null,
    val isOnline: Boolean,
    val _id: String,
    val username: String,
    val email: String,
    val password: String,
    val createdAt: String,
    val updatedAt: String,
    val __v: Long
)

data class LoginRequest(
    val username: String,
    val password: String
)
