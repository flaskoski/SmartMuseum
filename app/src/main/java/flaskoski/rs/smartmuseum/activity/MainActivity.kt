package flaskoski.rs.smartmuseum.activity

import androidx.lifecycle.ViewModelProvider
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
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
import androidx.lifecycle.Observer
import com.google.android.gms.maps.SupportMapFragment
import flaskoski.rs.smartmuseum.listAdapter.ItemsGridListAdapter
import flaskoski.rs.smartmuseum.util.ApplicationProperties
import kotlinx.android.synthetic.main.activity_main_bottom_sheet.*
import flaskoski.rs.smartmuseum.R
import flaskoski.rs.smartmuseum.databinding.ActivityMainBinding
import flaskoski.rs.smartmuseum.model.GroupItem
import flaskoski.rs.smartmuseum.model.ItemRepository
import flaskoski.rs.smartmuseum.viewmodel.JourneyManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.next_item.*
import kotlinx.android.synthetic.main.next_item.view.*
import java.lang.IllegalStateException


class MainActivity : AppCompatActivity(), ItemsGridListAdapter.OnShareClickListener {

    private val requestGetPreferences: Int = 1
    private val requestItemRatingChange: Int = 2

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
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var journeyManager : JourneyManager

    override fun onCreate(savedInstanceState: Bundle?) {
        //------------Standard Side Menu Screen---------------------------
        super.onCreate(savedInstanceState)
        DataBindingUtil.setContentView<ActivityMainBinding>(
                this, R.layout.activity_main)

        //draw toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#FF0099CC")))

        loading_view.visibility = View.VISIBLE
        //attach view model to activity
        journeyManager = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application)).get(JourneyManager::class.java)

        //journeyManager activity, userLocation and maps setup
        journeyManager.updateActivity(this)
        journeyManager.buildMap(supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment)

        journeyManager.isPreferencesSet.observe(this, preferencesSetListener)
        journeyManager.isItemsAndRatingsLoaded.observe(this, isItemsAndRatingsLoadedListener)
        journeyManager.isJourneyBegan.observe(this, isJourneyBeganListener)
        journeyManager.isCloseToItem.observe(this, closeToItemIsChangedListener)
        journeyManager.isGoToNextItem.observe(this, isGoToNextItemListener)
        journeyManager.isJourneyFinishedFlag.observe(this, isJourneyFinishedListener)
        journeyManager.itemListChangedListener = {
            @Suppress("UNNECESSARY_SAFE_CALL")
            adapter?.notifyDataSetChanged()
            loading_view.visibility = View.GONE
        }

        //bottomsheet setup
        bottomSheetBehavior = BottomSheetBehavior.from(sheet_next_items)
        ApplicationProperties.bringToFront(loading_view, 50f)
        ApplicationProperties.bringToFront(sheet_next_items, 40f)
        ApplicationProperties.bringToFront(view_next_item, 30f)

   //     navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        //GridItems setup
        itemsGridList.layoutManager = GridLayoutManager(this, 2)
        adapter = ItemsGridListAdapter(journeyManager.itemsList, applicationContext, this, ItemRepository.recommenderManager)
        itemsGridList.adapter = adapter

        if(journeyManager.recoverSavedPreferences() == null){
            //--DEBUG
//                @Suppress("ConstantConditionIf")
//                if(isDebugging) {
//                    ApplicationProperties.user = User("Felipe", "Felipe", 155.0)
//                    bt_begin_route.visibility = View.VISIBLE
//                    journeyManager.isPreferencesSet.value = true
//                }
//                //--DEBUG
//                else {
            val getPreferencesIntent = Intent(applicationContext, FeaturePreferencesActivity::class.java)
            startActivityForResult(getPreferencesIntent, requestGetPreferences)
//                }
        }

//        @Suppress("ConstantConditionIf")
//        if(isDebugging) {
//            val sharedPref = this.getPreferences(Context.MODE_PRIVATE)
//            var name = ""
//            name = sharedPref.getString("name", "")
//            ApplicationProperties.user = User(name, "Felipe", 155.0, userTimeSpent)
//            with (sharedPref.edit()) {
//                putString("name", "Felipe")
//                apply()
//            }
          //  bt_begin_route.visibility = View.VISIBLE
