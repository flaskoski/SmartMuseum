package flaskoski.rs.smartmuseum.model

class Rating(val user : String ="", val item : String = "", val rating : Float = 0F) {
    override fun toString(): String {
        return "$user $item ${rating.toString()}"
    }
}