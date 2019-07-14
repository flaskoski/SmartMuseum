package flaskoski.rs.smartmuseum.viewmodel
import android.app.Activity
import android.content.Intent
import androidx.lifecycle.ViewModel
import android.util.Log
import androidx.databinding.Observable
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import flaskoski.rs.rs_cf_test.recommender.RecommenderBuilder
import flaskoski.rs.smartmuseum.DAO.SharedPreferencesDAO
import flaskoski.rs.smartmuseum.location.MapManager
import flaskoski.rs.smartmuseum.model.*
import flaskoski.rs.smartmuseum.routeBuilder.RecommendedRouteBuilder
import flaskoski.rs.smartmuseum.util.ApplicationProperties
import flaskoski.rs.smartmuseum.util.ParseTime
import java.util.*
//import javax.inject.Inject
import kotlin.collections.ArrayList

class JourneyManager //@Inject constructor(itemRepository: ItemRepository)
    : ViewModel(), MapManager.OnUserArrivedToDestinationListener {

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
    var isCloseToItem = MutableLiveData<Boolean>()

    var isMapLoaded: Boolean = false
    private var whenMapLoadedSetDestination: Boolean = false
    //--

    private var startTime: Date? = null

    var mapManager: MapManager? = null
    var userLocationManager : UserLocationManager? = null

    private var activity: Activity? = null
    private var sharedPreferences: SharedPreferencesDAO? = null

    val REQUEST_CHANGE_LOCATION_SETTINGS = 3

    //GET ITEMS INFORMATION
    private val isItemAndRatingListLoadedListener = object : Observable.OnPropertyChangedCallback(){
        override fun onPropertyChanged(observable: Observable, i: Int) {
            //if items and ratings are loaded
            if(ItemRepository.isItemListLoaded.get() && ItemRepository.isRatingListLoaded.get()){
                itemsList.addAll(ItemRepository.itemList)
                ratingsList = ItemRepository.ratingList
                subItemList = ItemRepository.subItemList

                recommendedRouteBuilder = RecommendedRouteBuilder(ItemRepository.allElements)
                lastItem = getFirstItem()

                buildRecommender()
                recoverSavedRecommendedItems()
                sortItemList()
                setNextRecommendedDestination()
                isItemsAndRatingsLoaded.value = true

            }
        }
    }

    private fun getFirstItem() : Point?{
        return userLocationManager?.userLatLng?.let {recommendedRouteBuilder?.getNearestPointFromUser(Point(it))}
                ?: recommendedRouteBuilder?.getAllEntrances()?.first()
    }

    init {
        //live data vars initialization
        isCloseToItem.value = false
        isPreferencesSet.value = false
        isItemsAndRatingsLoaded.value = false
        isGoToNextItem.value = false
        isJourneyBegan.value = false
        isJourneyFinishedFlag.value = false

        //Repository observers init
        ItemRepository.isRatingListLoaded.addOnPropertyChangedCallback(isItemAndRatingListLoadedListener)
        ItemRepository.isItemListLoaded.addOnPropertyChangedCallback(isItemAndRatingListLoadedListener)

        //maps setup
        userLocationManager = UserLocationManager(REQUEST_CHANGE_LOCATION_SETTINGS)
        mapManager = MapManager(this){
            isMapLoaded = true
            Log.i(TAG, "Map loaded")
            if(whenMapLoadedSetDestination) {
                whenMapLoadedSetDestination = false
                setNextRecommendedDestination()
            }
                //mapManager?.setDestination()
        }
        userLocationManager?.onUserLocationUpdateCallback = mapManager?.updateUserLocationCallback
    }

    fun recoverSavedRecommendedItems() {
        sharedPreferences?.getAllRecommendedItemStatus()?.let {list->
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
            (itemsList as ArrayList<Item>)[i++] = it
        }
        itemListChangedListener?.invoke()
    }

//    fun getNextClosestItem(): Point? {
//        //TODO: Has to catch the closest entrance to the user
//        if(this.museumGraph?.entrances == null) {
//            Log.e(TAG, "no entrances found!")
//            throw Exception("No entrances found in the graph!")
//        }
//
//        lastItem = this.closestItem ?: museumGraph?.entrances?.first()
//        lastItem?.isClosest = false
//        closestItem = museumGraph?.getClosestItemTo(lastItem!!)
//        closestItem?.isClosest = true
//        return closestItem
//    }

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

    fun beginJourney() {
        lastItem = getFirstItem()
        startTime = ParseTime.getCurrentTime()
        sharedPreferences?.saveStartTime(startTime!!)
        isJourneyFinishedFlag.value = false
        isJourneyBegan.value = true
        getRecommendedRoute()
        sortItemList()
        setNextRecommendedDestination()
        isGoToNextItem.value = true
    }

    /***
     * set app state with the information saved from the last time it was used if the user didn't finish the journey.
     * @return User saved
     */
    fun recoverSavedPreferences(): User? {
        if(ApplicationProperties.userNotDefinedYet() || startTime == null) {
            ApplicationProperties.user = sharedPreferences?.getUser()
            this.startTime = sharedPreferences?.getStartTime()
            if (!ApplicationProperties.userNotDefinedYet()) {
                isPreferencesSet.value = true
                if (startTime != null)
                    isJourneyBegan.value = true
            }
        }else{
            isPreferencesSet.value = true
            isJourneyBegan.value = true
        }
        return ApplicationProperties.user
    }


    private fun getRecommendedRoute() {
        if(recommendedRouteBuilder == null || lastItem == null) throw Exception("previous point is null. Did you instantiate RecommendedRouteBuilder?")

        ItemRepository.resetRecommendedOrder()
        val itemsRemaining = recommendedRouteBuilder?.getRecommendedRouteFrom(lastItem!!,
                ApplicationProperties.user?.timeAvailable?: 100.0,
                startTime?.let {
                    ParseTime.differenceInMinutesUntilNow(it) } ?: 0.0)
        if(itemsRemaining!= null && itemsRemaining.isEmpty())
            Log.w(TAG, "No time available for visiting any item.")
        itemsRemaining?.toSet()?.let {
            sharedPreferences?.setAllRecommendedItems(it) }
    }

    fun setNextRecommendedDestination() {
        if(isMapLoaded) {
            nextItem = null
            if (!itemsList[0].isVisited && itemsList[0].recommendedOrder != Int.MAX_VALUE)
                nextItem = itemsList[0]

            if (nextItem != null) {
                if (lastItem != null) {
                    if (lastItem!!.isUserPoint())
                        recommendedRouteBuilder?.findAndSetShortestPathFromUserLocation(nextItem!!, lastItem!!)
                    else recommendedRouteBuilder?.findAndSetShortestPath(nextItem!!, lastItem!!)

                    try {
                        mapManager?.setDestination(nextItem!!, lastItem,
                                LatLng(userLocationManager?.userLastKnownLocation?.latitude!!, userLocationManager?.userLastKnownLocation?.longitude!!))
                        isGoToNextItem.value = true
                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                        //                Toast.makeText(applicationContext, "Erro ao carregar posição.", Toast.LENGTH_SHORT)
                    }

                } else
                    Log.e(TAG, "Último ponto visitado não foi identificado!")
            } else {
                Log.w(TAG, "Todos os itens já foram visitados e setNextRecommendedDestination foi chamado.")
//            Toast.makeText(applicationContext, "Todos os itens já foram visitados.", Toast.LENGTH_LONG).show()
            }
        }else whenMapLoadedSetDestination = true
    }

    fun changeLocationSettingsResult() {
        userLocationManager?.createLocationRequest()
    }

    fun getPreferencesResult(data: Intent) {
        (data.getSerializableExtra("featureRatings") as List<*>).forEach {
            ratingsList.add(it as Rating)
        }
        ApplicationProperties.user?.let { sharedPreferences?.saveUser(it) }
        updateRecommender()
       // getRecommendedRoute()
        sortItemList()
        if (!isPreferencesSet.value!!) {
            isPreferencesSet.value = true
        }
    }

    fun itemRatingChangeResult(data: Intent) {
        val rating : Rating? = data.getSerializableExtra(ApplicationProperties.TAG_ITEM_RATING)?.let { it as Rating }
        val goToNextItem : Boolean = data.getBooleanExtra(ApplicationProperties.TAG_GO_NEXT_ITEM, false)
        val arrived : Boolean = data.getBooleanExtra(ApplicationProperties.TAG_ARRIVED, false)
        val visitedSubItems : List<String>? =  data.getSerializableExtra(ApplicationProperties.TAG_VISITED_SUBITEMS)?.let { it as List<String> }
        if(arrived)
            visitedSubItems?.let { sharedPreferences?.setVisitedSubItems(it) }
        if(goToNextItem) {
            lastItem = nextItem
            isGoToNextItem.value = true
            isCloseToItem.value = false
            (lastItem as Item).isVisited = true

            if(rating != null) {//rating changed
                ratingsList.remove(rating)
                ratingsList.add(rating)
                updateRecommender()
            }else sharedPreferences?.setRecommendedItem(lastItem as Item)

            if(isJourneyFinished())
                finishJourney()

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
                if(isJourneyBegan.value!!) if(rating.item != nextItem?.id){
                    getRecommendedRoute()
                    sortItemList()
                    setNextRecommendedDestination()
                }
            }
    }

    fun finishJourney() {
        sharedPreferences?.clear()
        ApplicationProperties.resetConfigurations()
        resetConfigurations()
        isPreferencesSet.value = false
        isJourneyFinishedFlag.value = true
    }

    fun restartJourney(){
        sharedPreferences?.resetJourney()
        resetConfigurations()
    }

    private fun resetConfigurations(){
        ItemRepository.resetJourney()
        mapManager?.clearMap()

        isJourneyBegan.value = false
        isGoToNextItem.value = false
        isCloseToItem.value = false
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

    fun recoverCurrentState() {

    }

    fun setOnDestroyActivityState() {
        isMapLoaded = false
    }
}