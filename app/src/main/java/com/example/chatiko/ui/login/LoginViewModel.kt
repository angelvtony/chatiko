package com.example.chatiko.ui.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatiko.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException

class LoginViewModel : ViewModel() {

    private val _loginState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val loginState: StateFlow<LoginUiState> = _loginState

    fun login(context: Context, username: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginUiState.Loading // Show loading state
            try {
                val response = RetrofitClient.instance.login(LoginRequest(username, password))
                if (!response.token.isNullOrEmpty()) {
                    _loginState.value = LoginUiState.Success(response.token)

                    val sharedPref = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    sharedPref.edit().putString("jwt_token", response.token).apply()

// Retrieve token
                    val token = sharedPref.getString("jwt_token", null)
                } else {
                    _loginState.value = LoginUiState.Error(response.error ?: "Login failed")
                }
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                val message = parseErrorMessage(errorBody) ?: "Bad Request"
                _loginState.value = LoginUiState.Error(message)
            } catch (e: IOException) {
                _loginState.value = LoginUiState.Error("Network Error: ${e.message}")
            } catch (e: Exception) {
                _loginState.value = LoginUiState.Error("Error: ${e.message}")
            }
        }
    }

    private fun parseErrorMessage(errorBody: String?): String? {
        return try {
            val json = JSONObject(errorBody ?: "")
            json.getString("message") // Ensure this matches your backend JSON key
        } catch (e: Exception) {
            null
        }
    }
}

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    data class Success(val token: String) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}