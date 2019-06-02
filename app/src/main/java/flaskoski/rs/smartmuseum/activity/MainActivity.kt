package flaskoski.rs.smartmuseum.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
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

    private var mMap: GoogleMap? = null
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private var mCurrLocationMarker : Marker? = null
    private var locationCallback :LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            locationResult ?: return
            for (location in locationResult.locations){
                val userLocationLatLng = LatLng(location.latitude, location.longitude)
                if(mCurrLocationMarker == null) {
                    val markerOptions = MarkerOptions().position(userLocationLatLng).title("Sua posição")
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.blue_circle))
                    mCurrLocationMarker = mMap?.addMarker(markerOptions)
                }
                else mCurrLocationMarker?.position = userLocationLatLng
                //mMap.clear();

                //Marker currentLocationMarker = mMap.addMarker(new MarkerOptions().position(userLocation));
//                    mMap?.moveCamera(CameraUpdateFactory.newLatLng(userLocation))
//                    mMap?.animateCamera(CameraUpdateFactory.zoomTo(12f))
            }
        }
    }
    private lateinit var locationManager : LocationManager
    private var requestingLocationUpdates: Boolean = false

    private val REQUESTING_LOCATION_UPDATES_KEY: String = "request_updates_key"
    private var locationRequest: LocationRequest? = null

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, requestingLocationUpdates)
        super.onSaveInstanceState(outState)
    }

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>

    override fun onCreate(savedInstanceState: Bundle?) {
        //------------Standard Side Menu Screen---------------------------
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //draw toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        bottomSheetBehavior = BottomSheetBehavior.from(sheet_next_items)
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
        addItemsToMap()
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
        if(p0 == null){
            Toast.makeText(applicationContext, "Erro ao carregar o mapa.", Toast.LENGTH_SHORT).show()
            throw Exception("null maps variable")
        }
        mMap = p0
        mMap?.mapType = GoogleMap.MAP_TYPE_SATELLITE
        // Add a marker in Sydney and move the camera
        //LatLng sydney = new LatLng(-34, 151);
        //mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap?.setMinZoomPreference(16f)
        //mMap.set
      //  mMap.setLatLngBoundsForCameraTarget(LatLngBounds(LatLng(-23.7, -46.57), LatLng(-23.6, -46.67)))
        mMap?.moveCamera(CameraUpdateFactory.zoomTo(19.5f))
        mMap?.moveCamera(CameraUpdateFactory.newLatLng(LatLng(-23.651450,-46.622546)));
        //Initialize Location
        addItemsToMap()
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createLocationRequest()
    }

    private fun addItemsToMap() {
        mMap?.clear()
        if(mCurrLocationMarker != null) mCurrLocationMarker = null
        for(item in itemsList)
            item.coordinates?.let {  mMap?.addMarker(MarkerOptions().position(it).title(item.title)) }
    }

    override fun onResume() {
        super.onResume()
        if (requestingLocationUpdates) startLocationUpdates()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {



//        locationCallback = object :  LocationCallback() {
//            override fun onLocationResult(locationResult: LocationResult?) {
//                locationResult ?: return
//                for (location in locationResult.locations){
//                    val userLocation = LatLng(location.latitude, location.longitude)
//                    val markerOptions = MarkerOptions()
//                    markerOptions.position(userLocation)
//                    markerOptions.title("Sua posição")
//                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
//                    mCurrLocationMarker = mMap?.addMarker(markerOptions)
//                    //mMap.clear();
//
//                    //Marker currentLocationMarker = mMap.addMarker(new MarkerOptions().position(userLocation));
//                    mMap?.moveCamera(CameraUpdateFactory.newLatLng(userLocation))
//                    mMap?.animateCamera(CameraUpdateFactory.zoomTo(12f))
//                }
//            }
//        }
        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            Toast.makeText(applicationContext, "need permission", Toast.LENGTH_SHORT).show()
        mFusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                null)
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    private fun stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                createLocationRequest()
            }

        }
    }

    private fun createLocationRequest() {
        if (Build.VERSION.SDK_INT >= 23)
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1)

        locationRequest = LocationRequest.create()?.apply {
            interval = 1000
            fastestInterval = 1000
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

    fun toggleSheet(v: View){
        if (bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED)
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
         else
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
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



