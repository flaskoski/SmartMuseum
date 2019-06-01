package flaskoski.rs.smartmuseum.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import flaskoski.rs.rs_cf_test.recommender.RecommenderBuilder
import flaskoski.rs.smartmuseum.DAO.ItemDAO
import flaskoski.rs.smartmuseum.DAO.RatingDAO
import flaskoski.rs.smartmuseum.util.ParallelRequestsManager
import flaskoski.rs.smartmuseum.R
import flaskoski.rs.smartmuseum.listAdapter.ItemsGridListAdapter
import flaskoski.rs.smartmuseum.model.Item
import flaskoski.rs.smartmuseum.model.Rating
import flaskoski.rs.smartmuseum.recommender.RecommenderManager
import flaskoski.rs.smartmuseum.util.ApplicationProperties
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main_bottom_sheet.*
import java.util.*

class MainActivity : AppCompatActivity(), OnMapReadyCallback, ItemsGridListAdapter.OnShareClickListener {

    private val REQUEST_GET_PREFERENCES: Int = 1
    private val REQUEST_ITEM_RATING_CHANGE: Int = 2
    private val REQUEST_CHECK_LOCATION_SETTINGS: Int = 3

    private val itemsList = ArrayList<Item>()
    private var ratingsList  = HashSet<Rating>()
    private val TAG = "MainActivity"
//    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
//        when (item.itemId) {
//            R.id.navigation_home -> {
//                return@OnNavigationItemSelectedListener true
//            }
//            R.id.navigation_dashboard -> {
//                return@OnNavigationItemSelectedListener true
//            }
//            R.id.navigation_notifications -> {
//              //  message.setText(R.string.title_notifications)
//                return@OnNavigationItemSelectedListener true
//            }
//        }
//        false
//    }

    private lateinit var adapter: ItemsGridListAdapter

    private lateinit var parallelRequestsManager: ParallelRequestsManager

    private lateinit var mMap: GoogleMap
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var mCurrLocationMarker: Marker

    private var requestingLocationUpdates: Boolean = false

