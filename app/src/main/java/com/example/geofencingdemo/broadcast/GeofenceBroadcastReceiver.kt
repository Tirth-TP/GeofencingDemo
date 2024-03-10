package com.example.geofencingdemo.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.example.geofencingdemo.MainActivity
import com.example.geofencingdemo.helper.NotificationHelper
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "GeofenceBroadcastReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {

        val notificationHelper = NotificationHelper(context)

        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent != null && geofencingEvent.hasError()) {
            Log.d(TAG, "onReceive: Error receiving geofence event...")
            return
        }

        val geoFenceList = geofencingEvent?.triggeringGeofences
        if (geoFenceList != null) {
            for (geofence in geoFenceList) {
                Log.d(TAG, "onReceive: ${geofence.requestId}")
            }
        } else {
            Log.d(TAG, "onReceive: No triggering geofences found")
        }


        val transitionType = geofencingEvent?.geofenceTransition

        Log.e(TAG, "onReceive() --> HERE ON WHEN")
        Log.e(TAG, "onReceive() --> Type $transitionType")
        when (transitionType) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                Log.e(TAG, "onReceive() --> ENTER")
                Toast.makeText(context, "GEOFENCE_TRANSITION_ENTER", Toast.LENGTH_SHORT).show()
                notificationHelper.sendHighPriorityNotification(
                    "GEOFENCE_TRANSITION_ENTER",
                    "",
                    MainActivity::class.java
                )
            }

            Geofence.GEOFENCE_TRANSITION_DWELL -> {
                Toast.makeText(context, "GEOFENCE_TRANSITION_DWELL", Toast.LENGTH_SHORT).show()
                notificationHelper.sendHighPriorityNotification(
                    "GEOFENCE_TRANSITION_DWELL",
                    "",
                    MainActivity::class.java
                )
            }

            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                Toast.makeText(context, "GEOFENCE_TRANSITION_EXIT", Toast.LENGTH_SHORT).show()
                notificationHelper.sendHighPriorityNotification(
                    "GEOFENCE_TRANSITION_EXIT",
                    "",
                    MainActivity::class.java
                )
            }
        }
    }
}