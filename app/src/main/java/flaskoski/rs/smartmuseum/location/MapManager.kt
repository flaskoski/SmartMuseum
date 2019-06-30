package flaskoski.rs.smartmuseum.location

import android.graphics.Color
import android.location.Location
import android.util.Log
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import flaskoski.rs.smartmuseum.R
import flaskoski.rs.smartmuseum.model.Item
import flaskoski.rs.smartmuseum.model.Point
import flaskoski.rs.smartmuseum.util.ApplicationProperties
import java.lang.IllegalStateException


class MapManager(private var onUserArrivedToDestinationListener: OnUserArrivedToDestinationListener? = null,
                 private val onMapConfiguredCallback: (() -> Unit)? = null): OnMapReadyCallback {

    var mMap : GoogleMap? = null
    private val TAG = "MapManager"
    private var mCurrLocationMarker: Marker? = null
    private var destinationMarker: Marker? = null
    private var destinationPath : Polyline? = null
    private val polyline = PolylineOptions().color(Color.CYAN).width(10f).visible(true).zIndex(30f);

    override fun onMapReady(p0: GoogleMap?) {
        val locationListener : Int

        mMap = p0
        mMap?.mapType = GoogleMap.MAP_TYPE_SATELLITE
        mMap?.setMinZoomPreference(14f)
        mMap?.moveCamera(CameraUpdateFactory.zoomTo(19.5f))
        mMap?.moveCamera(CameraUpdateFactory.newLatLng(LatLng(-23.651450,-46.622546)));

        //map ready ->  callback
        onMapConfiguredCallback?.invoke()
    }

    private var alreadyInformed: Boolean = false
    /**
     * function to be called after getting last user position
     */
    val updateUserLocationCallback = { userLatLng : LatLng ->
        if (mCurrLocationMarker == null) {
            val markerOptions = MarkerOptions().position(userLatLng).title("Sua posição")
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.blue_circle))
            mCurrLocationMarker = mMap?.addMarker(markerOptions)
        } else mCurrLocationMarker?.position = userLatLng

        if(isDestinationSet())
            if(!alreadyInformed && isVeryCloseToDestination(userLatLng)) { // < 10 meters
                alreadyInformed = true
                if(onUserArrivedToDestinationListener != null)
                    onUserArrivedToDestinationListener?.onUserArrivedToDestination()
                else Log.w(TAG, "onUserArrived state reached but it's callback is null")
            }
    }

    private fun isVeryCloseToDestination(userLatLng: LatLng): Boolean {
        val distance = FloatArray(3)
        Location.distanceBetween(userLatLng.latitude, userLatLng.longitude, destinationMarker?.position?.latitude!!, destinationMarker?.position?.longitude!!, distance)
        if(ApplicationProperties.isDebugOn)
            return distance[0] < 11000
        else return distance[0] < 11
    }

    interface OnUserArrivedToDestinationListener{
        fun onUserArrivedToDestination()
    }

    fun addItemList(itemList: List<Item>): MapManager {
        for(item in itemList)
            item.getCoordinates()?.let {mMap?.addMarker( MarkerOptions().position(it).title(item.title)) }
        return this
    }

    fun addItem(item : Item) : Marker? {
        item.getCoordinates()?.let { return mMap?.addMarker(MarkerOptions().position(it).title(item.title)) }
        return null
    }

    fun setDestination(item: Item, previousItem: Point?) : MapManager{
        val itemPathCoordinates = item.getPathCoordinates()
        itemPathCoordinates?.let{
            if(mCurrLocationMarker != null) {
                if(destinationPath == null) {//create line
                    polyline.points.clear()
                    //polyline.add(mCurrLocationMarker?.position)
                    polyline.addAll(itemPathCoordinates)
                    destinationPath = mMap?.addPolyline(polyline)
                }
                else {//update line
                    val points : List<LatLng>? = destinationPath?.points
                    (points as ArrayList).clear()
                    //points.add(mCurrLocationMarker!!.position)
                    points.addAll(itemPathCoordinates)
                    destinationPath?.points = points
                }
                // .width(1.5f))
                mMap?.moveCamera(CameraUpdateFactory.newLatLngBounds(LatLngBounds.Builder()
                        .include(previousItem?.getCoordinates()).include(item.getCoordinates()).build(), 130))
                        //.include(mCurrLocationMarker?.position).include(item.getCoordinates()).build(), 130))

                if(destinationMarker != null) //last marker changed to green
                    destinationMarker?.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                destinationMarker = addItem(item)
                alreadyInformed = false
            }else
                throw IllegalStateException("User map marker is null! Probable cause: User location wasn't found.")
        }
        return this
    }

    private fun isDestinationSet(): Boolean {
        return destinationMarker != null
    }

    fun clearMap() : MapManager{
        mMap?.clear()
        if(destinationMarker != null) destinationMarker = null
        if(mCurrLocationMarker != null) mCurrLocationMarker = null
        if(destinationPath != null) destinationPath = null
        return this
    }

    fun goToLocation(location: Location) : MapManager {
        mMap?.animateCamera(CameraUpdateFactory.newLatLng(LatLng(location.latitude, location.longitude)))
        return this
    }
}