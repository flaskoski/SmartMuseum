package flaskoski.rs.smartmuseum.model

import java.io.Serializable

class SubItem(
        override var id: String = "",
        var groupItem: String? = null,
        var isRecommended: Boolean = false,
        override var title: String = "",
        override var description: String = "",
        override var photoId: String = "",
        override var avgRating: Float = 0f,
        override var numberOfRatings: Int = 0,
        override var recommedationRating: Float = 3f,
        override var timeNeeded: Double = 5.0,
        override var isVisited: Boolean = false) : Itemizable, Serializable