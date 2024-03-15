package com.example.geofencingdemo

import android.util.Log
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
private val locationList = ArrayList<LatLng>()

fun retrieveAndDrawPolyline(
    userId: String?
) {
    locationList.clear()
    // Retrieve data from Firebase
    val locationRef =
        FirebaseDatabase.getInstance().getReference("user_locations").child(userId!!)
    locationRef.addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            for (snapshot in dataSnapshot.children) {
                val latitude = snapshot.child("latitude").value as Double
                val longitude = snapshot.child("longitude").value as Double
                val latLng = LatLng(latitude, longitude)
                locationList.add(latLng)
            }
            if (locationList.isNotEmpty()) {
                //To prevent from ConcurrentModificationException
                drawPolyline()  //Draw Polyline
            } else {
                Log.e(MainActivity.TAG, "Location list is empty")
            }
        }

        override fun onCancelled(error: DatabaseError) {
            Log.e(MainActivity.TAG, "Failed to read value.", error.toException())
        }

    })
}

fun drawPolyline() {
    MainActivity.mGoogleMap.let { map ->
        if (locationList.isNotEmpty()) {
            Log.e(MainActivity.TAG, "drawPolyline: Drawing")
            // Add the polyline to the map
            map.addPolyline(
                PolylineOptions()
                    .clickable(true)
                    .addAll(locationList)
                    .endCap(RoundCap())
                    .jointType(JointType.ROUND)
                    .width(12f)
                    .pattern(patternPolyDot)
            )
        } else {
            Log.e(MainActivity.TAG, "Location list is empty")
        }
    }
}