package flaskoski.rs.smartmuseum.model

import android.os.Parcelable
import java.io.Serializable
import java.util.*

class Item(var id: String = "",
           var title : String = "",
           var description : String = "",
           var photoId : String = "",
           var avgRating : Float = 0.0f,
           var numberOfRatings : Int = 0,
           var recommedationRating : Float = 0.0f,
           var contentUri : String = "") : Serializable {

}
