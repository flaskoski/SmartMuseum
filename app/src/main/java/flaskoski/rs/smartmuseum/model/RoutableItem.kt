package flaskoski.rs.smartmuseum.model

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import java.util.*

interface RoutableItem : Routable, Itemizable {
    var recommendedOrder : Int

    fun isRecommended() : Boolean{
        return recommendedOrder != Int.MAX_VALUE
    }
}