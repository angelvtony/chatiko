package com.example.chatiko.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL = "http://10.63.0.148:3000"

    val instance: RegistrationServices by lazy {

        // Logging interceptor
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client) // <-- attach logging client
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(RegistrationServices::class.java)
    }
}