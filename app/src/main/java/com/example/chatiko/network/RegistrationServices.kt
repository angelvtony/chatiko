package com.example.chatiko.network

import com.example.chatiko.ui.registration.RegistrationResponse
import com.example.chatiko.ui.registration.User
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface RegistrationServices {

    @POST("api/auth/signup")
    fun registerUser(
        @Body request: User
    ): Call<RegistrationResponse>

}