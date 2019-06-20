package flaskoski.rs.smartmuseum.routeBuilder
import androidx.lifecycle.ViewModel
import android.util.Log
import flaskoski.rs.smartmuseum.model.Item
import flaskoski.rs.smartmuseum.model.Point
import java.util.*

class JourneyManager : ViewModel() {

    var museumGraph: MuseumGraph?  = null
    var closestItem: Point? = null
    var previousItem : Point? = null
    private val TAG = "JourneyManager"
    private var itemList : List<Point>? = null
    private var pointsRemaining = LinkedList<Point>()


    var isPreferencesSet = false
    var isItemsAndRatingsLoaded: Boolean = false
    var isJourneyBegan: Boolean = false
    var timeAvailable: Double = 120.0
    private val timeAlreadySpent: Double? = null

    private val MIN_TIME_BETWEEN_ITEMS: Double = 0.5

    fun build(points: List<Point>, itemList : List<Item>){
        museumGraph = MuseumGraph(points.toHashSet())
        previousItem = museumGraph?.entrances?.first()
        this.itemList = itemList
    }

    private fun isBuilt() : Boolean{
        return museumGraph != null
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



    fun getRecommendedRoute(): LinkedList<Point> {
        if(!isBuilt()) throw Exception("previous point is null. Did you build JourneyManager?")
        pointsRemaining.clear()
        var totalCost = timeAlreadySpent?.let { it } ?: 0.0

        //add most recommended points that have not been visited yet until it reaches total available time
        itemList?.filter { it is Item && !it.isVisited}?.sortedByDescending { (it as Item).recommedationRating }?.forEach{
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
        if(itemList == null) throw Exception("Manager was not built yet!")
        return itemList?.none { (it as Item).isRecommended() && !it.isVisited }!!
    }
}