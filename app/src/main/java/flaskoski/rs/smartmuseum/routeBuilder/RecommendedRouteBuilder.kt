package flaskoski.rs.smartmuseum.routeBuilder

import flaskoski.rs.smartmuseum.model.*
import flaskoski.rs.smartmuseum.util.ApplicationProperties.EASTERN_POINT
import flaskoski.rs.smartmuseum.util.ApplicationProperties.NORTHERN_POINT
import flaskoski.rs.smartmuseum.util.ApplicationProperties.SOUTHERN_POINT
import flaskoski.rs.smartmuseum.util.ApplicationProperties.WESTERN_POINT
import java.lang.NullPointerException
import java.util.*

class RecommendedRouteBuilder(elements: Set<Element>){
    private var museumGraph = MuseumGraph(elements)
    private var itemsRemaining = ArrayList<Itemizable>()

    private val MIN_TIME_BETWEEN_ITEMS = 0.4

    @Suppress("UNCHECKED_CAST")
    var allItems : HashSet<Itemizable> = elements.filter { it is Itemizable}.toSet() as HashSet<Itemizable>


    fun getAllEntrances(): Set<Point>? {
        return museumGraph.entrances
    }

    fun getRecommendedRouteFrom(start : Point, timeAvailable : Double, initialCost : Double): List<Itemizable> {
        val startPoint : Point = start
        itemsRemaining.clear()
        allRemainingSubItemsSetRecommendedFalse()
        var totalCost = initialCost
        var count = 0
        //add most recommended Points (items or subItems' parents) that have not been visited yet (plus a min. time between items) until it reaches total available time
        allItems.filter {!it.isVisited && !it.isRemoved}.sortedByDescending { it.recommedationRating }.forEach{ item ->
            if(totalCost + item.timeNeeded + MIN_TIME_BETWEEN_ITEMS < timeAvailable){
                totalCost += item.timeNeeded + MIN_TIME_BETWEEN_ITEMS
//                 if(item is SubItem)
//                    if(itemsRemaining.none{ it.id == item.groupItem })
//                        allItems.find { it.id == item.groupItem }?.let{ parent -> itemsRemaining.add(parent)}
                itemsRemaining.add(item)
                if(!(item is SubItem) )
                    count++ //used next to subtract the time between Points only since it will get the cost from the db
            }
            else return@forEach
        }
        //remove the min_time between items since it will check the determined time (on db) of each route now
        totalCost -= count * MIN_TIME_BETWEEN_ITEMS
//        itemsRemaining.removeLast()

        addNotRecommendedGroupItemsOfRecommendedSubItems()
        moveUpGroupItemsWithSubitemsWithBetterRecommendations()

        var itemsCost = totalCost

        //now will consider the db time to get to each point in the most recommended items list
        //for all points remaining, calculate db time and then if available time is passed, remove the least recommended and try again.
        val allPointsRemainingSize = itemsRemaining.size
        for(i in allPointsRemainingSize downTo 1 ){
            var currentStartPoint = startPoint
            var enoughTime = true
            for(j in 1 .. i) {
                //TODO MEMORIZE ITEMS ROUTE COST

                //time between routableItems only
                var nextPoint : Point? = null
                try {
                    //if no more items to be visited
                    if(itemsRemaining.none { it is RoutableItem && it is Point && !it.isVisited })
                        break
                    @Suppress("UNCHECKED_CAST")
                    nextPoint = museumGraph.getNextClosestItemFromList(currentStartPoint,
                            (itemsRemaining.filter { it is RoutableItem && it is Point }.toSet() as Set<Point>))!!
                }
                catch(e: Exception){
                    //startPoint = null
                    e.printStackTrace()
                }
                if (nextPoint != null) {
                    currentStartPoint = nextPoint
                    if (totalCost + currentStartPoint.cost <= timeAvailable) {
                        totalCost += currentStartPoint.cost
                        (currentStartPoint as Item).isVisited = true
                        currentStartPoint.recommendedOrder = j
                    }
                    else{
                        enoughTime = false
                        break
                    }
                }
            }
            itemsRemaining.filter { it is RoutableItem && it.isVisited }.forEach {
                it.isVisited = false
                if(!enoughTime) (it as RoutableItem).recommendedOrder = Int.MAX_VALUE
            }
            if(enoughTime) {
                itemsRemaining.filter { it is SubItem }.forEach { (it as SubItem).isRecommended = true }
                break
            }
            itemsCost -= (itemsRemaining.last() as Itemizable).timeNeeded
            totalCost = itemsCost
            itemsRemaining.remove(itemsRemaining.last())
        }
        return itemsRemaining
    }

    private fun moveUpGroupItemsWithSubitemsWithBetterRecommendations() {
        val itemsRemainingSet = itemsRemaining.toSet()
        //for all recommended subitems:
        itemsRemainingSet.filter { it is SubItem }.forEach {subitem->
            //if subitem's parent is not in the recommended list
            val parent = itemsRemainingSet.find{ it.id == (subitem as SubItem).groupItem }
            if(parent != null){
                val parentIndex = itemsRemaining.indexOf(parent)
                val childIndex = itemsRemaining.indexOf(subitem)
                if(parentIndex > childIndex){
                    itemsRemaining.remove(parent)
                    itemsRemaining.add(childIndex, parent)
                }
            }
        }
    }

    private fun addNotRecommendedGroupItemsOfRecommendedSubItems() {
        val itemsRemainingSet = itemsRemaining.toHashSet()
        //for all recommended subitems:
        itemsRemainingSet.filter { it is SubItem }.forEach {subitem->
            //if subitem's parent is not in the recommended list
            if(itemsRemainingSet.none{ it.id == (subitem as SubItem).groupItem })
                //find the parent and add it just before its child on the recommended list
                allItems.find { it.id == (subitem as SubItem).groupItem }?.let {groupItem ->
                    itemsRemaining.add( itemsRemaining.indexOf(subitem), groupItem )
                    itemsRemainingSet.add(groupItem)
                }
        }
    }

    private fun allRemainingSubItemsSetRecommendedFalse() {
        ItemRepository.subItemList.filter { !it.isVisited}.forEach { it.isRecommended = false }
    }


    fun findAndSetShortestPath(to: Point, from: Point): Point? {
        return museumGraph.getNextClosestItemFromList(from, setOf(to))
    }

    fun findAndSetShortestPathFromUserLocation(item: Item, userPosition: Point): Point? {
        val closestPoint = getNearestPointFromUser(userPosition)
        return findAndSetShortestPath(item, closestPoint)
    }

    fun getNearestPointFromUser(userPosition: Point): Point {
        if(userPosition.lat == null || userPosition.lng == null) throw NullPointerException("userPosition lat or lng is null!")
        if(isUserWithinLocationBorders(userPosition.lat!!, userPosition.lng!!))
            return museumGraph.getNearestPointFrom(userPosition)
        else return museumGraph.getNearestEntranceFrom(userPosition)
    }

    private fun isUserWithinLocationBorders(lat : Double, lng : Double): Boolean {
        if(lat > SOUTHERN_POINT && lat < NORTHERN_POINT && lng > WESTERN_POINT && lng < EASTERN_POINT)
            return true
        return false
    }

    fun removeItemFromRoute(itemToBeRemoved: Item) {
        itemToBeRemoved.isRemoved = true
        itemToBeRemoved.recommendedOrder = Int.MAX_VALUE
    }
}