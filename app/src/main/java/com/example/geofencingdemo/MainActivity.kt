package com.example.geofencingdemo

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
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
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.Dot
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PatternItem
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.RoundCap
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import java.util.Timer
import java.util.TimerTask


class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    // Declare variables for location updates
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    // Firebase
    private lateinit var database: FirebaseDatabase
    private lateinit var locationRef: DatabaseReference

    private val locationList = ArrayList<LatLng>()

    private val TIMER_INTERVAL: Long = 2000 // 30 seconds
    private var timer: Timer? = null

    // Polyline pattern
    private val dot: PatternItem = Dot()
    private val gap: PatternItem = Gap(25f)
    private val patternPolyDot = listOf(gap, dot)

    private lateinit var binding: ActivityMainBinding
    private var circle: Circle? = null

    private lateinit var geofencingClient: GeofencingClient
    private lateinit var geofenceHelper: GeofenceHelper
    val GEOFENCE_ID = "my_geofence_1"
    lateinit var latLndBackgroundPermission: LatLng


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
                // Permission is denied, handle it accordingly
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    @RequiresApi(Build.VERSION_CODES.Q)
    private val requestBackgroundLocationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                requestBackgroundPermission(latLndBackgroundPermission)
            } else {
                // Permission is denied, handle it accordingly
                Toast.makeText(this, "Background location permission denied", Toast.LENGTH_SHORT)
                    .show()
            }
        }

    private var mGoogleMap: GoogleMap? = null

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
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

        // Initialize Firebase
        database = FirebaseDatabase.getInstance()
        locationRef = database.getReference("user_locations")

        // Initialize fused location client and location callback
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                // Store the user's location in the database
                storeLocation(locationResult.lastLocation)
            }
        }


        val apiKey = resources.getString(R.string.google_maps_key)

        geofencingClient = LocationServices.getGeofencingClient(this)
        geofenceHelper = GeofenceHelper(this)

        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.map_view) as SupportMapFragment
        mapFragment.getMapAsync(this)


    }

    override fun onMapReady(googlwMap: GoogleMap) {
        mGoogleMap = googlwMap

        val location = LatLng(23.597969, 72.969818)

        val cameraPosition = CameraPosition.Builder()
            .target(location)
            .zoom(14f) // Adjust the zoom level as needed
            .build()

        mGoogleMap?.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))

//        retrieveAndDrawPolyline()
        startLocationUpdates()

        enableUserLocation()

        mGoogleMap?.setOnMapLongClickListener {
            latLndBackgroundPermission = it
            if (Build.VERSION.SDK_INT >= 29) {
                //We need background permission
                requestBackgroundPermission(it)
            } else {
                tryAddingGeofence(it)
            }
        }
    }


    private fun storeLocation(location: Location?) {
        // Generate a unique key for the location data
        val locationKey = locationRef.push().key
        locationKey?.let {
            val locationData = HashMap<String, Any>()
            locationData["latitude"] = location!!.latitude
            locationData["longitude"] = location.longitude
            // You can add more data such as timestamp
            locationData["timestamp"] = ServerValue.TIMESTAMP
            // Set the location data in the database under a unique key
            locationRef.child(locationKey).setValue(locationData)
                .addOnSuccessListener {
                    Log.d("TAG", "Location stored successfully")
                }
                .addOnFailureListener { e ->
                    Log.e("TAG", "Failed to store location: $e")
                }
        }
    }

    private fun requestLocationUpdates() {
        /*    val locationRequest = LocationRequest.create().apply {
                interval = 2000 // 30 seconds
                fastestInterval = 2000 // 15 seconds
                priority = Priority.PRIORITY_HIGH_ACCURACY
            }*/
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(2000)
            .setMaxUpdateDelayMillis(2000)
            .build()
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    override fun onResume() {
        super.onResume()
        // Request location updates when the activity is resumed
        requestLocationUpdates()
        stopLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        // Remove location updates when the activity is paused
        fusedLocationClient.removeLocationUpdates(locationCallback)
        stopLocationUpdates()
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
        mGoogleMap?.clear()
        addCircle(latLng)
        addMarker(latLng)
        addGeofence(latLng, 1000.0f)
    }

    private fun addGeofence(latLng: LatLng, radius: Float) {
        val geofence = geofenceHelper.getGeofencing(
            GEOFENCE_ID,
            latLng,
            radius,
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
        mGoogleMap?.addMarker(
            MarkerOptions()
                .position(position)
                .draggable(true)
        )
    }

    private fun addCircle(center: LatLng) {
        circle?.remove()
        circle = mGoogleMap?.addCircle(
            CircleOptions()
                .center(center)
                .radius(1000.0)
                .strokeWidth(8f)
                .strokeColor(Color.parseColor("#FF0000"))
                .fillColor(ContextCompat.getColor(this, R.color.red))
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
            mGoogleMap?.isMyLocationEnabled = true

            //Set Icon position
            mGoogleMap?.setPadding(0, 90, 0, 0)
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

    // Function to start fetching location data periodically
    private fun startLocationUpdates() {
        if (timer == null) {
            timer = Timer()
            timer?.scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    Log.e("tag timer", "run() --> yinmrtr")
                    retrieveAndDrawPolyline()
                }
            }, 0, TIMER_INTERVAL)
        }
    }

    private fun retrieveAndDrawPolyline() {
        locationList.clear()
        // Retrieve data from Firebase
        locationRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Iterate through the dataSnapshot to retrieve location data
                for (snapshot in dataSnapshot.children) {
                    val latitude = snapshot.child("latitude").value as Double
                    val longitude = snapshot.child("longitude").value as Double
                    val latLng = LatLng(latitude, longitude)
                    locationList.add(latLng)
                }
                // Draw polyline on the map using the retrieved location data
                drawPolyline()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("TAG", "Failed to read value.", databaseError.toException())
            }
        })
    }

    private fun drawPolyline() {
        mGoogleMap?.clear()
        Log.e("TAG", "drawPolyline: $locationList")
        mGoogleMap?.let { map ->
            if (locationList.isNotEmpty()) {
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
                Log.e("TAG", "Location list is empty")
            }
        }
    }

    private fun stopLocationUpdates() {
        timer?.cancel()
        timer = null
    }

}
