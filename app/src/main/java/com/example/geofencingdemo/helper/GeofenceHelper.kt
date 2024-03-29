package com.example.geofencingdemo.helper

import android.app.PendingIntent
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import com.example.geofencingdemo.broadcast.GeofenceBroadcastReceiver
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.maps.model.LatLng

/**
 * Created by Tirth Patel.
 */
class GeofenceHelper(base: Context) : ContextWrapper(base) {
    companion object {
        const val TAG = "GeofenceHelper"
    }

    fun getGeofencingRequest(geofence: Geofence): GeofencingRequest {
        return GeofencingRequest.Builder()
            .addGeofence(geofence)
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .build()
    }

    fun getGeofencing(
        id: String,
        latLng: LatLng,
        radius: Float,
        transitionTypes: Int,
    ): Geofence {
        return Geofence.Builder()
            .setCircularRegion(latLng.latitude, latLng.longitude, radius)
            .setRequestId(id)
            .setTransitionTypes(transitionTypes)
            .setLoiteringDelay(5000)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .build()
    }

    private val _pendingIntent: PendingIntent by lazy {
        val intent = Intent("com.example.geofencingdemo.ACTION_GEOFENCE_EVENT")
        intent.setClass(this, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            this,
            2607,
            intent,
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                PendingIntent.FLAG_CANCEL_CURRENT
            } else {
                PendingIntent.FLAG_MUTABLE
            }
        )
    }

    fun getPendingIntent(): PendingIntent {
        return _pendingIntent
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