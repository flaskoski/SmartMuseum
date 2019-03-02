package flaskoski.rs.smartmuseum.model

import java.util.*

class Item(var id: String,
           var title : String,
           var photoUri : String = "",
           var avgRating : Float = 0.0f,
           var recommedationRating : Float = 0.0f,
           var contentUri : String = "") {

}
