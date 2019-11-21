package flaskoski.rs.smartmuseum.model

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import java.util.*

/**
 *
 * Copyright (c) 2019 Felipe Ferreira Laskoski
 * código fonte licenciado pela MIT License - https://opensource.org/licenses/MIT
 *
 * Can be considered on route recommendation building for path purpose only
 */
interface Routable {
    var isEntrance: Boolean
    var isExit : Boolean
    var adjacentPoints : Map<String, Double>
}