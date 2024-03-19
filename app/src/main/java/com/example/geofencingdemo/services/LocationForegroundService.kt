package com.example.geofencingdemo.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.util.Log
import com.example.geofencingdemo.MainActivity
import com.example.geofencingdemo.helper.retrieveAndDrawPolyline
import com.example.geofencingdemo.helper.storeLocation
import com.example.geofencingdemo.model.LocationEvent
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import org.greenrobot.eventbus.EventBus

/**
 * Created by Tirth Patel.
 */
class LocationForegroundService : Service() {
    // Declare variables for location updates
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var locationRequest: LocationRequest? = null

    // Firebase
    private lateinit var database: FirebaseDatabase
    private lateinit var locationRef: DatabaseReference

    private lateinit var location: Location

    private val NOTIFICATION_ID = 123 // Unique ID for the notification

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest =
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).setIntervalMillis(500)
                .build()
        Log.e("ServiceTAG", "onCreate: called service")
        // Initialize Firebase
        database = FirebaseDatabase.getInstance()
        locationRef = database.getReference("user_locations")

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                try {
                    location = locationResult.lastLocation!!
                    Log.e("ServiceTAG", "onCreate->onLocationResult: called service")

                    onNewLocation(location)
                    storeLocation(location, locationRef)

                    //retrieveAndDrawPolyline(MainActivity.userKey)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }

            override fun onLocationAvailability(p0: LocationAvailability) {
                super.onLocationAvailability(p0)
            }
        }
    }

    private fun onNewLocation(latLng: Location) {
        EventBus.getDefault().post(
            LocationEvent(
                latLng = LatLng(latLng.latitude, latLng.longitude)
            )
        )
        startForeground(NOTIFICATION_ID, getNotification())
    }

    @SuppressLint("MissingPermission")
    fun createLocationRequest() {
        try {
            fusedLocationClient?.requestLocationUpdates(
                locationRequest!!, locationCallback!!, null
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient?.removeLocationUpdates(it)
        }
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
    }

    private fun getNotification(): Notification {
        return createNotification(
            this,
            "Foreground Service",
            "Your foreground service is running...",
            MainActivity::class.java
        )
    }

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createLocationRequest()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        removeLocationUpdates()
    }

}