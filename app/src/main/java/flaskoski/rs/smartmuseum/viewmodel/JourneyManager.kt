package flaskoski.rs.smartmuseum.viewmodel
import android.app.Activity
import android.content.Intent
import androidx.lifecycle.ViewModel
import android.util.Log
import androidx.databinding.Observable
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import flaskoski.rs.smartmuseum.recommender.RecommenderBuilder
import flaskoski.rs.smartmuseum.DAO.SharedPreferencesDAO
import flaskoski.rs.smartmuseum.activity.MainActivity
import flaskoski.rs.smartmuseum.location.MapManager
import flaskoski.rs.smartmuseum.location.UserLocationManager
import flaskoski.rs.smartmuseum.model.*
import flaskoski.rs.smartmuseum.routeBuilder.RecommendedRouteBuilder
import flaskoski.rs.smartmuseum.util.ApplicationProperties
import flaskoski.rs.smartmuseum.util.ParseTime
import java.util.*
//import javax.inject.Inject
import kotlin.collections.ArrayList

class JourneyManager //@Inject constructor(itemRepository: ItemRepository)
    : ViewModel(), MapManager.OnUserArrivedToDestinationListener {

    var checkedForUpdates: Boolean = false
    var recommendedRouteBuilder : RecommendedRouteBuilder? = null
//    var closestItem: Point? = null
    var lastItem : Point? = null
    private var nextItem: Item? = null
    private val TAG = "JourneyManager"

    var ratingsList  = HashSet<Rating>()
    var itemsList = ArrayList<Item>()
    private var subItemList = ArrayList<SubItem>()
//    var currentItem : Item? = null

    //--state flags
    var itemListChangedListener : (()->Unit)? = null
    var isItemsAndRatingsLoaded = MutableLiveData<Boolean>()
    var isPreferencesSet = MutableLiveData<Boolean>()

    var isGoToNextItem = MutableLiveData<Boolean>()
    var isJourneyFinishedFlag = MutableLiveData<Boolean>()
    var isJourneyBegan = MutableLiveData<Boolean>()

    var showNextItem_okPressed = false
    var isCloseToItem = MutableLiveData<Boolean>()

    var isMapLoaded: Boolean = false
    private var whenMapLoadedSetDestination: Boolean = false
    //tell activity if the route was reconfigured due to the new ratings given
    var isRatingChanged: Boolean = false
    //--

    private var startTime: Date? = null

    var mapManager: MapManager? = null
    var userLocationManager : UserLocationManager? = null

    private var activity: Activity? = null
    private lateinit var sharedPreferences: SharedPreferencesDAO

    //GET ITEMS INFORMATION
    private val isItemAndRatingListLoadedListener = object : Observable.OnPropertyChangedCallback(){
        override fun onPropertyChanged(observable: Observable, i: Int) {
            initializeListsAndBuildRecommendations()
    }}

    private fun isPreferencesSetTrue(){
        isPreferencesSet.value = true
        initializeListsAndBuildRecommendations()
    }

    private fun initializeListsAndBuildRecommendations(){
        //if items and ratings were downloaded, preferences are set and lists weren't initialized yet
        if(ItemRepository.isItemListLoaded.get() && ItemRepository.isRatingListLoaded.get()
                && ItemRepository.itemList.isNotEmpty() && ItemRepository.ratingList.any { it.user != ApplicationProperties.user?.id }
                && isPreferencesSet.value!! && !isItemsAndRatingsLoaded.value!!){
            itemsList.addAll(ItemRepository.itemList)

            recommendedRouteBuilder = RecommendedRouteBuilder(ItemRepository.allElements)
            buildRecommender()
            isItemsAndRatingsLoaded.value = true
        }
    }


    private fun getFirstItem() : Point?{

        return userLocationManager?.userLatLng?.let {recommendedRouteBuilder?.getNearestPointFromUser(Point(it))}
                ?: userLocationManager?.userLastKnownLocation?.let{
                    recommendedRouteBuilder?.getNearestPointFromUser(Point(it))}
//                            ?: recommendedRouteBuilder?.getAllEntrances()?.first()}
    }

    init {
        //live data vars initialization
        isCloseToItem.value = false
        isPreferencesSet.value = false
        isItemsAndRatingsLoaded.value = false
        isGoToNextItem.value = false
        isJourneyBegan.value = false
        isJourneyFinishedFlag.value = false

        ratingsList = ItemRepository.ratingList
        subItemList = ItemRepository.subItemList

        //Repository observers init
        ItemRepository.isRatingListLoaded.addOnPropertyChangedCallback(isItemAndRatingListLoadedListener)
        ItemRepository.isItemListLoaded.addOnPropertyChangedCallback(isItemAndRatingListLoadedListener)
        //maps setup
        userLocationManager = UserLocationManager(MainActivity.REQUEST_CHANGE_LOCATION_SETTINGS)
        mapManager = MapManager(this){
            isMapLoaded = true
            Log.i(TAG, "Map loaded")
            if(whenMapLoadedSetDestination) {
                whenMapLoadedSetDestination = false
                setNextRecommendedDestination()
            }
                //mapManager?.setDestination()
        }
        userLocationManager?.onUserLocationUpdateCallbacks = arrayListOf(mapManager?.updateUserLocationCallback,{
            if(lastItem == null) {
                recoverCurrentState()
            }
        })

        //If exited the app but didn't terminate it and then came back, the ItemRepository will already be loaded
        if(ItemRepository.itemList.isNotEmpty() && ItemRepository.ratingList.isNotEmpty())
            isItemAndRatingListLoadedListener.onPropertyChanged(ItemRepository.isItemListLoaded, 0)
    }


    fun updateActivity(activity : Activity) {
        this.activity = activity
        userLocationManager?.updateActivity(activity)
        sharedPreferences = SharedPreferencesDAO(activity)
    }

    private fun updateRecommender() {
//        Toast.makeText(applicationContext, "Atualizando recomendações...", Toast.LENGTH_SHORT).show()
        buildRecommender()
//        Toast.makeText(applicationContext, "Atualizado!", Toast.LENGTH_SHORT).show()
    }

    private fun buildRecommender() {
        ItemRepository.recommenderManager.recommender = RecommenderBuilder().buildKNNRecommender(ratingsList)

        if(!ApplicationProperties.userNotDefinedYet()) {
            for(item in itemsList){
                val rating = ItemRepository.recommenderManager.getPrediction(ApplicationProperties.user!!.id, item.id)
                if (rating != null)
                    item.recommedationRating = rating
                else item.recommedationRating = 0F
            }
        }
        itemListChangedListener?.invoke()
    }

    private fun sortItemList() {
        var i = 0
        itemsList.sortedWith(compareBy<Itemizable>{it.isVisited}.thenBy{ (it as RoutableItem).recommendedOrder}).forEach{
            itemsList[i++] = it
        }
        itemListChangedListener?.invoke()
    }

    override fun onUserArrivedToDestination() {
        isCloseToItem.value = true
    }

    fun isJourneyFinished(): Boolean {
        if(itemsList.isEmpty()) throw Exception("Manager was not built yet!")
        return itemsList.none { it.isRecommended() && !it.isVisited }
    }

    fun buildMap(mapFragment: SupportMapFragment) {
        mapFragment.getMapAsync(mapManager)
    }

    private fun beginJourney() {
        startTime = ParseTime.getCurrentTime()
        sharedPreferences.saveStartTime(startTime!!)
        isJourneyFinishedFlag.value = false
        if (!getRecommendedRoute()) {
            completeJourney()
            return
        }
        sortItemList()
        setNextRecommendedDestination()
        isJourneyBegan.value = true
        isGoToNextItem.value = true
    }

    fun recoverCurrentState() {
        if(isItemsAndRatingsLoaded.value!! && isPreferencesSet.value!!) {
            lastItem = getFirstItem()
            if(lastItem != null)
                if (isJourneyBegan.value!!) {
                    if (!sharedPreferences.isSavedItemsSynchronized) {
                        recoverSavedRecommendedItems()
                    }
                    sortItemList()
                    setNextRecommendedDestination()
                } else
                    beginJourney()
        }
    }

    /***
     * set app state with the information saved from the last time it was used if the user didn't finish the journey.
     * @return User saved
     */
    fun recoverSavedPreferences(): User? {
        if(ApplicationProperties.userNotDefinedYet() || startTime == null) {
            ApplicationProperties.user = sharedPreferences.getUser()
            this.startTime = sharedPreferences.getStartTime()
            if (!ApplicationProperties.userNotDefinedYet()) {
                isPreferencesSetTrue()
                if (startTime != null)
                    isJourneyBegan.value = true
            }
        }else{
            isPreferencesSetTrue()
            isJourneyBegan.value = true
        }
        return ApplicationProperties.user
    }

    private fun recoverSavedRecommendedItems() {
        sharedPreferences.getAllRecommendedItemStatus().let {list->
            if (list.isNotEmpty()) {
                list.forEach{recommendedItem ->
                    val item : Itemizable?
                    if(recommendedItem is RoutableItem) {
                        item = itemsList.find{it.id == recommendedItem.id}
                        item?.recommendedOrder = recommendedItem.recommendedOrder
                    }else item = ItemRepository.subItemList.find{it.id == recommendedItem.id}
                    item?.isVisited= recommendedItem.isVisited

                }
                lastItem = getFirstItem()
                //?: itemsList.filter { it.isVisited }.sortedWith(compareByDescending<Itemizable>{ (it as RoutableItem).recommendedOrder}).get(0)
            }
        }
    }

    private fun getRecommendedRoute(): Boolean {
        if(recommendedRouteBuilder == null || lastItem == null) throw Exception("previous point is null. Did you instantiate RecommendedRouteBuilder?")

        ItemRepository.resetRecommendedOrder()
        val itemsRemaining = recommendedRouteBuilder?.getRecommendedRouteFrom(lastItem!!,
                ApplicationProperties.user?.timeAvailable?: 100.0,
                startTime?.let { ParseTime.differenceInMinutesUntilNow(it) } ?: 0.0)
        if(itemsRemaining!= null && itemsRemaining.isEmpty()) {
            Log.w(TAG, "No time available for visiting any item.")
            return false
        }
        sharedPreferences.setAllRecommendedItems(itemsList.filter { it.recommendedOrder != Int.MAX_VALUE }, subItemList.filter { it.isRecommended })
        return true
    }

    fun removeItemFromRoute(itemToBeRemoved: Item, callback: () -> Unit) {
        recommendedRouteBuilder?.removeItemFromRoute(itemToBeRemoved)
                ?: Log.e(TAG, "removeItemFromRoute - recommendedRouteBuilder is null")
        sharedPreferences.removeItem(itemToBeRemoved)
                ?: Log.e(TAG, "removeItemFromRoute - sharedPreferences is null")
        sortItemList()
        setNextRecommendedDestination()
        callback.invoke()
    }

    fun setNextRecommendedDestination() {
        if(isMapLoaded) {
            if (lastItem != null) {
                nextItem = null
                if (!itemsList[0].isVisited && itemsList[0].recommendedOrder != Int.MAX_VALUE)
                    nextItem = itemsList[0]

                if (nextItem != null) {
                    if (lastItem!!.isUserPoint())
                        recommendedRouteBuilder?.findAndSetShortestPathFromUserLocation(nextItem!!, lastItem!!)
                    else recommendedRouteBuilder?.findAndSetShortestPath(nextItem!!, lastItem!!)

                    try {
                        mapManager?.setDestination(nextItem!!, lastItem, userLocationManager?.userLastKnownLocation?.let {
                                LatLng(it.latitude, it.longitude) })
                        isGoToNextItem.value = true
                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                        //                Toast.makeText(applicationContext, "Erro ao carregar posição.", Toast.LENGTH_SHORT)
                    }


                } else {
                    if(isJourneyFinished())
                        completeJourney()
                    Log.w(TAG, "Todos os itens já foram visitados e setNextRecommendedDestination foi chamado.")
                }
////            Toast.makeText(applicationContext, "Todos os itens já foram visitados.", Toast.LENGTH_LONG).show()
            }
            else
                Log.e(TAG, "Último ponto visitado não foi identificado!")
        }else whenMapLoadedSetDestination = true
    }

    fun createLocationRequest() {
        userLocationManager?.createLocationRequest()
    }

    fun getPreferencesResult(data: Intent) {
        (data.getSerializableExtra("featureRatings") as List<*>).forEach {
            ratingsList.add(it as Rating)
        }
        ApplicationProperties.user?.let { sharedPreferences.saveUser(it) }
        updateRecommender()
       // getRecommendedRoute()
        sortItemList()
        if (!isPreferencesSet.value!!) {
            isPreferencesSetTrue()
        }
    }

    fun itemRatingChangeResult(data: Intent) {
        val ratingChangedItemId : String? = data.getStringExtra(ApplicationProperties.TAG_RATING_CHANGED_ITEM_ID)
        val goToNextItem : Boolean = data.getBooleanExtra(ApplicationProperties.TAG_GO_NEXT_ITEM, false)
        val arrived : Boolean = data.getBooleanExtra(ApplicationProperties.TAG_ARRIVED, false)
        @Suppress("UNCHECKED_CAST") val visitedSubItems : List<String>? =  data.getSerializableExtra(ApplicationProperties.TAG_VISITED_SUBITEMS)?.let { it as List<String> }
        if(arrived)
            visitedSubItems?.let { sharedPreferences.setVisitedSubItems(it) }
        if(ratingChangedItemId != null) {
            updateRecommender()
            isRatingChanged = true
        }

        if(goToNextItem) {
            lastItem = nextItem
            isGoToNextItem.value = true
            isCloseToItem.value = false
            (lastItem as Item).isVisited = true

            sharedPreferences.setRecommendedItem(lastItem as Item)

            if(isJourneyFinished())
                completeJourney()
            else {
                if (ratingChangedItemId != null)
                    if(!getRecommendedRoute()){
                        completeJourney()
                        return
                    }
                sortItemList()
                setNextRecommendedDestination()
            }
        }
        else
            if(ratingChangedItemId != null) {
                if(isJourneyBegan.value!! && ratingChangedItemId != nextItem?.id){//rating changed from an item that is not the destination item
                    if(!getRecommendedRoute()){
                        completeJourney()
                        return
                    }
                    sortItemList()
                    setNextRecommendedDestination()
                }
            }
    }

    //to go to satisfaction survey
    fun completeJourney() {
        isJourneyFinishedFlag.value = true
    }

    //user finished using the app -> doesn't want to use the app to further check items. Erase all the configured data
    fun finishUserSession(){
        sharedPreferences.clear()
        ApplicationProperties.resetConfigurations()
        resetConfigurations()
        isPreferencesSet.value = false
    }

    fun restartJourney(){
        sharedPreferences.resetJourney()
        resetConfigurations()
    }

    private fun resetConfigurations(){
        ItemRepository.resetJourney()
        mapManager?.clearMap()

        isJourneyBegan.value = false
        isGoToNextItem.value = false
        isCloseToItem.value = false
        showNextItem_okPressed = false
    }

    //TODO move to GroupItem class
    fun getSubItemsOf(group: GroupItem): List<Itemizable> {
        val subItems = ArrayList<Itemizable>()
        group.subItems.forEach { subItemId ->
            val item = recommendedRouteBuilder?.allItems?.find { subItemId == it.id }
            if(item != null) subItems.add(item)
        }
        return subItems
    }

    fun focusOnUserPosition() {
        userLocationManager?.userLastKnownLocation?.let {
            mapManager?.goToLocation(it)
//            mapManager?.setDestination(nextItem!!, lastItem, LatLng(userLastKnowLocation.latitude, userLastKnowLocation.longitude))
        }

    }

    fun setOnDestroyActivityState() {
        isMapLoaded = false
    }

    fun nextItemCardShowedWithRatingChangeWarning() {
        isRatingChanged = false
    }

}