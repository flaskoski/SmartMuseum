package flaskoski.rs.smartmuseum.model

interface Itemizable {
    var title: String
    var description: String
    var photoId: String
    var avgRating: Float
    var numberOfRatings: Int
    var recommedationRating: Float
    var timeNeeded : Double
    var recommendedOrder : Int
    var isVisited: Boolean

    fun isRecommended() : Boolean{
        return recommendedOrder != Int.MAX_VALUE
    }
}