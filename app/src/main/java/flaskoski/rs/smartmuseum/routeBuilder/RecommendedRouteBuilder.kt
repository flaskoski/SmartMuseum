package flaskoski.rs.smartmuseum.routeBuilder

import flaskoski.rs.smartmuseum.model.*
import java.util.*

class RecommendedRouteBuilder(elements: Set<Element>){
    private var museumGraph: MuseumGraph?  = null
    private var itemsRemaining = LinkedList<Itemizable>()

    private val MIN_TIME_BETWEEN_ITEMS = 0.4

    @Suppress("UNCHECKED_CAST")
    var allItems : Set<Itemizable> = elements.filter { it is Itemizable}.toSet() as Set<Itemizable>

    init {
        museumGraph = MuseumGraph(elements)
    }

    fun getAllEntrances(): Set<Point>? {
        return museumGraph?.entrances
    }

    fun getRecommendedRouteFrom(start : Point, timeAvailable : Double, initialCost : Double): LinkedList<Itemizable> {
        var startPoint = start
        itemsRemaining.clear()
        var totalCost = initialCost
        var count = 0
        //add most recommended Points (items or subItems' parents) that have not been visited yet (plus a min. time between items) until it reaches total available time
        allItems.filter {!it.isVisited}.sortedByDescending { it.recommedationRating }.forEach{ item ->
            if(totalCost + item.timeNeeded + MIN_TIME_BETWEEN_ITEMS < timeAvailable){
                totalCost += item.timeNeeded + MIN_TIME_BETWEEN_ITEMS
//                if(item is SubItem)
//                    if(itemsRemaining.none{ it.id == item.groupItem })
//                        allItems.find { it.id == item.groupItem }?.let{ parent -> itemsRemaining.add(parent)}
                itemsRemaining.add(item)
                if(!(item is SubItem) )
                    count++ //used next to subtract the time between Points only since it will get the cost from the db
            }
            else return@forEach
        }
        //remove the min_time between items since it will check the determined time (on db) of each route now
        totalCost -= (count-1) * MIN_TIME_BETWEEN_ITEMS
//        itemsRemaining.removeLast()
        var itemsCost = totalCost

        //now will consider the db time to get to each point in the most recommended items list
        //for all points remaining, calculate db time and then if available time is passed, remove the least recommended and try again.
        val allPointsRemainingSize = itemsRemaining.size
        for(i in allPointsRemainingSize downTo 1 ){
            var enoughTime = true
            for(j in 1 .. i) {
                //TODO MEMORIZE ITEMS ROUTE COST
                @Suppress("UNCHECKED_CAST")
                //time between routableItems only
                startPoint = museumGraph?.getNextClosestItemFromList(startPoint,
                        (itemsRemaining.subList(0, i).filter { it is RoutableItem }.toSet() as Set<Point>))!!
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
            itemsRemaining.filter { it is RoutableItem && it.isVisited }.forEach {
                it.isVisited = false
                if(!enoughTime) (it as RoutableItem).recommendedOrder = Int.MAX_VALUE
            }
            if(enoughTime)
                break
            itemsCost -= (itemsRemaining.last as Itemizable).timeNeeded
            totalCost = itemsCost
            itemsRemaining.removeLast()
        }
        return itemsRemaining
    }


    fun findAndSetShortestPath(to: Point, from: Point): Point? {
        return museumGraph?.getNextClosestItemFromList(from, setOf(to))
    }

    private fun isGraphBuilt() : Boolean{
        return museumGraph != null
    }
}