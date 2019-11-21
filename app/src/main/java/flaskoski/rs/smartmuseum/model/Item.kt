package flaskoski.rs.smartmuseum.model

import flaskoski.rs.smartmuseum.util.ParseTime
import java.util.*
import kotlin.collections.ArrayList
/**
 * Copyright (c) 2019 Felipe Ferreira Laskoski
 * código fonte licenciado pela MIT License - https://opensource.org/licenses/MIT
 */
/**
 * Routable items (have content, can be rated and considered on recommended route) with LatLng coordinates
 * @param timeNeeded to see the item (in minutes)
 * @param hours list of hours the item will be presented/working
 * @param onlyIfRecommended if true, it will only show on map if it was considered among the recommended items of the route
 */

open class Item(id: String = "",
                var hintText : String = "",
                override var title: String = "",
                override var description: String = "",
                override var photoId: String = "",
                lat: Double? = null,
                lng: Double? = null,
                override var avgRating: Float = 0.0f,
                override var numberOfRatings: Int = 0,
                override var recommedationRating: Float = 3f,
                isEntrance: Boolean = false,
                isExit : Boolean = false,
                override var timeNeeded : Double = 5.0,
                override var recommendedOrder : Int = Int.MAX_VALUE,
           //var contentUri: String = "",
                override var isVisited: Boolean = false,
                override var isRemoved: Boolean = false,
                override var isClosed: Boolean = false,
                val onlyIfRecommended : Boolean = false,
                val item : String = "",
                hours: List<String>? = null,
                var timeHours : List<Date>? = null,
                override var isAddedToRouteByTheUser: Boolean = false):
        RoutableItem, Point(id, lat, lng, isEntrance, isExit){

    var hours : List<String>? = hours
    set(value) {
        field = value
        timeHours = getTimeOfStrings(value)
    }

    fun hasSpecificHours() : Boolean{
        return timeHours?.isNotEmpty()?.let{it} ?: false
    }

    private fun getTimeOfStrings(hours: List<String>?): List<Date>? {
        if (hours == null) return null

        val convertedHours = ArrayList<Date>()
        for(hourString in hours){
            ParseTime.hourStringToDate(hourString)?.let { convertedHours.add(it) }
        }

        return convertedHours.sortedWith(compareBy { it.time })
    }

    override fun toString(): String {
        return super.toString() + " ${this.title} ${this.recommedationRating}"
    }

    fun getNextHour(): Date? {
        if(timeHours == null) return null
        for(hour in timeHours!!){
            if(hour > ParseTime.getCurrentTime())
                return hour
        }
        return null
    }
}