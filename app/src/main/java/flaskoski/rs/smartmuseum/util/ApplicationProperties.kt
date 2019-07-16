package flaskoski.rs.smartmuseum.util

import android.os.Build
import android.view.View
import com.google.android.gms.maps.model.LatLng
import flaskoski.rs.smartmuseum.model.User

object ApplicationProperties {
    //Custom application constants
    var isDebugOn: Boolean = false
    const val WESTERN_POINT : Double = -46.623196
    const val EASTERN_POINT : Double = -46.620273
    const val NORTHERN_POINT : Double = -23.649290
    const val SOUTHERN_POINT : Double = -23.652896

    //technical constants
    const val TAG_ITEM_RATING = "itemRating"
    const val TAG_GO_NEXT_ITEM = "nextItem"
    const val TAG_VISITED_SUBITEMS = "visitedSubItems"
    const val TAG_ARRIVED: String = "arrived"
    const val USER_LOCATION_ITEM_ID: String = "userLocation"
    const val SYSTEM_USER_BASED = "user_based"

    var user : User? = null
    val recommendationSystem: String = SYSTEM_USER_BASED

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

    fun resetConfigurations() {
        user = null
    }
}