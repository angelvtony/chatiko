package com.example.chatiko.network

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine

//class LocationHelper(context: Context) {
//
//    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
//
//    @SuppressLint("MissingPermission")
//    suspend fun getCurrentLocation(): Location? = suspendCancellableCoroutine { cont ->
//        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
//            cont.resume(location) {}
//        }.addOnFailureListener { e ->
//            cont.resume(null) {}
//        }
//    }
//}

class LocationHelper(context: Context) {

    private val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? = suspendCancellableCoroutine { cont ->

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->

                if (location != null) {
                    cont.resume(location) {}
                } else {

                    fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        null
                    ).addOnSuccessListener { freshLocation ->
                        cont.resume(freshLocation) {}
                    }.addOnFailureListener {
                        cont.resume(null) {}
                    }

                }

            }
            .addOnFailureListener {
                cont.resume(null) {}
            }
    }
}