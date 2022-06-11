package fr.epf.vlime

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import fr.epf.vlime.databinding.ActivityMapsBinding
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import android.util.Log
import android.view.*
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.maps.android.clustering.ClusterManager
import fr.epf.vlime.API.StationApi
import fr.epf.vlime.model.Station

const val TAG = "MapsActivity"
var favoriteList: List<Station> = listOf()

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private var stations: MutableList<Station> = mutableListOf()
    private lateinit var clusterManager: ClusterManager<Station>
    private lateinit var binding: ActivityMapsBinding
    private var isLocationPermissionGranted = false
    private lateinit var currentLocation: Location
    private lateinit var mapFragment: SupportMapFragment
    private var currentMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.title = "V'Lime"

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

    }

    override fun onMapReady(mMap: GoogleMap) {

        Cluster(mMap)

        clusterManager.setOnClusterItemInfoWindowClickListener { item ->

            val nameText = findViewById<TextView>(R.id.info_name)
            nameText.text = item.name

            val velibText = findViewById<TextView>(R.id.info_velib)
            velibText.text = item.bikes_available.toString()

            val evelibText = findViewById<TextView>(R.id.info_evelib)
            evelibText.text = item.ebikes_available.toString()

            val docksText = findViewById<TextView>(R.id.info_places)
            docksText.text = item.num_docks_available.toString()

            val cardview = findViewById<CardView>(R.id.card_view)
            cardview.isVisible = true

            val close = findViewById<ImageView>(R.id.info_close)


            close.setOnClickListener {
                cardview.isVisible = false
            }

            false
        }

        val geolocationButton = findViewById<FloatingActionButton>(R.id.geolocation_button)
        geolocationButton.imageTintList = ColorStateList.valueOf(Color.rgb(255, 255, 255))
        geolocationButton.setOnClickListener {
            if (isLocationEnabled()) {
                getCurrentLocation()
            }else {
                Toast.makeText(this, "Activer la localisation", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
        }

    }

    private fun Cluster(it: GoogleMap) {
        it.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(48.86, 2.35), 10f))

        clusterManager = ClusterManager(this, it)

        it.setOnCameraIdleListener(clusterManager)
        it.setOnMarkerClickListener(clusterManager)

        callApi()

        clusterManager.addItems(stations)
        clusterManager.cluster()
    }

    private fun callApi() {
        val httpLoggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(httpLoggingInterceptor)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("http://94.247.183.221:8078/")
            .addConverterFactory(MoshiConverterFactory.create())
            .client(client)
            .build()

        val service = retrofit.create(StationApi::class.java)

        runBlocking {
            val resultStation = service.getStations()
            Log.d(TAG, "synchroApi: $resultStation")

            resultStation.map {
                stations.remove(it)
                stations.add(it)
            }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.nav_menu,menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val intent = Intent(this, FavoriteActivity::class.java)
        when(item.itemId){
            R.id.nav_favorite -> startActivity(intent)
            androidx.appcompat.R.id.home -> this.finish()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER)
    }

    private fun getCurrentLocation() {
        val fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(this)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            isLocationPermissionGranted = false
            return
        }
        fusedLocationProviderClient.lastLocation.addOnCompleteListener {
            val location: Location? = it.result
            if (location == null) {
                Toast.makeText(this, "Veuillez activer votre localisation", Toast.LENGTH_SHORT).show()

            }else{
                currentLocation = location
                moveCameraToLocation(currentLocation)
            }
        }
    }

    private fun moveCameraToLocation(currentLocation: Location) {

        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(
            LatLng(
                currentLocation.latitude,
                currentLocation.longitude
            ), 17f
        )
        val markerOption = MarkerOptions()
            .position(LatLng(currentLocation.latitude, currentLocation.longitude))
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            .visible(false)


        mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment

        mapFragment.getMapAsync {
            currentMarker?.remove()
            currentMarker = it.addMarker(markerOption)
            currentMarker?.tag = 703
            it.animateCamera(cameraUpdate)
        }
    }


}

