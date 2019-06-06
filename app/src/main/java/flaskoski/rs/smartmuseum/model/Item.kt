package flaskoski.rs.smartmuseum.model

import com.google.android.gms.maps.model.LatLng
import java.io.Serializable
import java.lang.NullPointerException

class Item(var id: String = "",
           var title: String = "",
           var description: String = "",
           var photoId: String = "",
           var lat: Double? = null,
           var lng: Double? = null,
           var avgRating: Float = 0.0f,
           var numberOfRatings: Int = 0,
           var recommedationRating: Float = 0.0f,
           var contentUri: String = "",
           var isVisited: Boolean = false) : Serializable {

    fun getCoordinates(): LatLng? {
        return lat?.let { lng?.let { it1 -> LatLng(it, it1) } }
    }
}
