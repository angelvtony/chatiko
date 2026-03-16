package com.example.chatiko.ui.login

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatiko.network.CryptoManager
import com.example.chatiko.network.PublicKeyRequest
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

            _loginState.value = LoginUiState.Loading

            try {

                val response =
                    RetrofitClient.instance.login(
                        LoginRequest(username, password)
                    )

                if (!response.token.isNullOrEmpty()) {

                    val sharedPref =
                        context.getSharedPreferences(
                            "user_prefs",
                            Context.MODE_PRIVATE
                        )

                    sharedPref.edit()
                        .putString("jwt_token", response.token)
                        .apply()

                    sharedPref.edit()
                        .putString("userId", response.user._id)
                        .apply()

                    // 🔐 Generate encryption keys
                    CryptoManager.generateKeyPair()

                    // 🔐 Upload public key
                    uploadPublicKey(context, response.token)

                    _loginState.value =
                        LoginUiState.Success(response.token)

                } else {

                    _loginState.value =
                        LoginUiState.Error(
                            response.error ?: "Login failed"
                        )
                }

            } catch (e: HttpException) {

                val errorBody =
                    e.response()?.errorBody()?.string()

                val message =
                    parseErrorMessage(errorBody)
                        ?: "Bad Request"

                _loginState.value =
                    LoginUiState.Error(message)

            } catch (e: IOException) {

                _loginState.value =
                    LoginUiState.Error(
                        "Network Error: ${e.message}"
                    )

            } catch (e: Exception) {

                _loginState.value =
                    LoginUiState.Error(
                        "Error: ${e.message}"
                    )
            }
        }
    }

    private suspend fun uploadPublicKey(
        context: Context,
        token: String
    ) {

        try {

            val publicKey = CryptoManager.getPublicKeyString()
            Log.d("DEBUG_KEY", "My public key: $publicKey")

            val authHeader =
                "Bearer $token"

            RetrofitClient.instance.uploadPublicKey(
                PublicKeyRequest(publicKey),
                authHeader
            )

        } catch (e: Exception) {

            e.printStackTrace()

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