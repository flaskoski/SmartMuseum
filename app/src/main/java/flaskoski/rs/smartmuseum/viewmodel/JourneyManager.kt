package flaskoski.rs.smartmuseum.viewmodel
import android.app.Activity
import android.content.Intent
import androidx.lifecycle.ViewModel
import android.util.Log
import androidx.databinding.Observable
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.SupportMapFragment
import flaskoski.rs.rs_cf_test.recommender.RecommenderBuilder
import flaskoski.rs.smartmuseum.DAO.SharedPreferencesDAO
import flaskoski.rs.smartmuseum.location.MapManager
import flaskoski.rs.smartmuseum.model.*
import flaskoski.rs.smartmuseum.recommender.RecommenderManager
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
    private val TAG = "JourneyManager"

    var ratingsList  = HashSet<Rating>()
    var itemsList = ArrayList<Item>()
    private var subItemList = ArrayList<SubItem>()
//    var currentItem : Item? = null

    //--state flags
    var itemListChangedListener : (()->Unit)? = null
    var isItemsAndRatingsLoaded = MutableLiveData<Boolean>()
    var isPreferencesSet = MutableLiveData<Boolean>()
    var isCurrentItemVisited = MutableLiveData<Boolean>()
    var isJourneyFinishedFlag = MutableLiveData<Boolean>()
    var isJourneyBegan = MutableLiveData<Boolean>()
    var isCloseToItem = MutableLiveData<Boolean>()
    //--

    var timeAvailable: Double = 120.0
    private var startTime: Date? = null

    var mapManager: MapManager? = null
    var userLocationManager : UserLocationManager? = null
    val recommenderManager = RecommenderManager()

    private var activity: Activity? = null
    private var sharedPreferences: SharedPreferencesDAO? = null

    val REQUEST_CHANGE_LOCATION_SETTINGS = 3

    //GET ITEMS INFORMATION
    private val isItemAndRatingListLoadedListener = object : Observable.OnPropertyChangedCallback(){
        override fun onPropertyChanged(observable: Observable, i: Int) {
            if(ItemRepository.isItemListLoaded.get() && ItemRepository.isRatingListLoaded.get()){
                itemsList.addAll(ItemRepository.itemList)
                ratingsList = ItemRepository.ratingList
                subItemList = ItemRepository.subItemList

                recommendedRouteBuilder = RecommendedRouteBuilder(ItemRepository.allElements)
                lastItem = recommendedRouteBuilder?.getAllEntrances()?.first()

                isItemsAndRatingsLoaded.value = true
                buildRecommender()
            }
        }
    }

    init {
        //live data vars initialization
        isCloseToItem.value = false
        isPreferencesSet.value = false
        isItemsAndRatingsLoaded.value = false
        isCurrentItemVisited.value = false
        isJourneyBegan.value = false
        isJourneyFinishedFlag.value = false

        //Repository observers init
        ItemRepository.isRatingListLoaded.addOnPropertyChangedCallback(isItemAndRatingListLoadedListener)
        ItemRepository.isItemListLoaded.addOnPropertyChangedCallback(isItemAndRatingListLoadedListener)

        //maps setup
        userLocationManager = UserLocationManager(REQUEST_CHANGE_LOCATION_SETTINGS)
        mapManager = MapManager(this)
        userLocationManager?.onUserLocationUpdateCallback = mapManager?.updateUserLocationCallback

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
        recommenderManager.recommender = RecommenderBuilder().buildKNNRecommender(ratingsList)

        if(!ApplicationProperties.userNotDefinedYet()) {
            for(item in itemsList){
                val rating = recommenderManager.getPrediction(ApplicationProperties.user!!.id, item.id)
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
        startTime = ParseTime.getCurrentTime()
        sharedPreferences?.saveStartTime(startTime!!)
        isJourneyFinishedFlag.value = false
        isJourneyBegan.value = true
        getRecommendedRoute()
        sortItemList()
        setNextRecommendedDestination()
    }

    private fun getRecommendedRoute() {
        if(recommendedRouteBuilder == null || lastItem == null) throw Exception("previous point is null. Did you instantiate RecommendedRouteBuilder?")

        val itemsRemaining = recommendedRouteBuilder?.getRecommendedRouteFrom(lastItem!!, timeAvailable,
                startTime?.let { ParseTime.differenceInMinutesUntilNow(it) } ?: 0.0)
        itemsRemaining?.toSet()?.let {sharedPreferences?.setAllRecommendedItems(it) }
    }

    private fun setNextRecommendedDestination() {
        var item : Item? = null
        if(!itemsList[0].isVisited && itemsList[0].recommendedOrder != Int.MAX_VALUE)
            item = itemsList[0]

        if(item != null){
            lastItem?.let { recommendedRouteBuilder?.findAndSetShortestPath(item, it) }
            try{
                mapManager?.setDestination(item, lastItem)
            }
            catch(e: java.lang.Exception){
                e.printStackTrace()
//                Toast.makeText(applicationContext, "Erro ao carregar posição.", Toast.LENGTH_SHORT)
            }
            lastItem = item
        }
        else {
            Log.w(TAG, "Todos os itens já foram visitados e setNextRecommendedDestination foi chamado.")
//            Toast.makeText(applicationContext, "Todos os itens já foram visitados.", Toast.LENGTH_LONG).show()
        }
    }

    fun changeLocationSettingsResult() {
        userLocationManager?.createLocationRequest()
    }

    fun getPreferencesResult(data: Intent) {
        (data.getSerializableExtra("featureRatings") as List<*>).forEach {
            ratingsList.add(it as Rating)
        }
        timeAvailable = ApplicationProperties.user?.timeAvailable!!
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
        val nextItem : Boolean = data.getBooleanExtra(ApplicationProperties.TAG_GO_NEXT_ITEM, false)
        val arrived : Boolean = data.getBooleanExtra(ApplicationProperties.TAG_ARRIVED, false)
        val visitedSubItems : List<String>? =  data.getSerializableExtra(ApplicationProperties.TAG_VISITED_SUBITEMS)?.let { it as List<String> }
        if(arrived)
            visitedSubItems?.let { sharedPreferences?.setVisitedSubItems(it) }
        if(nextItem) {
            isCurrentItemVisited.value = true
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
                if(isJourneyBegan.value!!) {
                    getRecommendedRoute()
                    sortItemList()
                    setNextRecommendedDestination()
                }
            }
    }

    private fun finishJourney() {
        sharedPreferences?.clear()
        resetConfigurations()
        isPreferencesSet.value = false
        isJourneyFinishedFlag.value = true
    }

    fun resetConfigurations(){
        sharedPreferences?.resetJourney()
        ItemRepository.resetJourney()
        mapManager?.clearMap()

        isJourneyBegan.value = false
        isCurrentItemVisited.value = false
        isCloseToItem.value = false
    }
    /***
     * set app state with the information saved from the last time it was used if the user didn't finish the journey.
     * @return User saved
     */

    fun recoverSavedPreferences(): User? {
        //TODO items already visited
        ApplicationProperties.user = sharedPreferences?.getUser()
        this.startTime = sharedPreferences?.getStartTime()
        if(!ApplicationProperties.userNotDefinedYet()) {
            this.isPreferencesSet.value = true
            if(startTime != null)
                this.isJourneyBegan.value = true
        }
        return ApplicationProperties.user
    }

    fun recoverSavedJourney() {
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
                sortItemList()
                setNextRecommendedDestination()
            }
        }
    }

    fun getSubItemsOf(group: GroupItem): List<Itemizable> {
        val subItems = ArrayList<Itemizable>()
        group.subItems.forEach { subItemId ->
            val item = recommendedRouteBuilder?.allItems?.find { subItemId == it.id }
            if(item != null) subItems.add(item)
        }
        return subItems
    }
}