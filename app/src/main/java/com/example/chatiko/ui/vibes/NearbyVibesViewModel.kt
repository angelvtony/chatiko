package com.example.chatiko.ui.vibes

import android.content.Context
import android.location.Location
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.chatiko.network.LocationHelper
import com.example.chatiko.network.RegistrationServices
import com.example.chatiko.network.RetrofitClient
import com.example.chatiko.ui.registration.LocationRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NearbyVibesViewModel(
    private val context: Context?,
    private val api: RegistrationServices?,
    private val userId: String?,
    private val locationHelper: LocationHelper?,
    initialMood: String?
) : ViewModel() {

    private val _selectedMood = MutableStateFlow(initialMood ?: "Happy")
    val selectedMood: StateFlow<String> = _selectedMood

    val moods = listOf(
        "Happy","Sad","Stressed","Vibing",
        "Bored","Working","Idle","Normal"
    )

    var myLat: Double = 0.0
    var myLng: Double = 0.0

    private val _nearbyUsers =
        MutableStateFlow<List<NearbyVibeUser>>(emptyList())

    val filteredUsers: StateFlow<List<NearbyVibeUser>> =
        combine(_nearbyUsers, _selectedMood) { users, mood ->

            users
                .filterNotNull()
                .filter { it.mood == mood }

        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    private var isUpdating = false

    fun selectMood(mood: String) {
        _selectedMood.value = mood
    }

    fun startNearbyUpdates() {

        if (isUpdating) return
        isUpdating = true

        viewModelScope.launch {

            delay(2000) // allow GPS warmup

            while (true) {

                val sharedPref = context?.getSharedPreferences(
                    "user_prefs",
                    Context.MODE_PRIVATE
                )

                val jwtToken =
                    sharedPref?.getString("jwt_token", null)

                refreshNearbyUsers(jwtToken)

                delay(10000)
            }
        }
    }

    private suspend fun refreshNearbyUsers(jwtToken: String?) {

        if (jwtToken.isNullOrEmpty()) {
            Log.e("NearbyVM", "JWT token missing")
            return
        }

        try {
            val location = locationHelper?.getCurrentLocation()
            if (location == null) {
                Log.e("NearbyVM","Location null")
                return
            }

            myLat = location.latitude
            myLng = location.longitude

            Log.d("NearbyVM", "Location -> $myLat,$myLng")

            val authHeader = "Bearer $jwtToken"

            // ✅ Updated API call
            val response = RetrofitClient.instance.updateLocation(
                authHeader,
                userId.toString(),
                LocationRequest(myLat, myLng)
            )

            Log.d("NearbyVM", "Location update response: ${response?.message}")

            // Fetch nearby users
            val nearby = RetrofitClient.instance.getNearbyUsers(authHeader, myLat, myLng)

            val nearbyUsers = nearby
                .filter { it.id != userId } // exclude yourself
                .map {
                    NearbyVibeUser(
                        id = it.id,
                        username = it.username ?: "Unknown",
                        mood = it.mood ?: "Normal",    // fallback if mood is missing
                        moodEmoji = "🙂",
                        latitude = it.latitude ?: 0.0,
                        longitude = it.longitude ?: 0.0,
                        isOnline = it.isOnline ?: false
                    )
                }

// Update state
            _nearbyUsers.value = nearbyUsers

        } catch (e: Exception) {
            Log.e("NearbyVM", "API Error", e)
        }
    }
}

class NearbyVibesViewModelFactory(
    private val context: Context?,
    private val api: RegistrationServices?,
    private val userId: String?,
    private val locationHelper: LocationHelper?,
    private val initialMood: String?
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (modelClass.isAssignableFrom(
                NearbyVibesViewModel::class.java
            )
        ) {

            return NearbyVibesViewModel(
                context,
                api,
                userId,
                locationHelper,
                initialMood
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel"
        )
    }
}