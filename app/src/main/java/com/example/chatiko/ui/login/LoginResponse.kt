package com.example.chatiko.ui.login

data class LoginResponse(
    val token: String?,
    val error: String?
)

data class LoginRequest(
    val username: String,
    val password: String
)
