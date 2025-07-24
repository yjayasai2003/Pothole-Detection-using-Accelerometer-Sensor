package com.example.potholedetection

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Animatable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.time.LocalDateTime
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt


class Collect_data : Fragment(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var xTextView: TextView
    private lateinit var yTextView: TextView
    private lateinit var zTextView: TextView
    private lateinit var filename: EditText
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var database: FirebaseDatabase
    private var isRecording = false
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    var locationPair: Pair<Double, Double>? = null
    private var previousLatitude: Double = 0.0
    private var previousLongitude: Double = 0.0
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var flag:Int = 0
    private val latitudeList = mutableListOf<Double>()
    private val longitudeList = mutableListOf<Double>()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_collect_data, container, false)
        xTextView = view.findViewById(R.id.xTextView)
        yTextView = view.findViewById(R.id.yTextView)
        zTextView = view.findViewById(R.id.zTextView)
        filename = view.findViewById(R.id.editText)
        startButton = view.findViewById(R.id.startButton)
        stopButton = view.findViewById(R.id.stopButton)
        database = FirebaseDatabase.getInstance()
        fetchLocationPointsFromFirebase()
        startButton.setOnClickListener {
            isRecording = true
            Toast.makeText(requireContext(), "Data Recording Started", Toast.LENGTH_SHORT).show()
            registerSensor()
            startGifAnimation()
        }
        stopButton.setOnClickListener {
            isRecording = false
            Toast.makeText(requireContext(), "Data Recording Stopped", Toast.LENGTH_SHORT).show()
            unregisterSensor()
            stopGifAnimation()
        }
        return view
    }
    private fun fetchLocationPointsFromFirebase() {
        val databaseReference = database.getReference("sensor_data")
        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (ds in dataSnapshot.children) {
                    val latitude = ds.child("latitude").getValue(Double::class.java)
                    val longitude = ds.child("longitude").getValue(Double::class.java)
                    latitude?.let { latitudeList.add(it) }
                    longitude?.let { longitudeList.add(it) }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e(ContentValues.TAG, "Error fetching location points from Firebase", databaseError.toException())
            }
        })
    }
    private fun startGifAnimation() {
        val gifImageView = requireView().findViewById<ImageView>(R.id.gifImageView)
        gifImageView.visibility = View.VISIBLE
        val gifDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.data)
        gifImageView.setImageDrawable(gifDrawable)
        (gifDrawable as? Animatable)?.start()
    }

    // Function to stop the GIF animation
    private fun stopGifAnimation() {
        val gifImageView = requireView().findViewById<ImageView>(R.id.gifImageView)
        val gifDrawable = gifImageView.drawable
        (gifDrawable as? Animatable)?.stop()
    }
    private fun registerSensor() {
        sensorManager = requireActivity().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        } else {
            Toast.makeText(requireContext(), "Accelerometer not available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun unregisterSensor() {
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Do nothing for now
    }

    @SuppressLint("SuspiciousIndentation")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let { it ->
            if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                flag = 0
                val x = it.values[0]
                val y = it.values[1]
                val z = it.values[2]
                val amplitude = sqrt(x * x + y * y + z * z)
                xTextView.text = "X Value: $x"
                yTextView.text = "Y Value: $y"
                zTextView.text = "Z Value: $z"
                val loc = getLastLocation()
                val (lat1, long1) = loc ?: Pair(0.0, 0.0)
                val trimmedLat = trimDecimalPlaces(lat1, 5)
                val trimmedLong = trimDecimalPlaces(long1, 5)


                    if (latitudeList.contains(trimmedLat) && longitudeList.contains(trimmedLong))
                    {
                        if (trimmedLat != previousLatitude && trimmedLong != previousLongitude)
                        {
                            val databaseReference = database.getReference("sensor_data")
                            databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(dataSnapshot: DataSnapshot) {
                                    for (ds in dataSnapshot.children) {
                                        val latitude = ds.child("latitude").getValue(Double::class.java)
                                        val longitude = ds.child("longitude").getValue(Double::class.java)
                                        if (latitude == trimmedLat && longitude == trimmedLong) {
                                            val pcount =ds.child("positive").getValue(Double::class.java)
                                            val Count =ds.child("count").getValue(Double::class.java)
                                            if (amplitude > 14) {
                                                val newPositiveCount = pcount?.plus(1)
                                                ds.ref.child("positive").setValue(newPositiveCount)
                                                val newTotalCount = Count?.plus(1)
                                                ds.ref.child("count").setValue(newTotalCount)
                                            } else if (amplitude < 14) {
                                                val newTotalCount = Count?.plus(1)
                                                ds.ref.child("count").setValue(newTotalCount)
                                            }
                                        }

                                    }
                                }

                                override fun onCancelled(databaseError: DatabaseError) {
                                    Log.e(ContentValues.TAG, "Error fetching location points from Firebase", databaseError.toException())
                                }
                            })
                        }

                    }
                    else if (isRecording && amplitude > 14 && trimmedLat != previousLatitude && trimmedLong != previousLongitude) {
                        writeToFirebase(x, y, z, amplitude, trimmedLat, trimmedLong, 1, 1)
                        writeCsvData(filename.text.toString(), x, y, z, amplitude, trimmedLat, trimmedLong)
                        previousLatitude = trimmedLat
                        previousLongitude = trimmedLong

                    }
                    else if( isRecording)
                    {
                        writeCsvData(filename.text.toString(), x, y, z, amplitude, latitude, longitude)
                    }





                    /*if (isRecording && amplitude > 14)
                    {
                        if (lat1 != previousLatitude || long1 != previousLongitude)
                        {
                            writeCsvData(filename.text.toString(), x, y, z, amplitude, lat1, long1)
                            writeToFirebase(x, y, z, amplitude, lat1, long1, 1, 1)
                            previousLatitude = lat1
                            previousLongitude = long1
                        }
                        Toast.makeText(requireContext(), "Amp$lat1,$long1", Toast.LENGTH_SHORT).show()
                    }
                    else if (isRecording)
                    {
                        writeCsvData(filename.text.toString(), x, y, z,amplitude,latitude,longitude)
                    }*/


            }
        }
    }
    private fun trimDecimalPlaces(value: Double, decimalPlaces: Int): Double {
        val factor = 10.0.pow(decimalPlaces)
        return (value * factor).roundToInt() / factor
    }
    private fun checkLocationInFirebase(lat: Double, long: Double, amplitude: Float,x: Float, y: Float, z: Float) {
        val databaseReference = database.getReference("sensor_data")
        val query = databaseReference.orderByChild("latitude").equalTo(lat)
            .orderByChild("longitude").equalTo(long)
            .limitToFirst(1)

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Location data already exists
                    for (ds in dataSnapshot.children) {
                        val positiveCount = ds.child("positive").getValue(Int::class.java) ?: 0
                        val totalCount = ds.child("count").getValue(Int::class.java) ?: 0
                        val newPositiveCount = if (amplitude > 14) positiveCount + 1 else positiveCount
                        val newTotalCount = totalCount + 1
                        ds.ref.child("positive").setValue(newPositiveCount)
                        ds.ref.child("count").setValue(newTotalCount)
                    }
                }
                else {

                    if(amplitude >14 )
                    {
                        writeToFirebase(x, y, z, amplitude, lat, long,1, 1)
                        writeCsvData(filename.text.toString(), x, y, z, amplitude, lat, long)
                    }
                    else{
                        writeCsvData(filename.text.toString(), x, y, z, amplitude, latitude, longitude)
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e(ContentValues.TAG, "Error checking location in Firebase", databaseError.toException())
            }
        })
    }
    private fun checkLocationPresenceInFirebase(  lat1: Double,
        long1: Double,
        amplitude: Float,
        x: Float,
        y: Float,
        z: Float
    ) {
        val databaseReference = database.getReference("sensor_data")

        // Create a query to check if a record with matching latitude and longitude exists
        databaseReference.orderByChild("latitude").equalTo(lat1)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onDataChange(dataSnapshot: DataSnapshot)
                {    var matchingLocationFound = false
                        // Location found, increment "positive" count
                        for (childSnapshot in dataSnapshot.children) {
                            val childLongitude = childSnapshot.child("longitude").getValue(Double::class.java)
                            if (childLongitude != null && Math.abs(childLongitude - long1) < 0.00001)
                            {
                                // Matching location found, update "positive" count
                                val currentPositiveCount = childSnapshot.child("positive").getValue(
                                    Long::class.java
                                )
                                val Count = childSnapshot.child("count").getValue(
                                    Long::class.java
                                )
                                if(amplitude > 14)
                                {
                                    childSnapshot.ref.child("positive")
                                        .setValue(if (currentPositiveCount != null) currentPositiveCount + 1 else 1)

                                    childSnapshot.ref.child("count")
                                        .setValue(if (Count != null) Count + 1 else 1)
                                    Toast.makeText(
                                        requireContext(),
                                        "Positive, count incremented for $lat1,$long1",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    break
                                }
                                else
                                {
                                    childSnapshot.ref.child("count")
                                        .setValue(if (Count != null) Count + 1 else 1)
                                    Toast.makeText(requireContext(),"Positive, count incremented for $lat1,$long1",Toast.LENGTH_SHORT).show()
                                }
                                matchingLocationFound = true
                            }

                        writeCsvData(filename.text.toString(), x, y, z, amplitude, lat1, long1)
                    }
                    if(!matchingLocationFound)
                    {
                        if(amplitude > 14)
                        {    Toast.makeText(requireContext(),"Normally inserted",Toast.LENGTH_SHORT).show()
                            writeToFirebase(x, y, z, amplitude, lat1, long1, 1, 1)
                            writeCsvData(filename.text.toString(), x, y, z, amplitude, lat1, long1)
                        }
                        else
                        {
                            Toast.makeText(requireContext(),"Not inserted",Toast.LENGTH_SHORT).show()
                            writeCsvData(filename.text.toString(), x, y, z, amplitude, latitude, longitude)
                        }
                    }
                }
                override fun onCancelled(databaseError: DatabaseError)
                {
                    Log.w(
                        ContentValues.TAG,
                        "Error checking location in Firebase",
                        databaseError.toException()
                    )
                }
            })
    }
    private fun getLastLocation(): Pair<Double, Double>?
    {
        var lat:Double
        var long:Double
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                location?.let {
                    // Handle location object here
                    lat = location.latitude
                    long = location.longitude
                    locationPair = Pair(lat, long)

                }
            }
            .addOnFailureListener {
                // Handle any errors that occur during the request
                Toast.makeText(requireContext(), "No location", Toast.LENGTH_SHORT).show()
            }
        return locationPair
    }


    private fun writeToFirebase(
        xValue: Float, yValue: Float, zValue: Float, amp: Float,
        lat: Double,
        long: Double, pcount: Int, Count: Int
    ) {
        val databaseReference = database.getReference("sensor_data")
        val timestamp = System.currentTimeMillis()
        val dataMap = hashMapOf(
            "timestamp" to timestamp,
            "xValue" to xValue,
            "yValue" to yValue,
            "zValue" to zValue,
            "amplitude" to amp,
            "latitude" to lat,
            "longitude" to long,
            "positive" to pcount,
            "count" to Count
        )
        databaseReference.push().setValue(dataMap)
            .addOnSuccessListener {
                Log.d(ContentValues.TAG, "Sensor data written to Firebase")
            }
            .addOnFailureListener { e ->
                Log.e(ContentValues.TAG, "Error writing to Firebase", e)
            }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun writeCsvData(fileName: String, xValue: Float, yValue: Float, zValue: Float, amp: Float,lat: Double,long: Double) {
        try {
            val storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val file = File(storageDir, "$fileName.csv")

            // Check if file exists and write header row if needed
            if (!file.exists()) {
                val writer = FileWriter(file, false) // Create new file
                writer.write("Timestamp,X Values,Y Values,Z Values,Amplitude,Latitude,Longitude\n")
                writer.close()
            }

            val timestamp = LocalDateTime.now()
            val writer = FileWriter(file, true)
            writer.write("$timestamp,$xValue,$yValue,$zValue,$amp,$lat,$long\n")
            writer.close()
        } catch (e: IOException) {
            Toast.makeText(requireContext(), "Error writing to file", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isRecording) {
            registerSensor()
        }
    }

    override fun onPause() {
        super.onPause()
        if (isRecording) {
            unregisterSensor()
        }
    }
}
