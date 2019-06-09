package flaskoski.rs.smartmuseum.model

import com.google.android.gms.maps.model.LatLng

abstract class Point(
        var id: String = "",
        var lat: Double? = null,
        var lng: Double? = null,
        var isEntrance: Boolean = false,
        var isExit : Boolean = false,
        var oneWay: Boolean = false){


    var distance : Double = Double.MAX_VALUE

    fun getCoordinates(): LatLng? {
        return lat?.let { lng?.let { it1 -> LatLng(it, it1) } }
    }

}