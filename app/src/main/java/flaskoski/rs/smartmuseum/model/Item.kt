package flaskoski.rs.smartmuseum.model

import android.util.Log
import java.io.Serializable

/**
 * @param timeNeeded to see the item (in minutes)
 */

class Item(id: String = "",
           var title: String = "",
           var description: String = "",
           var photoId: String = "",
                lat: Double? = null,
                lng: Double? = null,
           var avgRating: Float = 0.0f,
           var numberOfRatings: Int = 0,
           var recommedationRating: Float = 0.0f,
                isEntrance: Boolean = false,
           var timeNeeded : Double = 5.0,
           var recommendedOrder : Int = Int.MAX_VALUE,
                isExit : Boolean = true,
           //var contentUri: String = "",
           var isVisited: Boolean = false) : Point(id, lat, lng, isEntrance, isExit){


    override fun toString(): String {
        return super.toString() + " ${this.title}"
    }
}