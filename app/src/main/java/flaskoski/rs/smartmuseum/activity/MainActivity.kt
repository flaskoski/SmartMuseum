package flaskoski.rs.smartmuseum.activity

import android.annotation.SuppressLint
import android.app.Activity
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
import android.view.ViewTreeObserver
import android.widget.LinearLayout
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.Marker
import com.google.android.material.snackbar.Snackbar
import flaskoski.rs.smartmuseum.listAdapter.ItemsGridListAdapter
import flaskoski.rs.smartmuseum.util.ApplicationProperties
import kotlinx.android.synthetic.main.activity_main_bottom_sheet.*
import flaskoski.rs.smartmuseum.R
import flaskoski.rs.smartmuseum.databinding.ActivityMainBinding
import flaskoski.rs.smartmuseum.model.GroupItem
import flaskoski.rs.smartmuseum.model.Item
import flaskoski.rs.smartmuseum.model.ItemRepository
import flaskoski.rs.smartmuseum.util.NetworkVerifier
import flaskoski.rs.smartmuseum.viewmodel.JourneyManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.next_item.*
import kotlinx.android.synthetic.main.next_item.view.*
/**
 * Copyright (c) 2019 Felipe Ferreira Laskoski
 * código fonte licenciado pela MIT License - https://opensource.org/licenses/MIT
 */

