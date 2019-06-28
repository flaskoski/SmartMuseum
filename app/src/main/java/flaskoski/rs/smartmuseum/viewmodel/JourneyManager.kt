package flaskoski.rs.smartmuseum.viewmodel
import android.app.Activity
import android.content.Intent
import androidx.lifecycle.ViewModel
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.SupportMapFragment
import flaskoski.rs.rs_cf_test.recommender.RecommenderBuilder
import flaskoski.rs.smartmuseum.DAO.ItemDAO
import flaskoski.rs.smartmuseum.DAO.RatingDAO
import flaskoski.rs.smartmuseum.DAO.SharedPreferencesDAO
import flaskoski.rs.smartmuseum.R
import flaskoski.rs.smartmuseum.location.MapManager
import flaskoski.rs.smartmuseum.model.*
import flaskoski.rs.smartmuseum.recommender.RecommenderManager
import flaskoski.rs.smartmuseum.routeBuilder.MuseumGraph
import flaskoski.rs.smartmuseum.util.ApplicationProperties
import flaskoski.rs.smartmuseum.util.ParallelRequestsManager
import flaskoski.rs.smartmuseum.util.ParseTime
import java.util.*

class JourneyManager() : ViewModel() ,
        MapManager.OnUserArrivedToDestinationListener {

    var museumGraph: MuseumGraph?  = null
    var closestItem: Point? = null
    var lastItem : Point? = null
    private val TAG = "JourneyManager"
    var itemsList : List<Item> = ArrayList()
    var ratingsList  = HashSet<Rating>()
    var currentItem : Item? = null

    private var pointsRemaining = LinkedList<Point>()

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

    private val MIN_TIME_BETWEEN_ITEMS = 0.5

    var mapManager: MapManager? = null
    var userLocationManager : UserLocationManager? = null

    val REQUEST_CHANGE_LOCATION_SETTINGS = 3

    init {
        //live data vars initialization
        isCloseToItem.value = false
        isPreferencesSet.value = false
        isItemsAndRatingsLoaded.value = false
        isCurrentItemVisited.value = false
        isJourneyBegan.value = false
        isJourneyFinishedFlag.value = false

        //maps setup
        userLocationManager = UserLocationManager(REQUEST_CHANGE_LOCATION_SETTINGS)
        mapManager = MapManager(this)
        userLocationManager?.onUserLocationUpdateCallback = mapManager?.updateUserLocationCallback


        getItemsData()
    }

    private var activity: Activity? = null
    private var sharedPreferences: SharedPreferencesDAO? = null

    fun updateActivity(activity : Activity) {
        this.activity = activity
        userLocationManager?.updateActivity(activity)
        sharedPreferences = SharedPreferencesDAO(activity)
    }

    private fun buildGraph(points: List<Point>, itemList: List<Item>){
        museumGraph = MuseumGraph(points.toHashSet())
        lastItem = museumGraph?.entrances?.first()
    }

    private fun isGraphBuilt() : Boolean{
        return museumGraph != null
    }

    private fun updateRecommender() {
//        Toast.makeText(applicationContext, "Atualizando recomendações...", Toast.LENGTH_SHORT).show()
        buildRecommender()
//        Toast.makeText(applicationContext, "Atualizado!", Toast.LENGTH_SHORT).show()
    }

    val recommenderManager = RecommenderManager()

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
        itemsList.sortedWith(compareBy<Item>{it.isVisited}.thenBy{ it.recommendedOrder}).forEach{
            (itemsList as java.util.ArrayList<Item>)[i++] = it
        }
        itemListChangedListener?.invoke()
    }

    private lateinit var getItemsAndRatingsBeforeRecommend: ParallelRequestsManager
    fun getItemsData() {
        getItemsAndRatingsBeforeRecommend = ParallelRequestsManager(2)
        val itemDAO = ItemDAO()
        itemDAO.getAllPoints { points ->
            (itemsList as ArrayList).addAll(points.filter { it is Item } as List<Item>)

            buildGraph(points, itemsList)
            getItemsAndRatingsBeforeRecommend.decreaseRemainingRequests()
            if (getItemsAndRatingsBeforeRecommend.isComplete) {
                isItemsAndRatingsLoaded.value = true
                buildRecommender()

            }
        }
        val ratingDAO = RatingDAO()
        ratingDAO.getAllItems {
            ratingsList.addAll(it)
            getItemsAndRatingsBeforeRecommend.decreaseRemainingRequests()
            if (getItemsAndRatingsBeforeRecommend.isComplete) {
                isItemsAndRatingsLoaded.value = true
                buildRecommender()
            }
        }
    }


    fun getNextClosestItem(): Point? {
        //TODO: Has to catch the closest entrance to the user
        if(this.museumGraph?.entrances == null) {
            Log.e(TAG, "no entrances found!")
            throw Exception("No entrances found in the graph!")
        }

        lastItem = this.closestItem ?: museumGraph?.entrances?.first()
        lastItem?.isClosest = false
        closestItem = museumGraph?.getClosestItemTo(lastItem!!)
        closestItem?.isClosest = true
        return closestItem
    }

    override fun onUserArrivedToDestination() {
        isCloseToItem.value = true
    }

    private fun getRecommendedRoute(): LinkedList<Point> {
        if(!isGraphBuilt()) throw Exception("previous point is null. Did you buildGraph JourneyManager?")
        pointsRemaining.clear()
        var totalCost = startTime?.let { ParseTime.differenceInMinutesUntilNow(it) } ?: 0.0

        //add most recommended points that have not been visited yet until it reaches total available time
        itemsList?.filter { it is Item && !it.isVisited}?.sortedByDescending { (it as Item).recommedationRating }?.forEach{
            if(totalCost + (it as Item).timeNeeded + MIN_TIME_BETWEEN_ITEMS < timeAvailable){
                totalCost += (it as Item).timeNeeded + MIN_TIME_BETWEEN_ITEMS
                pointsRemaining.add(it)
            }
            //for all points remaining, call get closest, and then if available time is passed, remove the least recommended and try again.
        }
        totalCost -= (pointsRemaining.size-1) * MIN_TIME_BETWEEN_ITEMS
//        pointsRemaining.removeLast()
        var itemsCost = totalCost

        //now will consider the db time to get to each item
        val allPointsRemainingSize = pointsRemaining.size
        for(i in allPointsRemainingSize downTo 1 ){
            var startPoint = lastItem
            var enoughTime = true
            for(j in 1 .. i) {
                //TODO MEMORIZE ITEMS ROUTE COST
                startPoint = museumGraph?.getNextClosestItemFromList(startPoint!!, pointsRemaining.subList(0, i).toHashSet())
                if (startPoint != null) {
                    if (totalCost + startPoint.cost <= timeAvailable) {
                        totalCost += startPoint.cost
                        (startPoint as Item).isVisited = true
                        startPoint.recommendedOrder = j
                    }
                    else{
                        enoughTime = false
                        break
                    }
                }
            }
            pointsRemaining.forEach {
                (it as Item).isVisited = false
                if(!enoughTime) it.recommendedOrder = Int.MAX_VALUE
            }
            if(enoughTime)
                break
            itemsCost -= (pointsRemaining.last as Item).timeNeeded
            totalCost = itemsCost
            pointsRemaining.removeLast()
        }
        sharedPreferences?.setAllRecommendedItems(pointsRemaining.map{it as Item}.toHashSet())
        return pointsRemaining
    }

    fun findAndSetShortestPath(to: Point, from: Point): Point? {
        return museumGraph?.getNextClosestItemFromList(from, setOf(to))
    }

    fun isJourneyFinished(): Boolean {
        if(itemsList == null) throw Exception("Manager was not built yet!")
        return itemsList?.none { it.isRecommended() && !it.isVisited }!!
    }

    fun buildMap(mapFragment: SupportMapFragment) {
        mapFragment.getMapAsync(mapManager)
    }

    fun beginJourney() {
        startTime = ParseTime.getCurrentTime()
        sharedPreferences?.saveStartTime(startTime!!)
        isJourneyBegan.value = true
        getRecommendedRoute()
        sortItemList()
        setNextRecommendedDestination()
    }
    private fun setNextRecommendedDestination() {
        var item : Item? = null
        if(!itemsList[0].isVisited && itemsList[0].recommendedOrder != Int.MAX_VALUE)
            item = itemsList[0]

        if(item != null){
            lastItem?.let { findAndSetShortestPath(item, it) }
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

    fun getPreferencesResult(data: Intent?) {
        if (data != null) {
            (data.getSerializableExtra("featureRatings") as List<*>).forEach {
                ratingsList.add(it as Rating)
            }
            timeAvailable = ApplicationProperties.user?.timeAvailable!!
            ApplicationProperties.user?.let { sharedPreferences?.saveUser(it) }
            updateRecommender()
            getRecommendedRoute()
            sortItemList()
            if (!isPreferencesSet.value!!) {
                isPreferencesSet.value = true
            }
        }
    }

    fun itemRatingChangeResult(data: Intent?) {
        if (data != null) {
            val rating : Rating? = data.getSerializableExtra(ApplicationProperties.EXTRA_ITEM_RATING)?.let { it as Rating }
            val nextItem : Boolean = data.getBooleanExtra(ApplicationProperties.EXTRA_NEXT_ITEM, false)
            if(nextItem) {
                isCurrentItemVisited.value = true
                (lastItem as Item).isVisited = true
                sharedPreferences?.setRecommendedItem(lastItem as Item)

                if(rating != null) {//rating changed
                    ratingsList.remove(rating)
                    ratingsList.add(rating)
                    updateRecommender()
                }

                if(isJourneyFinished()) {
                    isJourneyFinishedFlag.value = true
                    sharedPreferences?.clear()
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

    /***
     * set app state with the information saved from the last time it was used if the user didn't finish the journey.
     * @return User saved
     */

    fun recoverSavedPreferences(): User? {
        //TODO items already visited
        ApplicationProperties.user = sharedPreferences?.getUser()
        this.startTime = sharedPreferences?.getStartTime()
        if(!ApplicationProperties.userNotDefinedYet() && startTime != null)
            this.isPreferencesSet.value = true
        return ApplicationProperties.user
    }

    fun recoverSavedJourney() {
        sharedPreferences?.getAllRecommendedItemStatus()?.let {list->
            if (list.isNotEmpty()) {
                this.isJourneyBegan.value = true
                list.forEach{recommendedItem ->
                    val item = itemsList.find{it.id == recommendedItem.key}
                    item?.recommendedOrder = recommendedItem.value.first
                    item?.isVisited= recommendedItem.value.second
                }
                sortItemList()
                setNextRecommendedDestination()
            }
        }
    }
}