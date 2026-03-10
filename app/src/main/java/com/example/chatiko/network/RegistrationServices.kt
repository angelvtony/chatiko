package com.example.chatiko.network

import com.example.chatiko.ui.login.LoginRequest
import com.example.chatiko.ui.login.LoginResponse
import com.example.chatiko.ui.preferences.PreferenceModel
import com.example.chatiko.ui.registration.RegistrationResponse
import com.example.chatiko.ui.registration.User
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface RegistrationServices {

    @POST("api/auth/signup")
    fun registerUser(
        @Body request: User
    ): Call<RegistrationResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("api/preferences")
    fun savePreferences(
        @Header("Authorization") token: String,
        @Body preference: PreferenceModel
    ): Call<Map<String, Any>>

}