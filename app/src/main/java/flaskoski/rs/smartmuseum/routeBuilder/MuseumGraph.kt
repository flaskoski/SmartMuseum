package flaskoski.rs.smartmuseum.routeBuilder

import android.util.Log
import flaskoski.rs.smartmuseum.model.Item
import flaskoski.rs.smartmuseum.model.Point
import java.util.*
import kotlin.collections.HashSet

class MuseumGraph(val vertices: Set<Point>) {
    val entrances: Set<Point> = HashSet()
    val exits : Set<Point> = HashSet()
    val TAG = "MuseumGraph"


    init{
        for(vertex in vertices){
            if (vertex.isEntrance)
                (entrances as HashSet).add(vertex)
            if (vertex.isExit)
                (exits as HashSet).add(vertex)
        }
    }

    fun getClosestItemTo(p: Point): Point? {
        return getNextClosestItemFromList(p, vertices)
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
