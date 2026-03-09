package com.example.chatiko.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatiko.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    private val _loginState = MutableStateFlow<String?>(null)
    val loginState: StateFlow<String?> = _loginState

    fun login(username: String, password: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.login(LoginRequest(username, password))
                if (response.token != null) {
                    _loginState.value = "Login Successful! Token: ${response.token}"
                } else {
                    _loginState.value = "Login Failed: ${response.error}"
                }
            } catch (e: Exception) {
                _loginState.value = "Error: ${e.message}"
            }
        }
    }
}