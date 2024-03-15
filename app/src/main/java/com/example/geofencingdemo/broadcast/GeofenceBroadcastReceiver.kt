package com.example.geofencingdemo.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.geofencingdemo.helper.GeofenceWorker
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "Geofence"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive: Received ")

        if (intent.action == "com.example.geofencingdemo.ACTION_GEOFENCE_EVENT") {

            val geofencingEvent = GeofencingEvent.fromIntent(intent)

            if (geofencingEvent != null && geofencingEvent.hasError()) {
                Log.d(TAG, "onReceive: Error receiving geofence event...")
                return
            }

            val transitionType = geofencingEvent?.geofenceTransition ?: -1

            if (transitionType == Geofence.GEOFENCE_TRANSITION_ENTER ||
                transitionType == Geofence.GEOFENCE_TRANSITION_DWELL ||
                transitionType == Geofence.GEOFENCE_TRANSITION_EXIT
            ) {
                val inputData = Data.Builder()
                    .putInt("transitionType", transitionType)
                    .build()

                val oneTimeWorkRequest = OneTimeWorkRequestBuilder<GeofenceWorker>()
                    .setInputData(inputData)
                    .build()

                WorkManager.getInstance(context).enqueue(oneTimeWorkRequest)

            } else {
                Log.d(TAG, "onReceive: Triggering geofence not found")
            }
        } else {
            Log.d(TAG, "onReceive: Received intent with unexpected action: ${intent.action}")

        }
    }
}