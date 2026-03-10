package com.example.chatiko.ui.preferences

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.chatiko.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PreferencesViewModel : ViewModel() {

    private val _statusMessage = MutableLiveData<String>()
    val statusMessage: LiveData<String> get() = _statusMessage

    fun savePreferences(jwtToken: String?, preference: PreferenceModel) {
        val authHeader = "Bearer $jwtToken"
        RetrofitClient.instance.savePreferences(authHeader, preference)
            .enqueue(object : Callback<Map<String, Any>> {
                override fun onResponse(
                    call: Call<Map<String, Any>>,
                    response: Response<Map<String, Any>>
                ) {
                    if (response.isSuccessful) {
                        _statusMessage.value = "Preferences saved successfully"
                    } else {
                        _statusMessage.value = "Error: ${response.errorBody()?.string()}"
                    }
                }

                override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                    _statusMessage.value = "Network error: ${t.localizedMessage}"
                }
            })
    }
}