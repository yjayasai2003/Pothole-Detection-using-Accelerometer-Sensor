package com.example.potholedetection
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.*
import java.util.*

class home : Fragment(), OnMapReadyCallback, TextToSpeech.OnInitListener {

    private lateinit var googleMap: GoogleMap
    private lateinit var databaseReference: DatabaseReference
    private lateinit var mapFragment: SupportMapFragment
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val andhraPradeshLocation = LatLng(15.9129, 79.7400) // Latitude and Longitude of Andhra Pradesh
    private lateinit var tts: TextToSpeech

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        mapFragment = childFragmentManager.findFragmentById(R.id.mapView) as SupportMapFragment
        mapFragment.getMapAsync(this)

        databaseReference = FirebaseDatabase.getInstance().getReference("sensor_data")

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        tts = TextToSpeech(requireContext(), this)

        return view
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Check if location permission is granted
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request location permission if not granted
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        // Enable My Location button and set up the listener
        googleMap.isMyLocationEnabled = true
        // Zoom to Andhra Pradesh
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(andhraPradeshLocation, 7f))
        // Add marker for current location
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val currentLatLng = LatLng(location.latitude, location.longitude)
                googleMap.addMarker(
                    MarkerOptions()
                        .position(currentLatLng)
                        .title("Current Location")
                )

                // Check for nearby potholes
                checkForNearbyPotholes(location)
            }
        }

        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                googleMap.clear() // Clear existing markers
                for (data in dataSnapshot.children) {
                    val latitude = data.child("latitude").getValue(Double::class.java)
                    val longitude = data.child("longitude").getValue(Double::class.java)
                    val pcount = data.child("positive").getValue(Int::class.java)
                    val count = data.child("count").getValue(Int::class.java)
                    val div = pcount!! / count!!
                    if (div > 0.75) {
                        latitude?.let { lat ->
                            longitude?.let { long ->
                                val location = LatLng(lat, long)
                                googleMap.addMarker(
                                    MarkerOptions().position(location).title("Pothole")
                                )
                            }
                        }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle database error
            }
        })
    }

    private fun checkForNearbyPotholes(currentLocation: Location) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("sensor_data")

        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (data in dataSnapshot.children) {
                    val latitude = data.child("latitude").getValue(Double::class.java)
                    val longitude = data.child("longitude").getValue(Double::class.java)
                    latitude?.let { lat ->
                        longitude?.let { long ->
                            val potholeLocation = Location("").apply {
                                this.latitude = lat
                                this.longitude = long
                            }
                            val distance = currentLocation.distanceTo(potholeLocation)
                            if (distance < 20) {
                                speak("Warning: You will hit a Pothole in less than 20 meters ")
                            }
                        }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle database error
            }
        })
    }

    private fun speak(text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        } else {
            @Suppress("DEPRECATION")
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Language data is missing or the language is not supported
                // Handle accordingly, for example, show an error message
            }
        } else {
            // Initialization failed
            // Handle accordingly, for example, show an error message
        }
    }
    override fun onDestroy() {
        // Shutdown TTS engine when the fragment is destroyed to release resources
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}
