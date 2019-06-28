package flaskoski.rs.smartmuseum.model

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import java.util.*

interface Routable {
    var isEntrance: Boolean
    var isExit : Boolean
    var adjacentPoints : Map<String, Double>
}