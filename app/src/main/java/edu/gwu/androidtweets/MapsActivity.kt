package edu.gwu.androidtweets

import android.content.Intent
import android.location.Address
import android.location.Geocoder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import org.jetbrains.anko.doAsync

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var confirm: Button

    private lateinit var mMap: GoogleMap

    private var currentAddress: Address? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

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

            mMap.clear()

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
                } catch(exception: Exception) {
                    exception.printStackTrace()
                    Log.e("MapsActivity", "Failed to retrieve results: $exception")
                    listOf<Address>()
                }

                if (results.isNotEmpty()) {
                    Log.d("MapsActivity", "Received ${results.size} results")
                    val firstResult: Address = results.first()
                    val streetAddress = firstResult.getAddressLine(0)

                    currentAddress = firstResult

                    runOnUiThread {
                        val marker = MarkerOptions().position(latLng).title(streetAddress)
                        mMap.addMarker(marker)
                    }
                }
            }
        }
    }
}