class MainActivity : AppCompatActivity(),
        ItemsGridListAdapter.OnShareClickListener,
        ViewTreeObserver.OnGlobalLayoutListener,
        GoogleMap.OnInfoWindowClickListener{

    override fun onGlobalLayout() {
        container.viewTreeObserver.removeOnGlobalLayoutListener(this)
        bottomSheetBehavior.halfExpandedRatio = 0.8f
    }


    companion object {
        private const val REQUEST_GET_PREFERENCES: Int = 1
        private const val REQUEST_ITEM_RATING_CHANGE: Int = 2
        const val REQUEST_CHANGE_LOCATION_SETTINGS = 3
        private const val REQUEST_QUESTIONNAIRE: Int = 4
        private const val REQUEST_GO_TO_SCHEDULED_ITEM: Int = 5

        //intent extras
        const val TAG_ROUTE_TO_THIS_ENABLED: String = "RouteToThis"
        const val TAG_GO_TO_THIS: String = "goToThis"
        const val TAG_ITEM_ID: String = "itemId"
    }


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

    private lateinit var networkWarning: Snackbar
    private lateinit var networkVerifier: NetworkVerifier

    override fun onCreate(savedInstanceState: Bundle?) {
        //------------Standard Side Menu Screen---------------------------
        super.onCreate(savedInstanceState)
        DataBindingUtil.setContentView<ActivityMainBinding>(
                this, R.layout.activity_main)

        //draw toolbar
//        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#FF0099CC")))

        loading_view.visibility = View.VISIBLE

        networkWarning = AlertBuilder().buildNetworkUnavailableWarning(bt_begin_route, true, true)
        //NetworkVerifier
        networkVerifier = NetworkVerifier()
                .setOnAvailableCallback {
                    ItemRepository.retryToDownloadData()
                    if(!journeyManager.checkedForUpdates)
                        checkForUpdates()
                    networkWarning.dismiss()
                }
                .setOnUnavailableCallback {
                    if(journeyManager.ratingsList.isEmpty() || journeyManager.itemsList.isEmpty())
                        networkWarning.show()
                }

        //attach view model to activity
        journeyManager = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application)).get(JourneyManager::class.java)

        //journeyManager activity, userLocation and maps setup
        journeyManager.updateActivity(this)
        journeyManager.buildMap(supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment)

        //set observable states
        journeyManager.isPreferencesSet.observe(this, preferencesSetListener)
        journeyManager.isItemsAndRatingsLoaded.observe(this, isItemsAndRatingsLoadedListener  )
        journeyManager.isJourneyBegan.observe(this, isJourneyBeganListener)
        journeyManager.isCloseToItem.observe(this, closeToItemIsChangedListener)
        journeyManager.isGoToNextItem.observe(this, isGoToNextItemListener)
        journeyManager.isJourneyFinishedFlag.observe(this, isJourneyFinishedListener)
        journeyManager.itemListChangedListener = {
            @Suppress("UNNECESSARY_SAFE_CALL")
            adapter?.notifyDataSetChanged()
            if(view_next_item.visibility == View.VISIBLE)
                updateNextItemCard()
//            loading_view.visibility = View.GONE
        }

        //bottomsheet setup and bring views to front
        bottomSheetBehavior = BottomSheetBehavior.from(sheet_next_items)
        ApplicationProperties.bringToFront(loading_view, 50f)
        ApplicationProperties.bringToFront(sheet_next_items, 40f)
        ApplicationProperties.bringToFront(view_next_item, 30f)

        container.viewTreeObserver.addOnGlobalLayoutListener(this)
   //     navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        //GridItems setup
        itemsGridList.layoutManager = GridLayoutManager(this, 2)
        adapter = ItemsGridListAdapter(journeyManager.itemsList, applicationContext, this, ItemRepository.recommenderManager)
        itemsGridList.adapter = adapter

        if(journeyManager.isItemsAndRatingsLoaded.value!! && journeyManager.isJourneyBegan.value!!)
        {
            journeyManager.setNextRecommendedDestination()
        }else if(journeyManager.recoverSavedPreferences() == null){
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
            startActivityForResult(getPreferencesIntent, REQUEST_GET_PREFERENCES)
//                }
        }


        if(!NetworkVerifier().isNetworkAvailable(applicationContext))
            networkWarning.show()
            //AlertBuilder().showNetworkDisconnected(this@MainActivity)
        else if(!journeyManager.checkedForUpdates)
            checkForUpdates()
    }

    private fun checkForUpdates(){
        ApplicationProperties.checkForUpdates(ApplicationProperties.getCurrentVersionCode(applicationContext)) { isThereUpdates ->
            isThereUpdates?.let {
                if (isThereUpdates == true)
                    if (ApplicationProperties.checkIfForceUpdateIsOn() == true)
                        AlertBuilder().showUpdateRequired(this@MainActivity) {
                            finish()
                        }
                    else {
                        AlertBuilder().showUpdateAvailable(this@MainActivity)
                    }
                journeyManager.checkedForUpdates = true
            }
        }
    }

    //Show next item card on screen
    @SuppressLint("SetTextI18n")
    private val closeToItemIsChangedListener = Observer<Boolean> { isClose : Boolean ->
        if(isClose && journeyManager.showNextItem_okPressed) {
            journeyManager.showNextItem_okPressed = false
            view_next_item.lb_info.text =
                    "${journeyManager.itemsList[0].hintText.let{it+" "}}${getString(R.string.lb_you_arrived)}"
            view_next_item.visibility = View.VISIBLE
            bt_ok.visibility = View.GONE
            view_next_item.setOnClickListener { this.shareOnItemClicked(0, true) }
        }
    }

    private val preferencesSetListener = Observer<Boolean>{ preferencesSet : Boolean ->
        if(preferencesSet && !journeyManager.isJourneyBegan.value!!){
            beginAndShowStartMessage()
        }
    }

    override fun onDestroy() {
        journeyManager.setOnDestroyActivityState()
        super.onDestroy()
    }

    private val isItemsAndRatingsLoadedListener = Observer<Boolean>{ loaded : Boolean ->
        if(loaded)
            journeyManager.recoverCurrentState()
        if(journeyManager.isJourneyFinishedFlag.value!!)
            loading_view.visibility = View.GONE
    }

    private val isJourneyBeganListener = Observer<Boolean> {}

    private val isJourneyFinishedListener = Observer<Boolean> { isJourneyFinished: Boolean ->
        if(isJourneyFinished){
            adapter.notifyDataSetChanged()
            AlertDialog.Builder(this@MainActivity, R.style.Theme_AppCompat_Dialog_Alert)
                    .setTitle("Atenção")
                    .setIcon(R.drawable.baseline_done_black_24)
                    .setMessage(journeyManager.finishMessage)
                    .setNeutralButton(R.string.ok){_,_ ->}
                    .setOnDismissListener {
                        if (!journeyManager.isQuestionnaireAnswered) {
                            val goToQuestionnaire = Intent(applicationContext, QuestionnaireActivity::class.java)
                            startActivityForResult(goToQuestionnaire, REQUEST_QUESTIONNAIRE)
                        }else
                            finishedAndQuestionnaireAnswered()
                    }
                    .show()

        }
    }

    private fun finishedAndQuestionnaireAnswered() {
        view_next_item.visibility = View.GONE
        if(journeyManager.isItemsAndRatingsLoaded.value!!)
            loading_view.visibility = View.GONE
        if (journeyManager.finishButtonClicked) {
            journeyManager.finishButtonClicked = false
            AlertDialog.Builder(this@MainActivity, R.style.Theme_AppCompat_Dialog_Alert)
                    .setTitle("Atenção")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage("""Para começar com um novo perfil, todas as avaliações feitas e preferências serão removidas.
                                |Deseja continuar?""".trimMargin())
                    .setPositiveButton(R.string.yes) { _, _ ->
                        journeyManager.finishUserSession()
                        val getPreferencesIntent = Intent(applicationContext, FeaturePreferencesActivity::class.java)
                        startActivityForResult(getPreferencesIntent, REQUEST_GET_PREFERENCES)
                    }
                    .setNegativeButton(R.string.no) { _, _ -> }
                    .show()
        } else if (!journeyManager.isQuestionnaireAnswered)
            journeyManager.setQuestionnaireAnswered()
    }

    private fun beginAndShowStartMessage() {
        journeyManager.recoverCurrentState()
        val startDialog = AlertDialog.Builder(this@MainActivity, R.style.Theme_AppCompat_Dialog_Alert)
        startDialog.setTitle(getString(R.string.welcome_title))
                .setMessage(getString(R.string.welcome_message))
                .setNeutralButton(android.R.string.ok) { _, _ -> }
        startDialog.show()
    }

    private fun showNextItemCard(){
        if(journeyManager.isRatingChanged) {
            lb_info.text = getString(R.string.lb_next_item_with_rating_change)
            journeyManager.nextItemCardShowedWithRatingChangeWarning()
        }else lb_info.text = getString(R.string.lb_next_item)

        bt_ok.visibility = View.VISIBLE

        view_next_item.lb_next_item_name.text = journeyManager.itemsList[0].title
        view_next_item.next_item_ratingBar.rating = journeyManager.itemsList[0].recommedationRating
        ItemRepository.loadImage(applicationContext, view_next_item.next_item_img_itemThumb, journeyManager.itemsList[0].photoId)
        view_next_item.visibility = View.VISIBLE
        view_next_item.setOnClickListener{}
    }
    private val isGoToNextItemListener = Observer<Boolean> { isGoToNextItem: Boolean ->
        if(isGoToNextItem && journeyManager.isJourneyBegan.value!!){
            showNextItemCard()
            loading_view.visibility = View.GONE
        }
    }


    private fun updateNextItemCard() {
        view_next_item.lb_next_item_name.text = journeyManager.itemsList[0].title
        view_next_item.next_item_ratingBar.rating = journeyManager.itemsList[0].recommedationRating
        ItemRepository.loadImage(applicationContext, view_next_item.next_item_img_itemThumb, journeyManager.itemsList[0].photoId)
    }

    fun onClickNextItemOk(@Suppress("UNUSED_PARAMETER") v : View){
        journeyManager.showNextItem_okPressed = true
        view_next_item.visibility = View.GONE
        if(journeyManager.isFirstTime) {
            Toast.makeText(applicationContext, getString(R.string.follow_line),
                    Toast.LENGTH_LONG).show()
            goToUserLocation(v)
        }
        closeToItemIsChangedListener.onChanged(journeyManager.isCloseToItem.value)
    }

