package com.example.chatiko.network

import com.example.chatiko.ui.login.LoginRequest
import com.example.chatiko.ui.login.LoginResponse
import com.example.chatiko.ui.preferences.PreferenceModel
import com.example.chatiko.ui.registration.RegistrationResponse
import com.example.chatiko.ui.registration.User
import com.example.chatiko.ui.vibes.NearbyVibeUser
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

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

    @POST("api/users/{id}/location")
    suspend fun updateLocation(
        @Path("id") userId: String?,
        @Body location: User
    ): User

    @GET("api/users/nearby")
    suspend fun getNearbyUsers(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("radius") radius: Int = 1000
    ): List<NearbyVibeUser>

}