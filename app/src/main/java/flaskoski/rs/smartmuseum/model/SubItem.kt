package flaskoski.rs.smartmuseum.model

import java.io.Serializable
/**
 * Copyright (c) 2019 Felipe Ferreira Laskoski
 * código fonte licenciado pela MIT License - https://opensource.org/licenses/MIT
 */

//Non-routable item (cannot be considered on route building but has content and rating)
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
        override var timeNeeded: Double = 2.5,
        override var isVisited: Boolean = false,
        override var isRemoved: Boolean = false,
        override var isClosed: Boolean = false) : Itemizable, Serializable{

    override fun toString(): String {
        return "${this.id} ${this.title} ${this.recommedationRating}"
    }
}