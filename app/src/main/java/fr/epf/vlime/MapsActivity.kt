package fr.epf.vlime


import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.maps.android.clustering.ClusterManager
import fr.epf.vlime.API.StationApi
import fr.epf.vlime.model.Station
import fr.epf.vlime.model.StationAdapter

const val TAG = "MapsActivity"

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private var stations: MutableList<Station> = mutableListOf()
    private lateinit var clusterManager: ClusterManager<Station>
    private lateinit var binding: ActivityMapsBinding

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

        /*val recyclerview = findViewById<RecyclerView>(R.id.recycler_view)
        recyclerview.layoutManager = LinearLayoutManager(this)
        val data = ArrayList<Station>()
        val adapter = StationAdapter(data)
        recyclerview.adapter = adapter*/

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

            val favoris = findViewById<Button>(R.id.info_favoris)

            /*favoris.setOnClickListener {
                data.add(item)
            }*/

            close.setOnClickListener {
                cardview.isVisible = false
            }

            false
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
}

