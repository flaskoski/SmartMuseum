package flaskoski.rs.smartmuseum.activity

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import flaskoski.rs.rs_cf_test.recommender.RecommenderBuilder
import flaskoski.rs.smartmuseum.DAO.ItemDAO
import flaskoski.rs.smartmuseum.DAO.RatingDAO
import flaskoski.rs.smartmuseum.util.ParallelRequestsManager
import flaskoski.rs.smartmuseum.listAdapter.ItemsGridListAdapter
import flaskoski.rs.smartmuseum.model.Item
import flaskoski.rs.smartmuseum.model.Rating
import flaskoski.rs.smartmuseum.model.UserLocationManager
import flaskoski.rs.smartmuseum.recommender.RecommenderManager
import flaskoski.rs.smartmuseum.util.ApplicationProperties
import kotlinx.android.synthetic.main.activity_main_bottom_sheet.*
import java.util.*
import flaskoski.rs.smartmuseum.R
import flaskoski.rs.smartmuseum.location.MapManager
import flaskoski.rs.smartmuseum.model.User
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity(), ItemsGridListAdapter.OnShareClickListener {

    private val REQUEST_GET_PREFERENCES: Int = 1
    private val REQUEST_ITEM_RATING_CHANGE: Int = 2
    val REQUEST_CHANGE_LOCATION_SETTINGS: Int = 3

    private var isPreferencesSet : Boolean = false
    private var userLocationManager : UserLocationManager? = null
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

    private lateinit var getItemsAndRatingsBeforeRecommend: ParallelRequestsManager

    //TODO verificar como substituir essa variavel
    private var requestingLocationUpdates: Boolean = false

    private val REQUESTING_LOCATION_UPDATES_KEY: String = "request_updates_key"

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, requestingLocationUpdates)
        super.onSaveInstanceState(outState)
    }

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>

    private var mapManager: MapManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        //------------Standard Side Menu Screen---------------------------
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //draw toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        mapManager = MapManager(this).build(){
            userLocationManager = UserLocationManager(this, REQUEST_CHANGE_LOCATION_SETTINGS, mapManager?.updateUserLocationCallback!!)
        }

        bottomSheetBehavior = BottomSheetBehavior.from(sheet_next_items)
        //supportActionBar?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#FF677589")))

   //     navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
        //---------------------------------------------------------------------

        getItemsAndRatingsBeforeRecommend = ParallelRequestsManager(2)

        // Access a Cloud Firestore instance from your Activity

        itemsGridList.layoutManager = GridLayoutManager(this, 2)
        adapter = ItemsGridListAdapter(itemsList, applicationContext, this, RecommenderManager())
        itemsGridList.adapter = adapter

        //Recover configuration variables
        updateValuesFromBundle(savedInstanceState)

        //--DEBUG
        ApplicationProperties.user = User("Felipe", "Felipe")
        if(!isPreferencesSet){
            bt_begin_route.visibility = View.VISIBLE
            isPreferencesSet = true
        }
        //--


        val itemDAO = ItemDAO()
        itemDAO.getAllItems {
            itemsList.addAll(it)
            getItemsAndRatingsBeforeRecommend.decreaseRemainingRequests()
            if(getItemsAndRatingsBeforeRecommend.isComplete)
                buildRecommender()
        }
        val ratingDAO = RatingDAO()
        ratingDAO.getAllItems {
            ratingsList.addAll(it)
            getItemsAndRatingsBeforeRecommend.decreaseRemainingRequests()
            if(getItemsAndRatingsBeforeRecommend.isComplete)
                buildRecommender()
        }

//
//        if(ApplicationProperties.userNotDefinedYet()){
//            val getPreferencesIntent = Intent(applicationContext, FeaturePreferencesActivity::class.java)
//            startActivityForResult(getPreferencesIntent, REQUEST_GET_PREFERENCES)
//        }
    }

    fun onClickBeginRoute(v : View){
        bt_begin_route.visibility = View.GONE
        setDestination()
    }

    private fun setDestination() {
        val item : Item? = itemsList.filter{ !it.isVisited}[0]
        if(item != null) mapManager?.setDestination(item)
        else {
            Log.w(TAG, "Todos os itens já foram visitados e setDestination foi chamado.")
            Toast.makeText(applicationContext, "Todos os itens já foram visitados.", Toast.LENGTH_SHORT).show()
        }
    }

    fun updateValuesFromBundle(savedInstanceState: Bundle?) {
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
        return when (item.itemId) {
            R.id.option_features -> {
                val goToFeaturePreferences = Intent(applicationContext, FeaturePreferencesActivity::class.java)
                // goToPlayerProfileIntent.putExtra("uid", uid)
                startActivityForResult(goToFeaturePreferences, REQUEST_GET_PREFERENCES)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            if(requestCode == REQUEST_CHANGE_LOCATION_SETTINGS){
                userLocationManager?.createLocationRequest()
            }
            else {
                Toast.makeText(applicationContext, "Atualizando recomendações...", Toast.LENGTH_SHORT).show()
                if (requestCode == REQUEST_GET_PREFERENCES) {
                    if (data != null)
                        (data.getSerializableExtra("featureRatings") as List<*>).forEach {
                            ratingsList.add(it as Rating)
                        }
                    updateRecommender()
                    if(!isPreferencesSet) {
                        isPreferencesSet = true
                        bt_begin_route.visibility = View.VISIBLE
                    }
                }
                if (requestCode == REQUEST_ITEM_RATING_CHANGE) {
                    if (data != null)
                        ratingsList.add(data.getSerializableExtra("itemRating") as Rating)
                    updateRecommender()
                }

            }
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

    override fun onResume() {
        super.onResume()
        userLocationManager?.startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        userLocationManager?.stopLocationUpdates()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.checkSelfPermission(applicationContext, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                userLocationManager?.createLocationRequest()
            }

        }
    }

    fun goToUserLocation(v: View) {
        userLocationManager?.userLastKnownLocation?.let { mapManager?.goToLocation(it) }
    }

    fun toggleSheet(v: View){
        if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
         else
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
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



