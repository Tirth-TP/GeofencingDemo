package com.example.geofencingdemo

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.geofencingdemo.databinding.ActivityMainBinding
import com.example.geofencingdemo.helper.GeofenceHelper
import com.example.geofencingdemo.helper.retrieveAndDrawPolyline
import com.example.geofencingdemo.model.LocationEvent
import com.example.geofencingdemo.services.LocationForegroundService
import com.example.geofencingdemo.utils.GeofenceManager
import com.example.geofencingdemo.utils.UserUtils
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe


class MainActivity : AppCompatActivity(), OnMapReadyCallback,
    GoogleMap.OnCameraMoveStartedListener {

    private lateinit var binding: ActivityMainBinding
    private var circle: Circle? = null

    private var isMapTouched = false

    private lateinit var geofencingClient: GeofencingClient
    private lateinit var geofenceHelper: GeofenceHelper
    private val GEOFENCE_ID = "my_geofence_1"
    private lateinit var latLndBackgroundPermission: LatLng
    private var locationEventCall: LocationEvent? = null

    private lateinit var geofenceManager: GeofenceManager

    private var service: Intent? = null

    private val requestFineLocationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                enableUserLocation()
            } else {
                // Permission is denied, handle it accordingly
                Toast.makeText(this, "Fine location permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    @RequiresApi(Build.VERSION_CODES.Q)
    private val requestBackgroundLocationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                requestBackgroundPermission(latLndBackgroundPermission)
            } else {
                Toast.makeText(this, "Background location permission denied", Toast.LENGTH_SHORT)
                    .show()
            }
        }


    companion object {
        const val PERMISSION_REQUEST_CODE = 1001
        const val TAG = "TAG"

        lateinit var mGoogleMap: GoogleMap

        //User Key For firebase
        lateinit var userKey: String
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
        }

        // For turn on screen while app is running
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        //Get Unique user key
        userKey = UserUtils.getUserId()

        service = Intent(this, LocationForegroundService::class.java)

        //val apiKey = resources.getString(R.string.google_maps_key)

        setupManagers()

        geofencingClient = LocationServices.getGeofencingClient(this)
        geofenceHelper = GeofenceHelper(this)

        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.map_view) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googlwMap: GoogleMap) {
        mGoogleMap = googlwMap

        mGoogleMap.mapType = GoogleMap.MAP_TYPE_TERRAIN

        enableUserLocation()

        //When user change camera position
        mGoogleMap.setOnCameraMoveStartedListener(this)

        mGoogleMap.setOnMapClickListener {
            isMapTouched = true
            binding.fbResume.visibility = View.VISIBLE
        }
        binding.fbResume.setOnClickListener {
            binding.fbResume.visibility = View.VISIBLE
            isMapTouched = false
        }
        mGoogleMap.setOnMapLongClickListener {
            latLndBackgroundPermission = it
            if (Build.VERSION.SDK_INT >= 29) {
                //We need background permission
                requestBackgroundPermission(it)
            } else {
                tryAddingGeofence(it)
            }
        }
    }

    override fun onCameraMoveStarted(p0: Int) {
        //When user change camera position
        if (p0 == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
            isMapTouched = true
            binding.fbResume.visibility = View.VISIBLE

            binding.fbResume.setIconResource(R.drawable.ic_navigation)
            binding.fbResume.text = null
            binding.fbResume.shrink()
        }
    }

    private fun setupManagers() {
        geofenceManager = GeofenceManager(this)
    }

    @Suppress("DEPRECATION")
    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    // Method to update the map with location
    @SuppressLint("SetTextI18n")
    private fun updateMapWithLocation() {
        if (!isMapTouched) {
            binding.fbResume.icon = null
            binding.fbResume.text = locationEventCall?.speed.toString() + " km/h"

            binding.fbResume.extend()

            locationEventCall?.latLng?.let {
                val locationC = LatLng(it.latitude, it.longitude)
                val cameraPosition = CameraPosition.Builder()
                    .target(locationC)
                    .zoom(16f)
                    .build()

                mGoogleMap.animateCamera(
                    CameraUpdateFactory.newCameraPosition(
                        cameraPosition
                    )
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (!isServiceRunning(LocationForegroundService::class.java) && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startService(service)
        }
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(service)
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
    }

    override fun onPause() {
        super.onPause()
        //Clear screen on flag
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }


    private fun askForNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // Permission is already granted, enable user location

            } else {
                // Permission not granted, request it
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun tryAddingGeofence(latLng: LatLng) {
        mGoogleMap.clear()
        addCircle(latLng)
        addMarker(latLng)
        addGeofence(latLng)
    }

    private fun addGeofence(latLng: LatLng) {
        val geofence = geofenceHelper.getGeofencing(
            GEOFENCE_ID,
            latLng,
            1000.0f,
            Geofence.GEOFENCE_TRANSITION_ENTER or
                    Geofence.GEOFENCE_TRANSITION_DWELL or
                    Geofence.GEOFENCE_TRANSITION_EXIT
        )
        val geofencingRequest = geofenceHelper.getGeofencingRequest(geofence)
        val pendingIntent = geofenceHelper.getPendingIntent()

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            geofencingClient.addGeofences(geofencingRequest, pendingIntent).run {
                addOnSuccessListener {
                    Log.d(GeofenceHelper.TAG, "onSuccess: Geofence Added...")
                }
                addOnFailureListener { e ->
                    val errorMessage = geofenceHelper.getErrorString(e)
                    Log.d(GeofenceHelper.TAG, "onFailure: $errorMessage")
                }
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun addMarker(position: LatLng) {
        mGoogleMap.addMarker(
            MarkerOptions()
                .position(position)
                .draggable(true)
        )
    }

    private fun addCircle(center: LatLng) {
        circle?.remove()
        circle = mGoogleMap.addCircle(
            CircleOptions()
                .center(center)
                .radius(1000.0)
                .strokeWidth(2f)
                .strokeColor(ContextCompat.getColor(this, R.color.blue_border))
                .fillColor(ContextCompat.getColor(this, R.color.blue_fill))
        )
    }

    private fun enableUserLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is already granted, enable user location
            binding.mapOptionsMenu.visibility = View.INVISIBLE
            mGoogleMap.isMyLocationEnabled = true

            //Set Icon position
            mGoogleMap.setPadding(0, 90, 0, 0)
        } else {
            // Permission not granted, request it
            requestFineLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestBackgroundPermission(latLng: LatLng) {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            tryAddingGeofence(latLng)
            //Notification permission after background permission
            askForNotificationPermission()
        } else {
            requestBackgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    // Subscribe to location updates
    @Subscribe
    fun receiveLocationEvent(locationEvent: LocationEvent) {
        locationEventCall = locationEvent
        // Update the map with location
        updateMapWithLocation()
        retrieveAndDrawPolyline(userKey)
    }
}
