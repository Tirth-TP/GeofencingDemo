package com.example.geofencingdemo.model

import com.google.android.gms.maps.model.LatLng

/**
 * Created by Tirth Patel.
 */
data class LocationEvent(
    val latLng: LatLng,
    val speed: Int
)
