package edu.gwu.androidtweets

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import org.jetbrains.anko.doAsync

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var locationProvider: FusedLocationProviderClient

    private lateinit var currentLocation: ImageButton

    private lateinit var confirm: Button

    private lateinit var mMap: GoogleMap

    private var currentAddress: Address? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        locationProvider = LocationServices.getFusedLocationProviderClient(this)

        currentLocation = findViewById(R.id.current_location)
        currentLocation.setOnClickListener {
            checkPermissions()
        }

        confirm = findViewById(R.id.confirm)
        confirm.setOnClickListener {
            if (currentAddress != null) {
                val tweetsIntent = Intent(this, TweetsActivity::class.java)
                tweetsIntent.putExtra("address", currentAddress)
                startActivity(tweetsIntent)
            }
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private val locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            // We only want a single update, so unregister (e.g. turn the GPS off) after we get one
            locationProvider.removeLocationUpdates(this)

            val mostRecent = locationResult.lastLocation
            val latitude = mostRecent.latitude
            val longitude = mostRecent.longitude
            val latlng = LatLng(latitude, longitude)
            doGeocoding(latlng)
        }
    }

    private fun useCurrentLocation() {
        // We could use .lastLocation here since our app only needs a rough location to function
        // But, we can also choose to request a fresh update using requestLocationUpdates

        // Use the default parameters for a LocationRequest, but generally you can customize
        // how often you get location results and how accurate you want them to be
        val locationRequest = LocationRequest.create()

        locationProvider.requestLocationUpdates(
            locationRequest,
            locationCallback,
            null
        )
    }

    private fun checkPermissions() {
        val permissionState: Int = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        if (permissionState == PackageManager.PERMISSION_GRANTED) {
            // Permission granted - we can now access the GPS
            useCurrentLocation()
        } else {
            // Permission has not been granted
            // Ask for the permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                200
            )
        }
    }

    // Called when the user either grants or denies the permission prompt
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Check if this is the result of our GPS permission prompt
        if (requestCode == 200) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // User granted GPS permission, so we can get the current location
                useCurrentLocation()
            } else {
                // User denied the GPS permission (or we had an automatic denial by the system)
                // In this case, this is *fine* since this is not a critical permission, so we'll just show a Toast
                // See Lecture 8 for other ways you can handle permission denial
                Toast.makeText(
                    this,
                    "Permission denied: cannot use current location",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap.setOnMapLongClickListener { latLng: LatLng ->
            Log.d("MapsActivity", "Long press at ${latLng.latitude}, ${latLng.longitude}")

            doGeocoding(latLng)
        }
    }

    fun doGeocoding(latLng: LatLng) {
        // The Geocoder can potentially take a long time to fetch results and you can risk
        // freezing the UI Thread if you invoke it on the UI Thread, so we do it on the background.
        doAsync {
            val geocoder = Geocoder(this@MapsActivity)

            // The Geocoder throws exceptions if there's a connectivity issue, so wrap it in a try-catch
            val results: List<Address> = try {
                geocoder.getFromLocation(
                    latLng.latitude,
                    latLng.longitude,
                    10
                )
            } catch (exception: Exception) {
                exception.printStackTrace()
                Log.e("MapsActivity", "Failed to retrieve results: $exception")
                listOf<Address>()
            }

            // We'll just take the first result we get back
            if (results.isNotEmpty()) {
                Log.d("MapsActivity", "Received ${results.size} results")
                val firstResult: Address = results.first()
                val streetAddress = firstResult.getAddressLine(0)

                currentAddress = firstResult

                // Switch back to the UI Thread (required to update the UI)
                runOnUiThread {
                    val marker = MarkerOptions().position(latLng).title(streetAddress)
                    mMap.clear()
                    mMap.addMarker(marker)
                    updateConfirmButton(firstResult)
                }
            }
        }
    }

    /**
     * Flip our Confirm button from red to green, show the check icon, and display the address.
     */
    private fun updateConfirmButton(address: Address) {
        confirm.text = address.getAddressLine(0)

        val greenColor = ContextCompat.getColor(this, R.color.colorPrimary)
        val checkImage = ContextCompat.getDrawable(this, R.drawable.ic_check_white)

        confirm.setBackgroundColor(greenColor)
        confirm.setCompoundDrawablesRelativeWithIntrinsicBounds(checkImage, null, null, null)
    }
}
