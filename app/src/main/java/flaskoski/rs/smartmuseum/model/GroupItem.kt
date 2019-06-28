package flaskoski.rs.smartmuseum.model

class GroupItem(
        id : String = "",
        lat: Double? = null,
        lng: Double? = null,
        isEntrance: Boolean = false,
        isExit : Boolean = false,
        override var title: String,
        override var description: String,
        override var photoId: String,
        override var avgRating: Float,
        override var numberOfRatings: Int,
        override var recommedationRating: Float,
        override var timeNeeded: Double,
        override var recommendedOrder: Int,
        override var isVisited: Boolean,
        var subItems: Set<Element>
) : Itemizable, Point(id, lat, lng, isEntrance, isExit)