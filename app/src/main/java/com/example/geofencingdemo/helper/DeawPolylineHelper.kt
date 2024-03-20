package com.example.geofencingdemo.helper

import android.util.Log
import com.example.geofencingdemo.MainActivity
import com.google.android.gms.maps.model.Dot
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PatternItem
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.RoundCap
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

/**
 * Created by Tirth Patel.
 */

// Polyline pattern
private val dot: PatternItem = Dot()
private val gap: PatternItem = Gap(25f)

private val patternPolyDot = listOf(gap, dot)

private var previousLocation: LatLng? = null
private var isPreviousLocationAvailable: Boolean = false

fun retrieveAndDrawPolyline(
    userId: String?
) {
    Log.e("Constant Tag", "retrieveAndDrawPolyline() - > called")
    //locationList.clear()
    // Retrieve data from Firebase
    val locationRef =
        FirebaseDatabase.getInstance().getReference("user_locations").child(userId!!)
    locationRef.limitToLast(1).addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            if (dataSnapshot.exists()) {
                // Retrieve the latest location data
                val latestLocation = dataSnapshot.children.first()
                val latitude = latestLocation.child("latitude").value as Double
                val longitude = latestLocation.child("longitude").value as Double
                val latLng = LatLng(latitude, longitude)

                if (!isPreviousLocationAvailable) {
                    previousLocation = latLng
                    isPreviousLocationAvailable = true
                }

                drawPolyline(latLng)

            } else {
                Log.e(MainActivity.TAG, "No location data found")
            }
        }

        override fun onCancelled(error: DatabaseError) {
            Log.e(MainActivity.TAG, "Failed to read value.", error.toException())
        }
    })
}

fun drawPolyline(latLng: LatLng) {
    MainActivity.mGoogleMap.let { map ->
        Log.e(MainActivity.TAG, "drawPolyline: Drawing $latLng")
        // Add the polyline to the map
        map.addPolyline(
            PolylineOptions()
                .clickable(true)
                .add(previousLocation, latLng)
                .endCap(RoundCap())
                .jointType(JointType.ROUND)
                .width(12f)
                .pattern(patternPolyDot)
        )
    }
    // Update the previous location to the current location
    previousLocation = latLng
}
