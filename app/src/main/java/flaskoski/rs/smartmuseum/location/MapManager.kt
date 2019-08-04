package flaskoski.rs.smartmuseum.location

import android.location.Location
import android.util.Log
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import flaskoski.rs.smartmuseum.R
import flaskoski.rs.smartmuseum.model.Item
import flaskoski.rs.smartmuseum.model.ItemRepository
import flaskoski.rs.smartmuseum.model.Point
import flaskoski.rs.smartmuseum.util.ApplicationProperties
import java.lang.IllegalStateException


class MapManager(private var onUserArrivedToDestinationListener: OnUserArrivedToDestinationListener? = null,
                 private val onMapConfiguredCallback: (() -> Unit)? = null): OnMapReadyCallback {

    var mMap : GoogleMap? = null
    private val TAG = "MapManager"
    private var mCurrLocationMarker: Marker? = null
    private var destinationMarker: Marker? = null
    private var routePolyline = RoutePolyline()

    override fun onMapReady(p0: GoogleMap?) {
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
            return distance[0] < 31000
        else return distance[0] < 12
    }

    interface OnUserArrivedToDestinationListener{
        fun onUserArrivedToDestination()
    }

    fun addItemList(itemList: List<Item>): MapManager {
        for(item in itemList)
            item.getCoordinates()?.let {mMap?.addMarker( MarkerOptions().position(it).title(item.title)) }
        return this
    }

    fun addItemToMap(item: Item, isVisitedItem: Boolean) : Marker? {
        item.getCoordinates()?.let {
            if(isVisitedItem)
                return mMap?.addMarker(MarkerOptions().position(it).title(item.title).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)))
            return mMap?.addMarker(MarkerOptions().position(it).title(item.title).icon(BitmapDescriptorFactory.defaultMarker()))
        }
        return null
    }

    fun setDestination(item: Item, previousItem: Point?, lastKnownUserLocation : LatLng? = null) : MapManager{
        clearMap()
        setVisitedItems()

        val itemPathCoordinates = item.getPathCoordinates()
        itemPathCoordinates.let{itemPath ->
            if(mCurrLocationMarker != null) {
                mMap?.let { map -> routePolyline.addRouteToMap(map, itemPath, mCurrLocationMarker?.position) }
            }else if(lastKnownUserLocation != null)
                mMap?.let { map -> routePolyline.addRouteToMap(map, itemPath, lastKnownUserLocation) }
            else throw IllegalStateException("User map marker is null! Probable cause: User location wasn't found.")

            mMap?.moveCamera(CameraUpdateFactory.newLatLngBounds(LatLngBounds.Builder()
                    .include(mCurrLocationMarker?.position?:lastKnownUserLocation?.let{it}?: previousItem?.getCoordinates()).include(item.getCoordinates()).build(), 130))
            destinationMarker = addItemToMap(item, false)
            alreadyInformed = false

        }
        return this
    }

    private fun setVisitedItems() {
        ItemRepository.itemList.filter { it.isVisited }.forEach {
            addItemToMap(it, true)
        }
    }

    private fun isDestinationSet(): Boolean {
        return destinationMarker != null
    }

    fun clearMap() : MapManager{
        mMap?.clear()
        destinationMarker = null
        mCurrLocationMarker = null
        routePolyline.clear()
        return this
    }

    fun goToLocation(location: LatLng) : MapManager {
        mMap?.animateCamera(CameraUpdateFactory.newLatLng(location))
        return this
    }
}