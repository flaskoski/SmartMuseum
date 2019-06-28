package flaskoski.rs.smartmuseum.util

import flaskoski.rs.smartmuseum.model.User

object ApplicationProperties {
    var user : User? = null

    val EXTRA_ITEM_RATING = "itemRating"
    val EXTRA_NEXT_ITEM = "nextItem"

    fun userNotDefinedYet(): Boolean {
        //TODO check db if there is a current user
        return user == null
    }
}