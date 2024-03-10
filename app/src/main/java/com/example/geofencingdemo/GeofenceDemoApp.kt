package com.example.geofencingdemo

import android.app.Application
import com.google.firebase.database.FirebaseDatabase

/**
 * Created by Tirth Patel.
 */
class GeofenceDemoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
    }
}

