package com.example.geofencingdemo.utils

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

/**
 * Created by Tirth Patel.
 */
object UserUtils {
    private const val PREF_USER_ID = "user_id"
    private lateinit var sharedPreferences: SharedPreferences

    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences("UserIdPref", Context.MODE_PRIVATE)
    }

    fun getUserId(): String {
        var userId = sharedPreferences.getString(PREF_USER_ID, "")
        if (userId.isNullOrEmpty()) {
            userId = generateUserId()
            sharedPreferences.edit().putString(PREF_USER_ID, userId).apply()
        }
        return userId
    }

    private fun generateUserId(): String {
        return UUID.randomUUID().toString()
    }
}