package com.example.geofencingdemo

import android.app.Application
import com.example.geofencingdemo.utils.UserUtils
import com.google.firebase.database.FirebaseDatabase

/**
 * Created by Tirth Patel.
 */
class GeofenceDemoApp : Application() {
    override fun onCreate() {
        super.onCreate()

        //Initialize firebase database
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)

        //Initialize user Id
        UserUtils.init(applicationContext)
    }
}

