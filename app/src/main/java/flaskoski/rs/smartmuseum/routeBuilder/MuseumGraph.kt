package flaskoski.rs.smartmuseum.routeBuilder

import android.util.Log
import flaskoski.rs.smartmuseum.model.*
import java.io.InvalidClassException
import java.util.*
import kotlin.collections.HashSet

class MuseumGraph(elements: Set<Element>) {
    val vertices = HashSet<Point>()
    val entrances = HashSet<Point>()
    val exits = HashSet<Point>()
    val TAG = "MuseumGraph"


    init{
        elements.filter { it is Point }.forEach {
            vertices.add(it as Point)
            if (it.isEntrance)
                entrances.add(it)
            if (it.isExit)
                exits.add(it)
        }
    }

    fun getClosestItemTo(p: Point): Point?{
        return getNextClosestItemFromList(p, vertices)
    }

    fun getClosestItemTo(p: SubItem) : Point?{
//        if(!(p is Point) ){
//            if(p is SubItem) {
        vertices.find { it.id == p.groupItem }?.let {
            return getNextClosestItemFromList(it, vertices) } ?: throw ParentNotFoundException()
//            }
//            else InvalidClassException("")
//        }
//        return getNextClosestItemFromList(p, vertices)
    }

    private fun calculateMinCost(currentPoint: Point, destinationPoint: Point, edgeCost: Double) {
        val currentPointCost = currentPoint.cost
        if (currentPointCost + edgeCost < destinationPoint.cost) {
            destinationPoint.cost = currentPointCost + edgeCost
            val shortestPath = LinkedList(currentPoint.shortestPath)
            shortestPath.add(destinationPoint)
            destinationPoint.shortestPath = shortestPath
        }
    }

    private fun getLowestCostAmong(notVisitedPoints: HashSet<Point>): Point {
        var lowestCostPoint: Point? = null
        var lowestCost = Double.MAX_VALUE
        for (point in notVisitedPoints) {
            val pointCost = point.cost
            if (pointCost < lowestCost) {
                lowestCost = pointCost
                lowestCostPoint = point
            }
        }
        return lowestCostPoint!!
    }

    fun getNextClosestItemFromList(p: Point, pointsRemaining: Set<Point>) : Point? {
        val notVisitedPoints = HashSet<Point>()
        val visitedPoints = HashSet<Point>()
        vertices.forEach {
            it.cost = Double.MAX_VALUE
            it.shortestPath.clear()
        }
        p.cost = 0.0
        notVisitedPoints.add(p)

        while(notVisitedPoints.isNotEmpty()){
            val currentPoint : Point = getLowestCostAmong(notVisitedPoints)

            //return as result the closest Item that hasn't been seen by the user
            if(currentPoint is Item && pointsRemaining.contains(currentPoint) && !currentPoint.isVisited) {
                currentPoint.shortestPath.addFirst(p) //origin put as the first point of the path
                return currentPoint
            }

            notVisitedPoints.remove(currentPoint)
            currentPoint.adjacentPoints.forEach{adjacentPoint->
                val nextPoint = vertices.find{adjacentPoint.key == it.id}
                if(nextPoint != null) {
                    if (!visitedPoints.contains(nextPoint)) {
                        calculateMinCost(currentPoint, nextPoint, adjacentPoint.value)
                        notVisitedPoints.add(nextPoint)
                    }
                }else
                    Log.w(TAG, "${adjacentPoint.key} from ${currentPoint.id} not found in vertices!")
            }
            visitedPoints.add(currentPoint)
        }
        //no items found (all visited or no items available)
        return null
    }


}
