package com.example.chatiko.network

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
// SocketManager.kt

import io.socket.client.IO
import io.socket.client.Socket
import java.net.URISyntaxException

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

object SocketManager {
    private var mSocket: Socket? = null

    fun init(userId: String) {
        if (mSocket == null) {
            try {
                val opts = IO.Options()
                opts.forceNew = true
                mSocket = IO.socket("http://YOUR_SERVER_IP:3000", opts)
                mSocket?.connect()
                mSocket?.emit("join", userId)
            } catch (e: URISyntaxException) {
                e.printStackTrace()
            }
        }
    }

    fun getSocket(): Socket? = mSocket

    fun disconnect() {
        mSocket?.disconnect()
        mSocket = null
    }
}