package com.example.geofencingdemo.workers

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.geofencingdemo.MainActivity
import com.example.geofencingdemo.retrieveAndDrawPolyline

/**
 * Created by Tirth Patel.
 */

class PolylineWorker(
    context: Context,
    params: WorkerParameters,
) : Worker(context, params) {
    override fun doWork(): Result {
        val userId = inputData.getString("userId")

        if (userId != null) {
            retrieveAndDrawPolyline(userId)
        } else {
            Log.e(MainActivity.TAG, "User ID not provided")
        }
        return Result.success()
    }
}