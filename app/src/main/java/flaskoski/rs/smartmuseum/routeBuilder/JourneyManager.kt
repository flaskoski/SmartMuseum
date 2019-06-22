package flaskoski.rs.smartmuseum.routeBuilder
import android.app.Activity
import androidx.lifecycle.ViewModel
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.SupportMapFragment
import flaskoski.rs.rs_cf_test.recommender.RecommenderBuilder
import flaskoski.rs.smartmuseum.DAO.ItemDAO
import flaskoski.rs.smartmuseum.DAO.RatingDAO
import flaskoski.rs.smartmuseum.activity.MainActivity
import flaskoski.rs.smartmuseum.location.MapManager
import flaskoski.rs.smartmuseum.model.Item
import flaskoski.rs.smartmuseum.model.Point
import flaskoski.rs.smartmuseum.model.Rating
import flaskoski.rs.smartmuseum.model.UserLocationManager
import flaskoski.rs.smartmuseum.recommender.RecommenderManager
import flaskoski.rs.smartmuseum.util.ApplicationProperties
import flaskoski.rs.smartmuseum.util.ParallelRequestsManager
import java.util.*

class JourneyManager() : ViewModel() ,
        MapManager.OnUserArrivedToDestinationListener {

    var museumGraph: MuseumGraph?  = null
    var closestItem: Point? = null
    var previousItem : Point? = null
    private val TAG = "JourneyManager"
    var itemsList : List<Item> = ArrayList()
    var ratingsList  = HashSet<Rating>()
    var currentItem : Item? = null

    private var pointsRemaining = LinkedList<Point>()

    var isCloseToItem = MutableLiveData<Boolean>()
    var itemListChangedListener : (()->Unit)? = null
    var isPreferencesSet = false
    var isItemsAndRatingsLoaded: Boolean = false
    var isJourneyBegan: Boolean = false
    var timeAvailable: Double = 120.0
    private val timeAlreadySpent: Double? = null

    private val MIN_TIME_BETWEEN_ITEMS = 0.5

    var mapManager: MapManager? = null
    var userLocationManager : UserLocationManager? = null

    val REQUEST_CHANGE_LOCATION_SETTINGS = 3

    init {
        //live data vars initialization
        isCloseToItem.value = false

        //maps setup
        userLocationManager = UserLocationManager(REQUEST_CHANGE_LOCATION_SETTINGS)
        mapManager = MapManager(this)
        userLocationManager?.onUserLocationUpdateCallback = mapManager?.updateUserLocationCallback
    }

    private var activity: Activity? = null
    fun updateActivity(activity : Activity) {
        this.activity = activity
        userLocationManager?.updateActivity(activity)
    }

    fun buildGraph(points: List<Point>, itemList: List<Item>){
        museumGraph = MuseumGraph(points.toHashSet())
        previousItem = museumGraph?.entrances?.first()
    }

    private fun isGraphBuilt() : Boolean{
        return museumGraph != null
    }

    fun updateRecommender() {
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

    fun sortItemList() {
        var i = 0
        itemsList.sortedWith(compareBy<Item>{it.isVisited}.thenBy{ it.recommendedOrder}).forEach{
            (itemsList as java.util.ArrayList<Item>)[i++] = it
        }

        itemListChangedListener?.invoke()
    }

    private fun mainGetRecommendedRoute() {
        if (isJourneyBegan)
            try {getRecommendedRoute()} catch (e: java.lang.Exception) { e.printStackTrace() }
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
                isItemsAndRatingsLoaded = true
                buildRecommender()

            }
        }
        val ratingDAO = RatingDAO()
        ratingDAO.getAllItems {
            ratingsList.addAll(it)
            getItemsAndRatingsBeforeRecommend.decreaseRemainingRequests()
            if (getItemsAndRatingsBeforeRecommend.isComplete) {
                isItemsAndRatingsLoaded = true
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

        previousItem = this.closestItem ?: museumGraph?.entrances?.first()
        previousItem?.isClosest = false
        closestItem = museumGraph?.getClosestItemTo(previousItem!!)
        closestItem?.isClosest = true
        return closestItem
    }

    override fun onUserArrivedToDestination() {
        isCloseToItem.value = true
    }

    fun getRecommendedRoute(): LinkedList<Point> {
        if(!isGraphBuilt()) throw Exception("previous point is null. Did you buildGraph JourneyManager?")
        pointsRemaining.clear()
        var totalCost = timeAlreadySpent?.let { it } ?: 0.0

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
            var startPoint = previousItem
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
        isJourneyBegan = true
        getRecommendedRoute()
        sortItemList()
        setNextRecommendedDestination()
    }
    fun setNextRecommendedDestination() {
        var item : Item? = null
        if(!itemsList[0].isVisited && itemsList[0].recommendedOrder != Int.MAX_VALUE)
            item = itemsList[0]

        if(item != null){
            previousItem?.let { findAndSetShortestPath(item, it) }
            try{
                mapManager?.setDestination(item, previousItem)
            }
            catch(e: java.lang.Exception){
                e.printStackTrace()
//                Toast.makeText(applicationContext, "Erro ao carregar posição.", Toast.LENGTH_SHORT)
            }
            previousItem = item
        }
        else {
            Log.w(TAG, "Todos os itens já foram visitados e setNextRecommendedDestination foi chamado.")
//            Toast.makeText(applicationContext, "Todos os itens já foram visitados.", Toast.LENGTH_LONG).show()
        }
    }
}