//            journeyManager.isPreferencesSet.value = true
//        }
//        else{
//            if (ApplicationProperties.userNotDefinedYet()) {
//                val getPreferencesIntent = Intent(applicationContext, FeaturePreferencesActivity::class.java)
//                startActivityForResult(getPreferencesIntent, requestGetPreferences)
//            }
//        }
        //--
    }

    //Show next item card on screen
    private val closeToItemIsChangedListener = Observer<Boolean> { isClose : Boolean -> if(isClose) {
        view_next_item.lb_info.text = getString(R.string.lb_you_arrived)
        view_next_item.visibility = View.VISIBLE
        bt_ok.visibility = View.GONE
        view_next_item.setOnClickListener { this.shareOnItemClicked(0, true) }
    }}

    private val preferencesSetListener = Observer<Boolean>{ preferencesSet : Boolean ->
        if(preferencesSet && !journeyManager.isJourneyBegan.value!!){
            bt_begin_route.visibility = View.VISIBLE
        }
    }

    private val isItemsAndRatingsLoadedListener = Observer<Boolean>{ loaded : Boolean ->
        if(loaded && journeyManager.isJourneyBegan.value!!) {
            journeyManager.recoverSavedJourney()

            //DEBUG
            //shareOnItemClicked(journeyManager.itemsList.indexOf(journeyManager.itemsList.find{ it.id == "7I7lVxSXOjvYWE2e5i72"}),false)

        }
    }

    private val isJourneyBeganListener = Observer<Boolean> { isJourneyBegan: Boolean ->
        if(isJourneyBegan)
            bt_begin_route.visibility = View.GONE
    }

    private val isJourneyFinishedListener = Observer<Boolean> { isJourneyFinished: Boolean ->
        if(isJourneyFinished){
            val confirmationDialog = AlertDialog.Builder(this@MainActivity, R.style.Theme_AppCompat_Dialog_Alert)
            confirmationDialog.setTitle("Atenção")
                    .setIcon(R.drawable.baseline_done_black_24)
                    .setMessage("Você já visitou todos os itens recomendados para você. Obrigado pela visita!")
                    .setNeutralButton(android.R.string.ok){_,_ ->
                        val getPreferencesIntent = Intent(applicationContext, FeaturePreferencesActivity::class.java)
                        startActivityForResult(getPreferencesIntent, requestGetPreferences)}
            confirmationDialog.show()

            view_next_item.visibility = View.GONE
            bt_begin_route.visibility = View.VISIBLE
        }
    }

    private val isGoToNextItemListener = Observer<Boolean> { isCurrentItemVisited: Boolean ->
        if(isCurrentItemVisited && journeyManager.isJourneyBegan.value!!){
            lb_info.text = getString(R.string.lb_next_item)
            bt_ok.visibility = View.VISIBLE
            view_next_item.lb_next_item_name.text = journeyManager.itemsList[0].title
            view_next_item.next_item_ratingBar.rating = journeyManager.itemsList[0].recommedationRating
            view_next_item.next_item_img_itemThumb.setImageResource(applicationContext.resources.getIdentifier(journeyManager.itemsList[0].photoId, "drawable", applicationContext.packageName))
            view_next_item.visibility = View.VISIBLE
            view_next_item.setOnClickListener{}
        }
    }

    fun onClickNextItemOk(v : View){
        view_next_item.visibility = View.GONE
    }

    @Suppress("UNUSED_PARAMETER")
    fun onClickBeginRoute(v : View){
        try {
            journeyManager.beginJourney()
            bt_begin_route.visibility = View.GONE
        }
        catch (e: IllegalStateException){
            Log.e(TAG, e.message)
            e.printStackTrace()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data != null) {
            loading_view.visibility = View.VISIBLE

            when (requestCode) {
                journeyManager.REQUEST_CHANGE_LOCATION_SETTINGS -> {
                    journeyManager.changeLocationSettingsResult()
                }
                requestGetPreferences ->{
                    journeyManager.getPreferencesResult(data)
                }
                requestItemRatingChange-> {
                    journeyManager.itemRatingChangeResult(data)
                }
            }
            loading_view.visibility = View.GONE
        }
    }

    //-----------onClick --------------

    override fun shareOnItemClicked(p1: Int, isArrived : Boolean) {
        if(ApplicationProperties.user == null) {
            Toast.makeText(applicationContext, "Usário não definido! Primeiro informe seu nome na página de preferências.", Toast.LENGTH_LONG).show()
            return
        }
        var viewItemDetails : Intent
        //var subItems : ArrayList<Itemizable>? = null
        if(journeyManager.itemsList[p1] is GroupItem) {
            viewItemDetails = Intent(applicationContext, GroupItemDetailActivity::class.java)
//            subItems = journeyManager.getSubItemsOf(journeyManager.itemsList[p1] as GroupItem) as ArrayList<Itemizable>
        }
        else viewItemDetails = Intent(applicationContext, ItemDetailActivity::class.java)
        val itemId = journeyManager.itemsList[p1].id
        var itemRating : Float = 0F
        journeyManager.ratingsList.find { it.user == ApplicationProperties.user?.id
                && it.item == itemId }?.let {
            itemRating = it.rating
        }

        viewItemDetails.putExtra("itemClicked",  journeyManager.itemsList[p1])
        //viewItemDetails.putExtra("subItems",  subItems)
        viewItemDetails.putExtra("itemRating",  itemRating)
        if(p1 == 0 && journeyManager.isCloseToItem.value!!)
            viewItemDetails.putExtra(ApplicationProperties.TAG_ARRIVED, true)
        else viewItemDetails.putExtra(ApplicationProperties.TAG_ARRIVED, isArrived)
        startActivityForResult(viewItemDetails, requestItemRatingChange)
    }

    //-------------MAPS AND LOCATION----------------------------------------

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

    fun goToUserLocation(@Suppress("UNUSED_PARAMETER") v: View) {
        journeyManager.userLocationManager?.userLastKnownLocation?.let { journeyManager.mapManager?.goToLocation(it) }
    }


    //------------LAYOUT FEATURES-------------------------------

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
                startActivityForResult(goToFeaturePreferences, requestGetPreferences)
                true
            }
            R.id.option_cancelar_rota -> {
                val confirmationDialog = AlertDialog.Builder(this@MainActivity, R.style.Theme_AppCompat_Dialog_Alert)
                confirmationDialog.setTitle("Atenção")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setMessage("Deseja cancelar a sua visita? Isso irá apagar suas informações de itens que já visitou.")
                        .setPositiveButton(android.R.string.yes) { _, _ ->
                            journeyManager.resetConfigurations()
                            view_next_item.visibility = View.GONE
                            bt_begin_route.visibility = View.VISIBLE
                        }.setNegativeButton(android.R.string.no){ _, _ -> }
                confirmationDialog.show()
                true
            }
            R.id.option_debug->{
                ApplicationProperties.isDebugOn = !ApplicationProperties.isDebugOn
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun toggleSheet(@Suppress("UNUSED_PARAMETER") v: View){
        if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
         else
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }
}



