package flaskoski.rs.smartmuseum.model

class SubItem(
        override var id: String = "",
        var groupItem : String,
        override var title: String,
        override var description: String,
        override var photoId: String,
        override var avgRating: Float,
        override var numberOfRatings: Int,
        override var recommedationRating: Float,
        override var timeNeeded: Double,
        override var recommendedOrder: Int,
        override var isVisited: Boolean) : Itemizable, HasContent