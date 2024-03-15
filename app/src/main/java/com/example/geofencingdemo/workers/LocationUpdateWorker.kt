package com.example.geofencingdemo.workers

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters

/**
 * Created by Tirth Patel.
 */
class LocationUpdateWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        // Get the user ID from input data
        val userId = inputData.getString("userId")

        if (userId != null) {
            // Perform location updates and store in the database
            performLocationUpdates(userId)
        } else {
            Log.e("TAG", "User ID not provided")
        }

        // Indicate that the work is completed successfully
        return Result.success()
    }

    private fun performLocationUpdates(userId: String) {
       Log.e("TAG","performLocationUpdates() --> Working Update Firebase")
    }
}