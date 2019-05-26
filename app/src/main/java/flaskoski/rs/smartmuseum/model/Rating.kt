package flaskoski.rs.smartmuseum.model

import java.io.Serializable

class Rating(val user : String ="", val item : String = "", val rating : Float = 0F, val type : String = TYPE_ITEM) : Serializable{
    companion object {
        val TYPE_ITEM = "item";
        val TYPE_FEATURE = "feature";
    }

    override fun toString(): String {
        return "$user $item ${rating.toString()}"
    }
}