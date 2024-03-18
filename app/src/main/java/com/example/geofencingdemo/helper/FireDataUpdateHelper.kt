package com.example.geofencingdemo.helper

import android.location.Location
import android.util.Log
import com.example.geofencingdemo.MainActivity
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ServerValue

/**
 * Created by Tirth Patel.
 */


fun storeLocation(location: Location?, locationRef: DatabaseReference) {

    val userId = MainActivity.userKey

    val locationKey = locationRef.child(userId).push().key
    locationKey?.let {
        val locationData = HashMap<String, Any>()
        locationData["latitude"] = location!!.latitude
        locationData["longitude"] = location.longitude
        // You can add more data such as timestamp
        locationData["timestamp"] = ServerValue.TIMESTAMP
        // Set the location data in the database under a unique key
        locationRef.child(userId).child(locationKey).setValue(locationData)
            .addOnSuccessListener {
                Log.d(MainActivity.TAG, "Location stored successfully")
            }
            .addOnFailureListener { e ->
                Log.e(MainActivity.TAG, "Failed to store location: $e")
            }
    }
}