package com.example.chatiko.network

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

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

    private val fusedClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val request = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        5000
    ).build()

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {

        return suspendCancellableCoroutine { cont ->

            fusedClient.lastLocation
                .addOnSuccessListener { location ->

                    if (location != null) {
                        cont.resume(location)
                    } else {

                        fusedClient.getCurrentLocation(
                            Priority.PRIORITY_HIGH_ACCURACY,
                            null
                        ).addOnSuccessListener {
                            cont.resume(it)
                        }.addOnFailureListener {
                            cont.resume(null)
                        }
                    }
                }
                .addOnFailureListener {
                    cont.resume(null)
                }
        }
    }
}