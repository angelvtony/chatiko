package com.example.chatiko.ui.vibes

import android.location.Location
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.chatiko.network.LocationHelper
import com.example.chatiko.network.RegistrationServices
import com.example.chatiko.ui.registration.User
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NearbyVibesViewModel(
    private val api: RegistrationServices?,
    private val userId: String?,
    private val locationHelper: LocationHelper?,
    private val initialMood: String? = null
) : ViewModel() {

    // Selected mood
    private val _selectedMood = MutableStateFlow(initialMood?:"")
    val selectedMood: StateFlow<String> = _selectedMood

    // All moods
    val moods = listOf("Happy", "Sad", "Stressed", "Vibing", "Bored", "Working", "Idle", "Normal")

    // Current device location
     var _myLat: Double = 0.0
     var _myLng: Double = 0.0

    // Raw nearby users from API
    private val _nearbyUsers = MutableStateFlow<List<NearbyVibeUser>>(emptyList())

    // Filtered users based on selected mood
    val filteredUsers: StateFlow<List<NearbyVibeUser>> = combine(_nearbyUsers, _selectedMood) { users, mood ->
        users
            .filter { it.mood == mood } // only keep users with matching mood
            .sortedBy { user ->
                val results = FloatArray(1)
                Location.distanceBetween(_myLat, _myLng, user.latitude, user.longitude, results)
                results[0]
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Select a mood
    fun selectMood(mood: String) {
        _selectedMood.value = mood
    }

    // Refresh users and include current device location
    fun refreshNearbyUsers() {
        viewModelScope.launch {
            try {
                // 1️⃣ Get current device location
                val location = locationHelper?.getCurrentLocation()
                if (location != null) {
                    _myLat = location.latitude
                    _myLng = location.longitude

                    // 2️⃣ Update current user location on backend
                    api?.updateLocation(
                        userId,
                        User(
                            username = null,
                            email = null,
                            password = null,
                            latitude = _myLat,
                            longitude = _myLng,
                            isOnline = true
                        )
                    )

                    // 3️⃣ Fetch nearby users from backend
                    val nearby = api?.getNearbyUsers(_myLat, _myLng) ?: emptyList()

                    // 4️⃣ Add current user to the list
                    val currentUser = NearbyVibeUser(
                        id = userId,
                        username = "You",
                        mood = _selectedMood.value,
                        moodEmoji = "😊",
                        latitude = _myLat,
                        longitude = _myLng,
                        isOnline = true,
                        color = Color(0xFF6A82FB)
                    )

                    // Filter out null users just in case API returns incomplete data
                    val safeNearby = nearby.filterNotNull()

                    _nearbyUsers.value = listOf(currentUser) + safeNearby

                    Log.d("NearbyVibesVM", "Nearby users refreshed: ${_nearbyUsers.value.size}")
                } else {
                    Log.e("NearbyVibesVM", "Device location is null")
                }
            } catch (e: Exception) {
                Log.e("NearbyVibesVM", "Error refreshing nearby users", e)
            }
        }
    }
}

// Factory for creating ViewModel with parameters
class NearbyVibesViewModelFactory(
    private val api: RegistrationServices?,
    private val userId: String?,
    private val locationHelper: LocationHelper?,
    private val initialMood: String?
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NearbyVibesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NearbyVibesViewModel(api, userId, locationHelper,initialMood) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}