package flaskoski.rs.smartmuseum.model

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import java.io.Serializable
import java.util.*
import kotlin.collections.HashMap

open class Point(
        override var id: String = "",
        var lat: Double? = null,
        var lng: Double? = null,
        override var isEntrance: Boolean = false,
        override var isExit : Boolean = false,
        var isClosest : Boolean = false,
        override var adjacentPoints : Map<String, Double> = HashMap()): Routable, Itemizable, Serializable {

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