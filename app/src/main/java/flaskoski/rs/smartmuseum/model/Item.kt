package flaskoski.rs.smartmuseum.model

/**
 * Routable items (have content, can be rated and considered on recommended route) with LatLng coordinates
 * @param timeNeeded to see the item (in minutes)
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
           override var isClosed: Boolean = false) : RoutableItem, Point(id, lat, lng, isEntrance, isExit){


    override fun toString(): String {
        return super.toString() + " ${this.title}"
    }
}