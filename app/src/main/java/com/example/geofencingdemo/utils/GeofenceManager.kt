package com.example.geofencingdemo.utils

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import com.example.geofencingdemo.MainActivity
import com.example.geofencingdemo.broadcast.GeofenceBroadcastReceiver
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener

class GeofenceManager(private val context: Context) {
    private val geofencingClient: GeofencingClient =
        LocationServices.getGeofencingClient(context)

    companion object {
        const val TAG = "GeofenceManager"
    }

    fun addGeofence(
        geofenceId: String,
        latLng: LatLng,
        radius: Float,
        onSuccessListener: OnSuccessListener<Void>,
        onFailureListener: OnFailureListener
    ) {
        val geofence = createGeofence(geofenceId, latLng, radius)
        val geofencingRequest = createGeofencingRequest(geofence)
        val pendingIntent = getPendingIntent()

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            geofencingClient.addGeofences(geofencingRequest, pendingIntent)
                .addOnSuccessListener(onSuccessListener)
                .addOnFailureListener(onFailureListener)
        } else {
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                MainActivity.PERMISSION_REQUEST_CODE
            )
        }

    }

    private fun createGeofence(geofenceId: String, latLng: LatLng, radius: Float): Geofence {
        return Geofence.Builder()
            .setRequestId(geofenceId)
            .setCircularRegion(latLng.latitude, latLng.longitude, radius)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(
                Geofence.GEOFENCE_TRANSITION_ENTER or
                        Geofence.GEOFENCE_TRANSITION_DWELL or
                        Geofence.GEOFENCE_TRANSITION_EXIT
            )
            .setLoiteringDelay(10000) // 10 seconds
            .build()
    }

    private fun createGeofencingRequest(geofence: Geofence): GeofencingRequest {
        return GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()
    }

    private val createGeofencePendingIntent: PendingIntent by lazy {
        val intent = Intent("com.example.geofencingdemo.ACTION_GEOFENCE_EVENT")
        intent.setClass(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            2607,
            intent,
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                PendingIntent.FLAG_CANCEL_CURRENT
            } else {
                PendingIntent.FLAG_MUTABLE
            }
        )
    }

    private fun getPendingIntent(): PendingIntent {
        return createGeofencePendingIntent
    }

    fun getErrorString(e: Exception): String {
        if (e is com.google.android.gms.common.api.ApiException) {
            return when (e.statusCode) {
                GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE -> "GEOFENCE_NOT_AVAILABLE"
                GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES -> "GEOFENCE_TOO_MANY_GEOFENCES"
                GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS -> "GEOFENCE_TOO_MANY_PENDING_INTENTS"
                else -> "Geofencing error: " + e.localizedMessage
            }
        }
        return e.localizedMessage ?: ""
    }
}
