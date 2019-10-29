package flaskoski.rs.smartmuseum.location

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.location.Location
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import flaskoski.rs.smartmuseum.R
import flaskoski.rs.smartmuseum.model.Item
import flaskoski.rs.smartmuseum.model.ItemRepository
import flaskoski.rs.smartmuseum.model.Point
import flaskoski.rs.smartmuseum.util.AnimationUtil
import flaskoski.rs.smartmuseum.util.ApplicationProperties
import java.lang.IllegalStateException
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.GroundOverlayOptions
import com.google.android.gms.maps.model.CameraPosition






class MapManager(private var onUserArrivedToDestinationListener: OnUserArrivedToDestinationListener? = null,
                 private val onMapConfiguredCallback: (() -> Unit)? = null):
        OnMapReadyCallback {

    var mMap : GoogleMap? = null

    private val TAG = "MapManager"
    private var mCurrLocationMarker: Marker? = null
    private var destinationMarker: Marker? = null
    private var routePolyline = RoutePolyline()
    private val scheduledItems: ArrayList<ScheduledItemMarker> = ArrayList()
    var resourceMap : Bitmap? = null
    var mapsActivity: Activity? = null
    set(value) {
        field = value
        if(value == null) return
        mMap?.setOnInfoWindowClickListener(value as GoogleMap.OnInfoWindowClickListener)

        Glide.with(value).asBitmap().load(R.drawable.museum_map3).into(object : CustomTarget<Bitmap>(){
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                resourceMap = resource
                showMuseumMap(resource)
            }
            override fun onLoadCleared(placeholder: Drawable?) {
                // this is called when imageView is cleared on lifecycle call or for some other reason.
                museumMap = null
            }
        })
    }

    var museumMap : GroundOverlay? = null
    private fun showMuseumMap(resource: Bitmap) {
        val museumDetailsMap = GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromBitmap(resource))
                .position(LatLng(-23.65134, -46.62258),  504f, 514.909f)
        museumMap = mMap?.addGroundOverlay(museumDetailsMap)
    }

    override fun onMapReady(p0: GoogleMap?) {
        mMap = p0
//        mMap?.mapType = GoogleMap.MAP_TYPE_SATELLITE
        //mMap?.setMinZoomPreference(14f)
        val cameraPosition = CameraPosition.Builder()
                .target(LatLng(-23.651450,-46.622546))      // Sets the center of the map to Mountain View
                .zoom(19.5f)                   // Sets the zoom
                .bearing(180f)                // Sets the orientation of the camera to east
                .build()                   // Creates a CameraPosition from the builder
        mMap?.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        mMap?.setOnInfoWindowClickListener(mapsActivity as GoogleMap.OnInfoWindowClickListener)

        resourceMap?.let { showMuseumMap(it) }
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
        } else AnimationUtil().animateMarkerTo(mCurrLocationMarker!!, userLatLng)

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
        if(ApplicationProperties.isArrivedIsSet) {
            ApplicationProperties.isArrivedIsSet = false
            return true
        }
        else return distance[0] < 15
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
        val latLng = mCurrLocationMarker?.position
        clearMap()
        latLng?.let { updateUserLocationCallback.invoke(it) }
        setVisitedItems()
        setScheduledItems()
        resourceMap?.let { showMuseumMap(it) }
        val itemPathCoordinates = item.getPathCoordinates()
        itemPathCoordinates.let{itemPath ->
            if(mCurrLocationMarker != null) {
                mMap?.let { map -> routePolyline.addRouteToMap(map, itemPath, mCurrLocationMarker?.position) }
            }else if(lastKnownUserLocation != null)
                mMap?.let { map -> routePolyline.addRouteToMap(map, itemPath, lastKnownUserLocation) }
            else throw IllegalStateException("User map marker is null! Probable cause: User location wasn't found.")

            mMap?.moveCamera(CameraUpdateFactory.newLatLngBounds(LatLngBounds.Builder()
                    .include(mCurrLocationMarker?.position?:lastKnownUserLocation?.let{it}?: previousItem?.getCoordinates()).include(item.getCoordinates()).build(), 130))
            val cameraPosition = mMap?.cameraPosition?.let {
                CameraPosition.builder()
                        .target(it.target)
                        .zoom(it.zoom).bearing(180f).build()
            }
            mMap?.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
            destinationMarker = addItemToMap(item, false)
            alreadyInformed = false

        }
        return this
    }
//
//    override fun onInfoWindowClick(p0: Marker?) {
//        p0?.let{marker ->
//            val scheduledMarker = scheduledItems.find { it.marker == marker }
//            if(scheduledMarker != null) {
//                scheduledMarker.isNextItem = !scheduledMarker.isNextItem
////                onScheduledItemWindowClicked(scheduledMarker.item)
//            }
//        }
//    }

    //TODO Reset scheduled items after item is visited or journey is completed/restarted/canceled
    private fun setScheduledItems() {
        ItemRepository.itemList.filter { it.hasSpecificHours() }.forEach {
            mMap?.let { map -> scheduledItems.add(ScheduledItemMarker(map, it)) }
        }
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
        museumMap = null
        routePolyline.clear()
        return this
    }

    fun goToLocation(location: LatLng) : MapManager {
        mMap?.animateCamera(CameraUpdateFactory.newLatLng(location))
        return this
    }
}