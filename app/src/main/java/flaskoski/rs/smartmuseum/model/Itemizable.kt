package flaskoski.rs.smartmuseum.model

interface Itemizable : Element {
    var title: String
    var description: String
    var photoId: String
    var avgRating: Float
    var numberOfRatings: Int
    var recommedationRating: Float
    var timeNeeded : Double
    var isVisited: Boolean
}