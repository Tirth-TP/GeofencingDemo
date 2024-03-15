package com.example.geofencingdemo

import android.Manifest
import android.content.Context
import android.content.Intent
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
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.geofencingdemo.databinding.ActivityMainBinding
import com.example.geofencingdemo.helper.GeofenceHelper
import com.example.geofencingdemo.services.LocationForegroundService
import com.example.geofencingdemo.utils.UserUtils
import com.example.geofencingdemo.workers.LocationUpdateWorker
import com.example.geofencingdemo.workers.PolylineWorker
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
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    //User Key For firebase
    private lateinit var userKey: String

    // Declare variables for location updates
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    // Firebase
    private lateinit var database: FirebaseDatabase
    private lateinit var locationRef: DatabaseReference

    private val TIMER_INTERVAL: Long = 1000 // 1 seconds
    private var timer: Timer? = null


    private lateinit var binding: ActivityMainBinding
    private var circle: Circle? = null

    private lateinit var geofencingClient: GeofencingClient
    private lateinit var geofenceHelper: GeofenceHelper
    private val GEOFENCE_ID = "my_geofence_1"
    private lateinit var latLndBackgroundPermission: LatLng

    private var previousLocationTime: Long = 0

    private var foregroundServiceStarted = false


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


    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
        const val TAG = "TAG"

        lateinit var mGoogleMap: GoogleMap
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

        // Initialize Firebase
        database = FirebaseDatabase.getInstance()
        locationRef = database.getReference("user_locations")

        // Initialize fused location client and location callback
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)

                // Get the latest user location
                val location = locationResult.lastLocation
                if (location != null) {

                    // Move the map's camera to the user's location
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(currentLatLng))

                    // Store the user's location in the database
                    storeLocation(location, userKey)

                    //startLocationUpdates()
                    //drawPolyline()
                    //Log.e(TAG, "onLocationResult: Called")

                    //Start the foreground service
                    // Check if the foreground service hasn't been started yet
                    if (!foregroundServiceStarted) {
                        // Start the foreground service
                        startForegroundService(
                            Intent(
                                applicationContext,
                                LocationForegroundService::class.java
                            )
                        )
                        // Set the flag to indicate that the service has been started
                        foregroundServiceStarted = true
                    }

                    enqueuePolylineWorker(this@MainActivity)
                    enqueueLocationUpdateWorker()
                } else {
                    Log.e(TAG, "onLocationResult: Location is not available")
                }
            }
        }


        //val apiKey = resources.getString(R.string.google_maps_key)

        geofencingClient = LocationServices.getGeofencingClient(this)
        geofenceHelper = GeofenceHelper(this)

        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.map_view) as SupportMapFragment
        mapFragment.getMapAsync(this)


    }

    override fun onMapReady(googlwMap: GoogleMap) {
        mGoogleMap = googlwMap


        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    location?.let {
                        val locationC = LatLng(location.latitude, location.longitude)
                        val cameraPosition = CameraPosition.Builder()
                            .target(locationC)
                            .zoom(14f)
                            .build()

                        mGoogleMap.animateCamera(
                            CameraUpdateFactory.newCameraPosition(
                                cameraPosition
                            )
                        )

                    }
                }
        }

        //startLocationUpdates()

        enableUserLocation()

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


    private fun storeLocation(location: Location?, userId: String) {
        // Calculate the time difference
        val currentTime = System.currentTimeMillis()
        val timeDifference =
            if (previousLocationTime.toInt() != 0) currentTime - previousLocationTime else 0

        previousLocationTime = currentTime

        Log.d(
            TAG,
            "Time difference since last location update: $timeDifference milliseconds"
        )
        // Generate a unique key for the location data
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
                    Log.d(TAG, "Location stored successfully")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to store location: $e")
                }
        }
    }

    private fun requestLocationUpdates() {

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(1000)
            .setMaxUpdateDelayMillis(1000)
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

    // Function to start fetching location data periodically
    private fun startLocationUpdates() {
        if (timer == null) {
            timer = Timer()
            timer?.scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    //retrieveAndDrawPolyline(userKey)
                }
            }, 0, TIMER_INTERVAL)
        }
    }

    /*    private fun retrieveAndDrawPolyline(userId: String) {
            locationList.clear()
            // Retrieve data from Firebase
            locationRef.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    // Iterate through the dataSnapshot to retrieve location data
                    for (snapshot in dataSnapshot.children) {
                        val latitude = snapshot.child("latitude").value as Double
                        val longitude = snapshot.child("longitude").value as Double
                        val latLng = LatLng(latitude, longitude)
                        locationList.add(latLng)
                    }
                    // Draw polyline on the map using the retrieved location data
                    if (locationList.isNotEmpty()) {    //To prevent from ConcurrentModificationException
                        drawPolyline(mGoogleMap, locationList)
                    } else {
                        Log.e(TAG, "Location list is empty")
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e(TAG, "Failed to read value.", databaseError.toException())
                }
            })
        }*/

    @RequiresApi(Build.VERSION_CODES.O)
    private fun enqueuePolylineWorker(context: Context) {
        val workManager = WorkManager.getInstance(context)

        // Build input data containing the user ID
        val inputData = Data.Builder()
            .putString("userId", userKey)
            .build()

        // Define constraints if needed
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Create a periodic request for the location update worker
        val request = OneTimeWorkRequestBuilder<PolylineWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .build()

        // Enqueue the worker with WorkManager
        workManager.enqueue(request)

    }

    private fun enqueueLocationUpdateWorker() {
        val workManager = WorkManager.getInstance(applicationContext)

        // Build input data containing the user ID
        val inputData = Data.Builder()
            .putString("userId", userKey)
            .build()

        // Define constraints if needed
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Define how often the worker should run
        val repeatInterval = 1000L // Repeat every 1000 milliseconds (1 second)

        // Create a periodic request for the location update worker
        val request = PeriodicWorkRequestBuilder<LocationUpdateWorker>(
            repeatInterval, // Repeat interval
            TimeUnit.MILLISECONDS // Time unit
        )
            .setConstraints(constraints) // Apply constraints
            .setInputData(inputData) // Provide input data
            .build()

        // Enqueue the worker with WorkManager
        workManager.enqueue(request)
    }

    private fun stopLocationUpdates() {
        timer?.cancel()
        timer = null
    }

}
