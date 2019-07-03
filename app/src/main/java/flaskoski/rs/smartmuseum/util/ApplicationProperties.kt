package flaskoski.rs.smartmuseum.util

import android.os.Build
import android.view.View
import flaskoski.rs.smartmuseum.model.User

object ApplicationProperties {
    var user : User? = null

    const val TAG_ITEM_RATING = "itemRating"
    const val TAG_GO_NEXT_ITEM = "nextItem"
    var isDebugOn: Boolean = false
    const val TAG_VISITED_SUBITEMS = "visitedSubItems"
    const val TAG_ARRIVED: String = "arrived"

    fun userNotDefinedYet(): Boolean {
        //TODO check db if there is a current user
        return user == null
    }

    fun bringToFront(view: View, z_value: Float = 20f){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view.translationZ = z_value
            view.bringToFront()
            view.invalidate()
        }
        else {
            view.bringToFront()
            view.parent.requestLayout()
            //sheet_next_items.parent.invalidate()
        }
    }
}