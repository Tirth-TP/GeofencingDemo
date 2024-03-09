package com.example.geofencingdemo

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.geofencingdemo.databinding.ActivityMainBinding
import com.example.geofencingdemo.helper.GeofenceHelper
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
import com.google.android.libraries.places.widget.AutocompleteSupportFragment

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMainBinding
    private lateinit var autoCompleteFragment: AutocompleteSupportFragment
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
        private const val BACKGROUND_LOCATION_ACCESS_REQUEST_CODE = 1002
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        } else {
            requestBackgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }
}
