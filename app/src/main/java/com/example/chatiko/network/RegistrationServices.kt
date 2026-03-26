package com.example.chatiko.network

import com.example.chatiko.ui.chat.Message
import com.example.chatiko.ui.login.LoginRequest
import com.example.chatiko.ui.login.LoginResponse
import com.example.chatiko.ui.preferences.PreferenceModel
import com.example.chatiko.ui.registration.LocationRequest
import com.example.chatiko.ui.registration.RegistrationResponse
import com.example.chatiko.ui.registration.User
import com.example.chatiko.ui.vibes.FetchNearbyLocationElement
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
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
        @Header("Authorization") token: String,
        @Path("id") userId: String,
        @Body location: LocationRequest
    ): LocationUpdateResponse

    @GET("api/users/nearby")
    suspend fun getNearbyUsers(
        @Header("Authorization") token: String,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("radius") radius: Int = 1000
    ): List<FetchNearbyLocationElement>

    @GET("api/messages/{userId}")
    suspend fun getMessages(
        @Path("userId") userId: String?,
        @Header("Authorization") token: String
    ): List<MessageDto>

    @GET("api/users/{id}/publicKey")
    suspend fun getUserPublicKey(
        @Path("id") userId: String?,
        @Header("Authorization") token: String
    ): PublicKeyResponse

    @POST("api/users/publicKey")
    suspend fun uploadPublicKey(
        @Body request: PublicKeyRequest,
        @Header("Authorization") token: String
    )
}

data class PublicKeyRequest(
    val publicKey: String
)

data class PublicKeyResponse(
    val publicKey: String?
)


data class MessageDto(

    @SerializedName("_id")
    val id: String,

    val senderId: String?,
    val receiverId: String?,
    val message: String?,
    val reaction: String?,
    val createdAt: String?,
    val replyTo: MessageDto? = null,
    val isMe: Boolean,
    val status: String? = "sent"
)


@kotlinx.serialization.Serializable
data class LocationUpdateResponse (
    val message: String? = null,
    val user: LocationUpdateResponseUser? = null
)

@kotlinx.serialization.Serializable
data class LocationUpdateResponseUser (
    @SerialName("_id")
    val id: String? = null,

    val username: String? = null,
    val email: String? = null,
    val password: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isOnline: Boolean? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,

    @SerialName("__v")
    val v: Long? = null
)