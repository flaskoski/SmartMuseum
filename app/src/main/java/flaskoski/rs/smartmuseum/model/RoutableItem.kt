package flaskoski.rs.smartmuseum.model

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import java.util.*

/**
 *
 * Copyright (c) 2019 Felipe Ferreira Laskoski
 * código fonte licenciado pela MIT License - https://opensource.org/licenses/MIT
 * Can be considered on recommended route building for path and as recommendation
 */
interface RoutableItem : Routable, Itemizable {
    var recommendedOrder : Int
    var isAddedToRouteByTheUser : Boolean

    fun isRecommended() : Boolean{
        return recommendedOrder != Int.MAX_VALUE
    }

    fun setNotRecommended(){
        recommendedOrder = Int.MAX_VALUE
    }
}