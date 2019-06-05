package flaskoski.rs.smartmuseum.model

import com.google.android.gms.maps.model.LatLng
import java.io.Serializable
import java.lang.NullPointerException

class Item(var id: String = "",
           var title: String = "",
           var description: String = "",
           var photoId: String = "",
           lat: Double? = null,
           lng: Double? = null,
           var avgRating: Float = 0.0f,
           var numberOfRatings: Int = 0,
           var recommedationRating: Float = 0.0f,
           var contentUri: String = "",
           val isVisited: Boolean = false) : Serializable {
    var coordinates : LatLng? = null
        set(value) {
            value ?: throw NullPointerException()
            field = value
        }

    var lat : Double? = lat
    set(value){
        value ?: throw NullPointerException()
        field = value
        lng?.let {
            coordinates = LatLng(value, it)
        }
    }
    var lng : Double? = lng
        set(value){
            value ?: throw NullPointerException()
            field = value
            lat?.let {
                coordinates = LatLng(it, value)
            }
        }

}
