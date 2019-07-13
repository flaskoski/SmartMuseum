package flaskoski.rs.smartmuseum.model

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import java.util.*

/**
 * Can be considered on recommended route building for path and as recommendation
 */
interface RoutableItem : Routable, Itemizable {
    var recommendedOrder : Int

    fun isRecommended() : Boolean{
        return recommendedOrder != Int.MAX_VALUE
    }
}