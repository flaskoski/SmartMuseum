package flaskoski.rs.smartmuseum

import java.util.*

class Item(var title : String, var photoUri : String = "", var rating : Float = 0.0f, var contentUri : String = "") {
    val id = UUID.randomUUID()

}