    private val REQUESTING_LOCATION_UPDATES_KEY: String = "request_updates_key"
    private var locationRequest: LocationRequest? = null

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, requestingLocationUpdates)
        super.onSaveInstanceState(outState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        //------------Standard Side Menu Screen---------------------------
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //draw toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        //supportActionBar?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#FF677589")))

//        val fab = findViewById(R.id.fab) as FloatingActionButton
//        fab.setOnClickListener { view ->
//            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                    .setAction("Action", null).show()
//        }
   //     navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
        //---------------------------------------------------------------------

        parallelRequestsManager = ParallelRequestsManager(2)

        // Access a Cloud Firestore instance from your Activity

        itemsGridList.layoutManager = GridLayoutManager(this, 2)
        adapter = ItemsGridListAdapter(itemsList, applicationContext, this, RecommenderManager())
        itemsGridList.adapter = adapter

        //Recover configuration variables
        updateValuesFromBundle(savedInstanceState)

        val itemDAO = ItemDAO()
        itemDAO.getAllItems {
            itemsList.addAll(it)
            parallelRequestsManager.decreaseRemainingRequests()
            if(parallelRequestsManager.isComplete!!)
                buildRecommender()
        }
        val ratingDAO = RatingDAO()
        ratingDAO.getAllItems {
            ratingsList.addAll(it)
            parallelRequestsManager.decreaseRemainingRequests()
            if(parallelRequestsManager.isComplete!!)
                buildRecommender()
        }
//        if(ApplicationProperties.userNotDefinedYet()){
//            val getPreferencesIntent = Intent(applicationContext, FeaturePreferencesActivity::class.java)
//            startActivityForResult(getPreferencesIntent, REQUEST_GET_PREFERENCES)
//        }
    }

    private fun updateValuesFromBundle(savedInstanceState: Bundle?) {
        savedInstanceState ?: return

        // Update the value of requestingLocationUpdates from the Bundle.
        if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
            requestingLocationUpdates = savedInstanceState.getBoolean(
                    REQUESTING_LOCATION_UPDATES_KEY)
        }
    }

    private fun updateRecommender() {
            buildRecommender()
            Toast.makeText(applicationContext, "Atualizado!", Toast.LENGTH_SHORT).show()
    }

    private fun buildRecommender() {
        adapter.recommenderManager.recommender = RecommenderBuilder().buildKNNRecommender(ratingsList, applicationContext)

        if(!ApplicationProperties.userNotDefinedYet()) {
            for(item in itemsList){
                val rating = adapter.recommenderManager.getPrediction(ApplicationProperties.user!!.id, item.id)
                if (rating != null)
                    item.recommedationRating = rating
                else item.recommedationRating = 0F
            }
            itemsList.sortByDescending{it.recommedationRating}
        }
        adapter.notifyDataSetChanged()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_main_activity, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.option_features -> {
                val goToFeaturePreferences = Intent(applicationContext, FeaturePreferencesActivity::class.java)
               // goToPlayerProfileIntent.putExtra("uid", uid)
                startActivityForResult(goToFeaturePreferences, REQUEST_GET_PREFERENCES)
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            if(requestCode == REQUEST_CHECK_LOCATION_SETTINGS){
                requestingLocationUpdates = true
            }
            else {
                Toast.makeText(applicationContext, "Atualizando recomendações...", Toast.LENGTH_SHORT).show()
                if (requestCode == REQUEST_GET_PREFERENCES) {
                    if (data != null)
                        (data.getSerializableExtra("featureRatings") as List<Rating>).forEach {
                            ratingsList.add(it)
                        }
                }
                if (requestCode == REQUEST_ITEM_RATING_CHANGE) {
                    if (data != null)
                        ratingsList.add(data.getSerializableExtra("itemRating") as Rating)
                }
                updateRecommender()
            }
        }else if(requestCode == REQUEST_CHECK_LOCATION_SETTINGS){
            requestingLocationUpdates = false
        }
    }


    override fun shareOnItemClicked(p1: Int) {
        if(ApplicationProperties.user == null)
        {
            //DEBUG
            Toast.makeText(applicationContext, "Usário não definido! Primeiro informe seu nome na página de preferências.", Toast.LENGTH_LONG).show()
            return
       //     ApplicationProperties.user = User("Felipe", "Felipe")
            //DEBUG
        }
        val viewItemDetails = Intent(applicationContext, ItemDetailActivity::class.java)
        val itemId = itemsList[p1].id
        var itemRating : Float
        ratingsList.find { it.user == ApplicationProperties.user?.id
                && it.item == itemId }?.let {
            itemRating = it.rating
            viewItemDetails.putExtra("itemRating", itemRating)
        }

        viewItemDetails.putExtra("itemClicked", itemsList[p1])
        startActivityForResult(viewItemDetails, REQUEST_ITEM_RATING_CHANGE)
    }

    override fun onMapReady(p0: GoogleMap) {
        val locationListener : Int

        mMap = p0
        // Add a marker in Sydney and move the camera
        //LatLng sydney = new LatLng(-34, 151);
        //mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.setMinZoomPreference(6f)
        mMap.setMaxZoomPreference(20f)
        //Initialize Location
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createLocationRequest()

//        if (Build.VERSION.SDK_INT >= 23) {
//            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
//            } else {
//                mFusedLocationClient.requestLocationUpdates()
////                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, locationListener)
//                setUserLocation()
////                setDestination()
//            }//else there is already permission
//        } else {
////            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, locationListener)
//            setUserLocation()
////            setDestination()
//
//        }
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }

    override fun onResume() {
        super.onResume()
        if (requestingLocationUpdates) startLocationUpdates()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        mFusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                null /* Looper */)
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    private fun stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(locationCallback)
    }

    fun createLocationRequest() {
        locationRequest = LocationRequest.create()?.apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        locationRequest?.let {
            val builder = LocationSettingsRequest.Builder()
                    .addLocationRequest(it)
            val client: SettingsClient = LocationServices.getSettingsClient(this)
            val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())
            task.addOnSuccessListener { locationSettingsResponse ->
                requestingLocationUpdates =  true
                startLocationUpdates()
            }

            task.addOnFailureListener { exception ->
                if (exception is ResolvableApiException){
                    // Location settings are not satisfied. Show to the user a dialog.
                    try {
                        // then check the result in onActivityResult().
                        exception.startResolutionForResult(this@MainActivity,
                                REQUEST_CHECK_LOCATION_SETTINGS)
                    } catch (sendEx: IntentSender.SendIntentException) {
                        // Ignore the error.
                    }
                }
            }
        }
    }

    private var locationCallback = object :  LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            locationResult ?: return
            for (location in locationResult.locations){
                val userLocation = LatLng(location.latitude, location.longitude)
                val markerOptions = MarkerOptions()
                markerOptions.position(userLocation)
                markerOptions.title("Sua posição")
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                mCurrLocationMarker = mMap.addMarker(markerOptions)
                //mMap.clear();

                //Marker currentLocationMarker = mMap.addMarker(new MarkerOptions().position(userLocation));
                mMap.moveCamera(CameraUpdateFactory.newLatLng(userLocation))
                mMap.animateCamera(CameraUpdateFactory.zoomTo(12f))
            }
        }
    }

    private fun setUserLocation() {
        //@SuppressLint("MissingPermission") val lastKnowLocation = mFusedLocationClient.getLastKnownLocation(LocationManager.GPS_PROVIDER)
    }
//
//            //  mMap.clear();
//            //            MarkerOptions mp = new MarkerOptions().icon(BitmapDescriptorFactory.defaultMarker());
//            //            mp.position(new LatLng(location.getLatitude(), location.getLongitude()));
//            //            mMap.addMarker(mp);
//            if (destinationCircle != null) {
//                mMap.addCircle(destinationCircle)
//                val btConfirmar = findViewById(R.id.btConfirm)
//                if (checkIfUserArrivedAtDestination(location)) {
//                    btConfirmar.setVisibility(View.VISIBLE)
//                } else
//                    btConfirmar.setVisibility(View.GONE)
//            }
//
//            mLastLocation = location
//            if (mCurrLocationMarker != null) {
//                mCurrLocationMarker.remove()
//            }
//
//            //Place current location marker
//            val latLng = LatLng(location.latitude, location.longitude)
//            val markerOptions = MarkerOptions()
//            markerOptions.position(latLng)
//            markerOptions.title("Sua posição")
//            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
//            mCurrLocationMarker = mMap.addMarker(markerOptions)

}



