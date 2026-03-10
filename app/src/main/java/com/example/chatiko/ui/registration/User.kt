package com.example.chatiko.ui.registration

data class User(
    val username: String?,
    val email: String?,
    val password: String?,
    val latitude: Double?,
    val longitude: Double?,
    val isOnline: Boolean?,
)