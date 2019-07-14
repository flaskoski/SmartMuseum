package flaskoski.rs.smartmuseum.location

import android.graphics.Color
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import java.util.*
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Dot
import com.google.android.gms.maps.model.PatternItem



class RoutePolyline {
    private val polylineOptions = PolylineOptions().color(Color.CYAN).width(10f).visible(true).zIndex(30f);
    private val userPolylineOptions = PolylineOptions().color(Color.CYAN).width(10f).visible(true).zIndex(30f);
    private var destinationPathPolyline: Polyline? = null
    private var userToPathPolyline: Polyline? = null


    fun addRouteToMap(map: GoogleMap, itemPath: LinkedList<LatLng>, userPosition: LatLng? = null) {
        destinationPathPolyline = updatePolyline(map, itemPath, destinationPathPolyline, polylineOptions)
        if(userPosition != null) {
            userToPathPolyline = updatePolyline(map, listOf(userPosition, itemPath.first), userToPathPolyline, userPolylineOptions)
            val pattern = Arrays.asList(
                    Dot(), Gap(20f))
            userToPathPolyline?.pattern = pattern
        }else userToPathPolyline = null
    }

    fun updatePolyline(map: GoogleMap, path : List<LatLng>, line : Polyline?, lineOptions : PolylineOptions): Polyline? {
        if(line == null)
        {//create line
            lineOptions.points.clear()
            //polylineOptions.add(mCurrLocationMarker?.position)
            lineOptions.addAll(path)

            return map.addPolyline(lineOptions)
        }
        else
        {//update line
            val points = ArrayList<LatLng>()
            points.addAll(path)
            line.points.clear()
            line.points = points
            return line
        }
    }

    fun clear() {
        if(destinationPathPolyline != null) destinationPathPolyline = null
        if(userToPathPolyline != null) userToPathPolyline = null
    }
}