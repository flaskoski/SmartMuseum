package flaskoski.rs.smartmuseum.model
/**
 * Copyright (c) 2019 Felipe Ferreira Laskoski
 * código fonte licenciado pela MIT License - https://opensource.org/licenses/MIT
 */

/**
 * Elements that have content
 */
interface Itemizable : Element {
    var title: String
    var description: String
    var photoId: String
    var avgRating: Float
    var numberOfRatings: Int
    var recommedationRating: Float
    var timeNeeded : Double
    var isVisited: Boolean
    var isRemoved: Boolean
    var isClosed: Boolean

    fun canConsiderForRouteRecommendation() : Boolean{
        return !isVisited && !isClosed && !isRemoved
    }
}