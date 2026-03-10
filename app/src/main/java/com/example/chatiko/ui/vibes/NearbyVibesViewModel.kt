package com.example.chatiko.ui.vibes

import android.location.Location
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class NearbyVibesViewModel : ViewModel() {

    private val myLat = 1.3521
    private val myLng = 103.8198

    // Mood Filter
    private val _selectedMood = MutableStateFlow("Happy")
    val selectedMood: StateFlow<String> = _selectedMood

    val moods = listOf("Happy", "Sad", "Stressed", "Vibing", "Bored", "Working", "Idle", "Normal")

    // Dummy users
    private val allUsers = listOf(
        NearbyVibeUser("1","User_7af","Happy","☕",1.3530,103.8205,true,Color(0xFF8B4513)),
//        NearbyVibeUser("2","User_847","Coffee","☕",1.3510,103.8150,true,Color(0xFF6F4E37)),
//        NearbyVibeUser("3","User_192","Study","📚",1.3580,103.8300,false,Color(0xFF4682B4)),
//        NearbyVibeUser("4","User_k21","Gaming","🎮",1.3450,103.8100,true,Color(0xFF32CD32)),
//        NearbyVibeUser("5","User_m90","Chill","🏖️",1.3600,103.8250,true,Color(0xFF40E0D0)),
//        NearbyVibeUser("6","User_v04","Coffee","☕",1.3550,103.8220,false,Color(0xFFD2691E)),
//        NearbyVibeUser("7","User_x55","Study","📚",1.3480,103.8180,true,Color(0xFF4169E1)),
//        NearbyVibeUser("8","User_p88","Gaming","🎮",1.3505,103.8210,true,Color(0xFF228B22)),
//        NearbyVibeUser("9","User_q12","Chill","🧘",1.3540,103.8190,false,Color(0xFF87CEEB)),
//        NearbyVibeUser("10","User_z33","Coffee","☕",1.3620,103.8400,true,Color(0xFF5D4037))
    )

    // Filtered Users
    val filteredUsers: StateFlow<List<NearbyVibeUser>> =
        selectedMood.map { mood ->

            allUsers
                .filter { it.mood == mood }
                .sortedBy { user ->

                    val results = FloatArray(1)

                    Location.distanceBetween(
                        myLat,
                        myLng,
                        user.latitude,
                        user.longitude,
                        results
                    )

                    results[0]
                }

        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    fun selectMood(mood: String) {
        _selectedMood.value = mood
    }
}