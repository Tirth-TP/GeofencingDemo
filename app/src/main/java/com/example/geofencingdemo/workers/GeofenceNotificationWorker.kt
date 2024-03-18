package com.example.geofencingdemo.workers

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.geofencingdemo.MainActivity
import com.example.geofencingdemo.helper.NotificationHelper
import com.google.android.gms.location.Geofence

/**
 * Created by Tirth Patel.
 */
class GeofenceNotificationWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {

        val transitionType = inputData.getInt("transitionType", -1)

        val notificationHelper = NotificationHelper(applicationContext)
        val title: String
        val message: String

        when (transitionType) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                title = "Entered geofence"
                message = "You've entered a geofence region"
            }

            Geofence.GEOFENCE_TRANSITION_DWELL -> {
                title = "Dwelling in geofence"
                message = "You're dwelling in a geofence region"
            }

            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                title = "Exited geofence"
                message = "You've exited a geofence region"
            }

            else -> {
                title = "Unknown geofence transition"
                message = "Unknown geofence transition occurred"
            }
        }

        notificationHelper.sendHighPriorityNotification(
            title, message, MainActivity::class.java
        )

        return Result.success()
    }

}