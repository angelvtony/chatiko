package com.example.chatiko.ui.registration

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.chatiko.network.RetrofitClient
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegistrationViewModel : ViewModel() {

    // UI state
    var username by mutableStateOf("")
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var passwordVisible by mutableStateOf(false)
    var showDialog by mutableStateOf(false)
    var dialogMessage by mutableStateOf("")
    var isLoading by mutableStateOf(false)

    // Registration function
    fun registerUser() {
        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            dialogMessage = "Please fill all fields"
            showDialog = true
            return
        }

        isLoading = true

        val request = User(username = username, email = email, password = password)
        RetrofitClient.instance.registerUser(request)
            .enqueue(object : Callback<RegistrationResponse> {
                override fun onResponse(
                    call: Call<RegistrationResponse>,
                    response: Response<RegistrationResponse>
                ) {
                    isLoading = false
                    if (response.isSuccessful) {
                        dialogMessage = response.body()?.message ?: "Registration successful"
                        showDialog = true
                    } else {
                        val errorJson = response.errorBody()?.string()
                        val errorMessage = JSONObject(errorJson ?: "").optString("message")
                        dialogMessage = errorMessage
                        showDialog = true
                    }
                }

                override fun onFailure(call: Call<RegistrationResponse>, t: Throwable) {
                    isLoading = false
                    dialogMessage = "Network Error: ${t.message}"
                    showDialog = true
                }
            })
    }
}