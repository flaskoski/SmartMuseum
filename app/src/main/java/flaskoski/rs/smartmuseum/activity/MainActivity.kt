package flaskoski.rs.smartmuseum.activity

import androidx.lifecycle.ViewModelProvider
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import androidx.core.app.ActivityCompat
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import flaskoski.rs.rs_cf_test.recommender.RecommenderBuilder
import flaskoski.rs.smartmuseum.DAO.ItemDAO
import flaskoski.rs.smartmuseum.DAO.RatingDAO
import flaskoski.rs.smartmuseum.util.ParallelRequestsManager
import flaskoski.rs.smartmuseum.listAdapter.ItemsGridListAdapter
import flaskoski.rs.smartmuseum.model.Item
import flaskoski.rs.smartmuseum.model.Rating
import flaskoski.rs.smartmuseum.recommender.RecommenderManager
import flaskoski.rs.smartmuseum.util.ApplicationProperties
import kotlinx.android.synthetic.main.activity_main_bottom_sheet.*
import java.util.*
import flaskoski.rs.smartmuseum.R
import flaskoski.rs.smartmuseum.databinding.ActivityMainBinding
import flaskoski.rs.smartmuseum.location.MapManager
import flaskoski.rs.smartmuseum.model.User
import flaskoski.rs.smartmuseum.routeBuilder.JourneyManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.grid_item.*
import kotlinx.android.synthetic.main.grid_item.view.*
import java.lang.Exception
import java.lang.IllegalStateException
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity(), ItemsGridListAdapter.OnShareClickListener,
        MapManager.onUserArrivedToDestinationCallback {


    private val REQUEST_GET_PREFERENCES: Int = 1
    private val REQUEST_ITEM_RATING_CHANGE: Int = 2

    private var itemsList : List<Item> = ArrayList()
    private var ratingsList  = HashSet<Rating>()
    private var currentItem : Item? = null
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

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>

    private lateinit var journeyManager : JourneyManager

    override fun onCreate(savedInstanceState: Bundle?) {
        val isDebugging = true
        //------------Standard Side Menu Screen---------------------------
        super.onCreate(savedInstanceState)
        DataBindingUtil.setContentView<ActivityMainBinding>(
                this, R.layout.activity_main)
//        setContentView(R.layout.activity_main)
        journeyManager = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application)).get(JourneyManager::class.java)
        //draw toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#FF0099CC")))

        bottomSheetBehavior = BottomSheetBehavior.from(sheet_next_items)
        bringToFront(loading_view, 50f)
        bringToFront(sheet_next_items, 40f)
        bringToFront(card_view, 30f)

   //     navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        itemsGridList.layoutManager = GridLayoutManager(this, 2)
        adapter = ItemsGridListAdapter(itemsList, applicationContext, this, RecommenderManager())
        itemsGridList.adapter = adapter

        //--DEBUG
        if(isDebugging) {
            ApplicationProperties.user = User("Felipe", "Felipe", 155.0)
            bt_begin_route.visibility = View.VISIBLE
            journeyManager.isPreferencesSet = true
        }
        //--

        getItemsAndRatingsBeforeRecommend = ParallelRequestsManager(2)
        val itemDAO = ItemDAO()
        itemDAO.getAllPoints {points ->
            (itemsList as ArrayList).addAll(points.filter { it is Item } as List<Item>)
            journeyManager.build(points, itemsList, this)
            getItemsAndRatingsBeforeRecommend.decreaseRemainingRequests()
            if(getItemsAndRatingsBeforeRecommend.isComplete) {
                journeyManager.isItemsAndRatingsLoaded = true
                buildRecommender()

            }
        }
        val ratingDAO = RatingDAO()
        ratingDAO.getAllItems {
            ratingsList.addAll(it)
            getItemsAndRatingsBeforeRecommend.decreaseRemainingRequests()
            if(getItemsAndRatingsBeforeRecommend.isComplete) {
                journeyManager.isItemsAndRatingsLoaded = true
                buildRecommender()
            }
        }

        if(!isDebugging) {
            if (ApplicationProperties.userNotDefinedYet()) {
                val getPreferencesIntent = Intent(applicationContext, FeaturePreferencesActivity::class.java)
                startActivityForResult(getPreferencesIntent, REQUEST_GET_PREFERENCES)
            }
        }
    }

    private fun updateRecommender() {
            Toast.makeText(applicationContext, "Atualizando recomendações...", Toast.LENGTH_SHORT).show()
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
        }
        adapter.notifyDataSetChanged()
    }

    private fun getRecommendedRoute() {
        if (journeyManager.isJourneyBegan)
            try {journeyManager.getRecommendedRoute()} catch (e: Exception) { e.printStackTrace() }
    }

    private fun sortItemList() {
        var i = 0
        itemsList.sortedWith(compareBy<Item>{it.isVisited}.thenBy{ it.recommendedOrder}).forEach{
            (itemsList as java.util.ArrayList<Item>)[i++] = it
        }
        adapter.notifyDataSetChanged()

    }

    @Suppress("UNUSED_PARAMETER")
    fun onClickBeginRoute(v : View){
        try {
            journeyManager.isJourneyBegan = true
            getRecommendedRoute()
            sortItemList()
            setNextRecommendedDestination()
            bt_begin_route.visibility = View.GONE
        }
        catch (e: IllegalStateException){
            Log.e(TAG, e.message)
            e.printStackTrace()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if(requestCode == journeyManager.REQUEST_CHANGE_LOCATION_SETTINGS){
                journeyManager.userLocationManager?.createLocationRequest()
            }
            else {
                loading_view.visibility = View.VISIBLE
                if (requestCode == REQUEST_GET_PREFERENCES) {
                    if (data != null) {
                        (data.getSerializableExtra("featureRatings") as List<*>).forEach {
                            ratingsList.add(it as Rating)
                        }
                        journeyManager.timeAvailable = data.getDoubleExtra("timeAvailable", 120.0)
                        updateRecommender()
                        getRecommendedRoute()
                        sortItemList()
                        if (!journeyManager.isPreferencesSet) {
                            journeyManager.isPreferencesSet = true
                            bt_begin_route.visibility = View.VISIBLE
                        }
                    }
                }
                else if (requestCode == REQUEST_ITEM_RATING_CHANGE) {
                    if (data != null) {
                        val rating : Rating? = data.getSerializableExtra("itemRating")?.let { it as Rating }
                        val nextItem : Boolean = data.getBooleanExtra("nextItem", false)
                        if(nextItem) {
                            currentItem?.isVisited = true
                            card_view.visibility = View.GONE
                            if(rating != null) {//rating changed
                                ratingsList.remove(rating)
                                ratingsList.add(rating)
                                updateRecommender()
                            }

                            if(journeyManager.isJourneyFinished()) {
                                showFinishedMessage()
                            }
                            else {
                                if (rating != null)
                                    getRecommendedRoute()
                                sortItemList()
                                setNextRecommendedDestination()
                            }
                        }
                        else
                            if(rating != null) {//rating changed
                                ratingsList.remove(rating)
                                ratingsList.add(rating)
                                updateRecommender()
                                getRecommendedRoute()
                                sortItemList()
                            }
                    }
                }
                loading_view.visibility = View.GONE
            }
        }
    }

    private fun showFinishedMessage() {
        val confirmationDialog = AlertDialog.Builder(this@MainActivity, R.style.Theme_AppCompat_Dialog_Alert)
        confirmationDialog.setTitle("Atenção")
                .setIcon(R.drawable.baseline_done_black_24)
                .setMessage("Você já visitou todos os itens recomendados para você. Obrigado pela visita!")
                .setNeutralButton(android.R.string.ok, null)
        confirmationDialog.show()
    }

    //-----------onClick --------------

    override fun shareOnItemClicked(p1: Int, arrived : Boolean) {
        if(ApplicationProperties.user == null) {
            Toast.makeText(applicationContext, "Usário não definido! Primeiro informe seu nome na página de preferências.", Toast.LENGTH_LONG).show()
            return
        }
        val viewItemDetails = Intent(applicationContext, ItemDetailActivity::class.java)
        val itemId = itemsList[p1].id
        var itemRating : Float
        ratingsList.find { it.user == ApplicationProperties.user?.id
                && it.item == itemId }?.let {
            itemRating = it.rating
            viewItemDetails.putExtra("itemRating", itemRating)
        }
        currentItem = itemsList[p1]
        viewItemDetails.putExtra("itemClicked", currentItem)
        viewItemDetails.putExtra("arrived", arrived)
        startActivityForResult(viewItemDetails, REQUEST_ITEM_RATING_CHANGE)
    }

    //-------------MAPS AND LOCATION----------------------------------------

    private fun setNextRecommendedDestination() {
        var item : Item? = null
        if(!itemsList[0].isVisited && itemsList[0].recommendedOrder != Int.MAX_VALUE)
            item = itemsList[0]

        if(item != null){
            journeyManager.previousItem?.let { journeyManager.findAndSetShortestPath(item, it) }
            journeyManager.mapManager?.setDestination(item, journeyManager.previousItem)
            journeyManager.previousItem = item
        }
        else {
            Log.w(TAG, "Todos os itens já foram visitados e setNextRecommendedDestination foi chamado.")
            Toast.makeText(applicationContext, "Todos os itens já foram visitados.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onUserArrivedToDestination() {
        card_view.lb_info.visibility = View.VISIBLE
        card_view.lb_item_name.text = itemsList[0].title
        card_view.ratingBar.rating = itemsList[0].recommedationRating
        card_view.img_itemThumb.setImageResource(applicationContext.resources.getIdentifier(itemsList[0].photoId, "drawable", applicationContext.packageName))
        card_view.visibility = View.VISIBLE
        card_view.icon_visited.visibility = View.GONE
        card_view.setOnClickListener{this.shareOnItemClicked(0, true)}
    }

    override fun onResume() {
        super.onResume()
        journeyManager.userLocationManager?.startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        journeyManager.userLocationManager?.stopLocationUpdates()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.checkSelfPermission(applicationContext, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                journeyManager.userLocationManager?.createLocationRequest()
            }

        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun goToUserLocation(v: View) {
        journeyManager.userLocationManager?.userLastKnownLocation?.let { journeyManager.mapManager?.goToLocation(it) }
    }


    //------------LAYOUT FEATURES-------------------------------

    private fun bringToFront(view: View, zval: Float = 20f){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view.translationZ = zval;
            view.invalidate();
        }
        else {
            view.bringToFront()
            view.parent.requestLayout()
            //sheet_next_items.parent.invalidate()
        }
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



