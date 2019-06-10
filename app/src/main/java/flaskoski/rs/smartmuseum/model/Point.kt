package flaskoski.rs.smartmuseum.model

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import java.util.*
import kotlin.collections.HashMap

open class Point(
        var id: String = "",
        var lat: Double? = null,
        var lng: Double? = null,
        var isEntrance: Boolean = false,
        var isExit : Boolean = false,
        var isClosest : Boolean = false,
        var adjacentPoints : Map<String, Double> = HashMap()){


    var cost : Double = Double.MAX_VALUE
    var shortestPath = LinkedList<Point>()

    fun getCoordinates(): LatLng? {
        return lat?.let { lng?.let { it1 -> LatLng(it, it1) } }
    }

    override fun toString(): String {
        return super.toString() + " ${this.id}"
    }

    fun getPathCoordinates(): LinkedList<LatLng> {
        val coordinates = LinkedList<LatLng>()
        shortestPath.forEach {
            it.getCoordinates()?.let { it1 -> coordinates.add(it1) } ?: Log.w("Model.Point", "${it.id} doesn't have coordinates!")
        }
        if(coordinates.isEmpty()) Log.w("Model.Point", "${this.id} getPathCoordinates called, but path is empty!")
        return coordinates
    }

}