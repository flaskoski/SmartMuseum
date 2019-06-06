package flaskoski.rs.smartmuseum.location

import android.graphics.Color
import android.location.Location
import android.support.v4.app.FragmentActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import flaskoski.rs.smartmuseum.R
import flaskoski.rs.smartmuseum.model.Item


class MapManager(private val mapActivity: FragmentActivity ) : OnMapReadyCallback {
    var activityCallback : (() -> Unit)? = null
    fun build(activityCallback: (() -> Unit)?): MapManager {
        this.activityCallback = activityCallback
        val mapFragment = mapActivity.supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        return this
    }

    var mMap : GoogleMap? = null
    private var mCurrLocationMarker: Marker? = null
    private var destinationMarker: Marker? = null
    private var destinationPath : Polyline? = null
    private val polyline = PolylineOptions().color(Color.CYAN).width(10f).visible(true).zIndex(30f);

    override fun onMapReady(p0: GoogleMap?) {
        val locationListener : Int

        mMap = p0
        mMap?.mapType = GoogleMap.MAP_TYPE_SATELLITE
        // Add a marker in Sydney and move the camera
        //LatLng sydney = new LatLng(-34, 151);
        //mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap?.setMinZoomPreference(14f)
        //mMap.set
        //  mMap.setLatLngBoundsForCameraTarget(LatLngBounds(LatLng(-23.7, -46.57), LatLng(-23.6, -46.67)))
        mMap?.moveCamera(CameraUpdateFactory.zoomTo(19.5f))
        mMap?.moveCamera(CameraUpdateFactory.newLatLng(LatLng(-23.651450,-46.622546)));
        //Initialize Location
        activityCallback?.invoke()
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
                if(mapActivity is onUserArrivedToDestinationCallback)
                    (mapActivity as onUserArrivedToDestinationCallback).onUserArrivedToDestination()
            }
    }

    private fun isVeryCloseToDestination(userLatLng: LatLng): Boolean {
        val distance = FloatArray(3)
        Location.distanceBetween(userLatLng.latitude, userLatLng.longitude, destinationMarker?.position?.latitude!!, destinationMarker?.position?.longitude!!, distance)
        return distance[0] < 11
    }

    interface onUserArrivedToDestinationCallback{
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

    fun setDestination(item : Item) : MapManager{
        val itemCoordinates = item.getCoordinates()
        itemCoordinates?.let{
            if(mCurrLocationMarker != null) {
                if(destinationPath == null) {//create line
                    polyline.points.clear()
                    polyline.addAll(listOf(mCurrLocationMarker?.position, itemCoordinates))
                    destinationPath = mMap?.addPolyline(polyline)
                }
                else {//update line
                    val points : List<LatLng>? = destinationPath?.points
                    (points as ArrayList).clear()
                    points.add(mCurrLocationMarker!!.position)
                    points.add(itemCoordinates)
                    destinationPath?.points = points
                }
                // .width(1.5f))
                mMap?.moveCamera(CameraUpdateFactory.newLatLngBounds(LatLngBounds.Builder()
                        .include(mCurrLocationMarker?.position).include(itemCoordinates).build(), 40))

                destinationMarker = addItem(item)
                alreadyInformed = false
            }
        }
        return this
    }

    fun isDestinationSet(): Boolean {
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