//    fun beginJourney(){
//        try {
//            if(!journeyManager.isJourneyBegan.value!!)
//                journeyManager.recoverCurrentState()
//        }
//        catch (e: IllegalStateException){
//            Log.e(TAG, e.message)
//            e.printStackTrace()
//        }
//    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(!NetworkVerifier().isNetworkAvailable(applicationContext))
            AlertBuilder().showNetworkDisconnected(this@MainActivity)

        if (resultCode == RESULT_OK && data != null) {
            when (requestCode) {
                REQUEST_CHANGE_LOCATION_SETTINGS -> {
                    journeyManager.createLocationRequest()
                    journeyManager.recoverCurrentState()
                }
                REQUEST_GET_PREFERENCES ->{
                    journeyManager.getPreferencesResult(data)
                }
                REQUEST_ITEM_RATING_CHANGE-> {
                    journeyManager.itemRatingChangeResult(data)
                }
                REQUEST_GO_TO_SCHEDULED_ITEM->{
                    journeyManager.routeToItem(data)
                    adapter.notifyDataSetChanged()
                }
            }
        }else if(resultCode == RESULT_OK && requestCode == REQUEST_QUESTIONNAIRE){
            if(journeyManager.isJourneyFinishedFlag.value!!) {
                finishedAndQuestionnaireAnswered()
            }
        }else if(resultCode == Activity.RESULT_CANCELED && requestCode == REQUEST_CHANGE_LOCATION_SETTINGS)
            journeyManager.createLocationRequest()
    }


    //-----------onClick --------------

    override fun onInfoWindowClick(p0: Marker?) {
        if(p0 == null) return
        val itemSelected = ItemRepository.itemList.find { it.getCoordinates() == p0.position }
                ?: return
        val viewItemDetails : Intent = setupItemDetailsIntent(itemSelected) ?: return
        if(itemSelected.hasSpecificHours() && itemSelected != journeyManager.itemsList[0]) {
            viewItemDetails.putExtra(TAG_ROUTE_TO_THIS_ENABLED, true)
            startActivityForResult(viewItemDetails, REQUEST_GO_TO_SCHEDULED_ITEM)
        }else startActivityForResult(viewItemDetails, REQUEST_ITEM_RATING_CHANGE)
    }

    override fun shareOnItemClicked(p1: Int, isArrived : Boolean) {
        val viewItemDetails : Intent = setupItemDetailsIntent(journeyManager.itemsList[p1])
                ?: return
        if(p1 == 0 && journeyManager.isCloseToItem.value!!)
            viewItemDetails.putExtra(ApplicationProperties.TAG_ARRIVED, true)
        else viewItemDetails.putExtra(ApplicationProperties.TAG_ARRIVED, isArrived)
        startActivityForResult(viewItemDetails, REQUEST_ITEM_RATING_CHANGE)
    }

    private fun setupItemDetailsIntent(item : Item): Intent? {
        if(ApplicationProperties.user == null) {
            Toast.makeText(applicationContext, "Usuário não definido! Primeiro informe seu nome na página de preferências.", Toast.LENGTH_LONG).show()
            return null
        }
        val viewItemDetails : Intent = if(item is GroupItem) {
            Intent(applicationContext, GroupItemDetailActivity::class.java)
        }
        else Intent(applicationContext, ItemDetailActivity::class.java)
        var itemRating = 0F
        journeyManager.ratingsList.find { it.user == ApplicationProperties.user?.id
                && it.item == item.id }?.let {
            itemRating = it.rating
        }

        viewItemDetails.putExtra("itemClicked",  item)
        //viewItemDetails.putExtra("subItems",  subItems)
        viewItemDetails.putExtra(ApplicationProperties.TAG_ITEM_RATING_VALUE,  itemRating)

        ApplicationProperties.user?.let {
            it.location = journeyManager.userLocationManager?.userLatLng
        }?: Log.e(TAG, "gridItem(${item.id})OnClick - user not defined!")
        return viewItemDetails
    }

    override fun shareOnRemoveItemClicked(p1: Int) {
        val confirmationDialog = AlertDialog.Builder(this@MainActivity, R.style.Theme_AppCompat_Dialog_Alert)
        confirmationDialog.setTitle("Atenção")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage("Tem certeza que deseja remover essa atração da sua rota recomendada?")
                .setPositiveButton(android.R.string.yes) { _, _ ->
                    journeyManager.removeItemFromRoute(journeyManager.itemsList[p1]){
                        Snackbar.make(sheet_next_items, getString(R.string.item_removed), Snackbar.LENGTH_SHORT).show()
                    }
                   // view_next_item.visibility = View.GONE
                }.setNegativeButton(android.R.string.no){ _, _ -> }
        confirmationDialog.show()
    }

    override fun shareOnAddItemClicked(p1: Int){
        val confirmationDialog = AlertDialog.Builder(this@MainActivity, R.style.Theme_AppCompat_Dialog_Alert)
        confirmationDialog.setTitle("Atenção")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage("Tem certeza que deseja adicionar essa atração a sua rota recomendada?")
                .setPositiveButton(android.R.string.yes) { _, _ ->
                    journeyManager.addItemToRoute(journeyManager.itemsList[p1]){
                        Snackbar.make(sheet_next_items, getString(R.string.item_added), Snackbar.LENGTH_SHORT).show()
                    }
                    // view_next_item.visibility = View.GONE
                }.setNegativeButton(android.R.string.no){ _, _ -> }
        confirmationDialog.show()
    }


    //-------------MAPS AND LOCATION----------------------------------------

    override fun onResume() {
        super.onResume()
        journeyManager.userLocationManager?.startLocationUpdates()

        networkVerifier.registerNetworkCallbackV21(applicationContext)
    }

    override fun onPause() {
        super.onPause()
        journeyManager.userLocationManager?.stopLocationUpdates()

        networkVerifier.unRegisterNetworkCallbackV21()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.checkSelfPermission(applicationContext, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                journeyManager.userLocationManager?.createLocationRequest()
                if(journeyManager.isJourneyBegan.value!!)
                    journeyManager.recoverCurrentState()
            }

        }
    }

    fun goToUserLocation(@Suppress("UNUSED_PARAMETER") v: View) {
        journeyManager.focusOnUserPosition()
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
                startActivityForResult(goToFeaturePreferences, REQUEST_GET_PREFERENCES)
                true
            }
            R.id.option_questionnaire->{
                val goToQuestionnaire = Intent(applicationContext, QuestionnaireActivity::class.java)
                startActivityForResult(goToQuestionnaire, REQUEST_QUESTIONNAIRE)
                true
            }
            R.id.option_restart -> {
                val confirmationDialog = AlertDialog.Builder(this@MainActivity, R.style.Theme_AppCompat_Dialog_Alert)
                confirmationDialog.setTitle("Atenção")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setMessage("Deseja recomeçar a sua visita? Isso irá apagar suas informações de itens que já visitou.")
                        .setPositiveButton(android.R.string.yes) { _, _ ->
                            journeyManager.restartJourney()
                            view_next_item.visibility = View.GONE
                            beginAndShowStartMessage()
                        }.setNegativeButton(android.R.string.no){ _, _ -> }
                confirmationDialog.show()
                true
            }
            R.id.option_finish -> {
                journeyManager.finishButtonClicked = true
                journeyManager.completeJourney("""${getString(R.string.thank_you_for_visiting)}
                    |${getString(R.string.answer_survey)}""".trimMargin())
                true
            }
            R.id.option_debug->{
                ApplicationProperties.isArrivedIsSet = !ApplicationProperties.isArrivedIsSet
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun toggleSheet(@Suppress("UNUSED_PARAMETER") v: View){
        if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_HALF_EXPANDED)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
         else
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }
}



