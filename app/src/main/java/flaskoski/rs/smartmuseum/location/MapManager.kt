package flaskoski.rs.smartmuseum.location

import android.location.Location
import android.support.v4.app.FragmentActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import flaskoski.rs.smartmuseum.R
import flaskoski.rs.smartmuseum.model.Item

class MapManager(val activity: FragmentActivity) : OnMapReadyCallback {
    fun build(): MapManager {
        val mapFragment = activity.supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        return this
    }

    var mMap : GoogleMap? = null
    private var mCurrLocationMarker: Marker? = null

    override fun onMapReady(p0: GoogleMap?) {
        val locationListener : Int

        mMap = p0
        mMap?.mapType = GoogleMap.MAP_TYPE_SATELLITE
        // Add a marker in Sydney and move the camera
        //LatLng sydney = new LatLng(-34, 151);
        //mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap?.setMinZoomPreference(16f)
        //mMap.set
        //  mMap.setLatLngBoundsForCameraTarget(LatLngBounds(LatLng(-23.7, -46.57), LatLng(-23.6, -46.67)))
        mMap?.moveCamera(CameraUpdateFactory.zoomTo(19.5f))
        mMap?.moveCamera(CameraUpdateFactory.newLatLng(LatLng(-23.651450,-46.622546)));
        //Initialize Location

    }

    /**
     * function to be called after getting last user position
     */
    val updateUserLocationCallback = { userLatLng : LatLng ->
        if (mCurrLocationMarker == null) {
            val markerOptions = MarkerOptions().position(userLatLng).title("Sua posição")
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.blue_circle))
            mCurrLocationMarker = mMap?.addMarker(markerOptions)
        } else mCurrLocationMarker?.position = userLatLng
    }

    fun updateItemsListOnMap(itemList : List<Item> ) {
        mMap?.clear()
        if(mCurrLocationMarker != null) mCurrLocationMarker = null
        for(item in itemList)
            item.coordinates?.let {  mMap?.addMarker(MarkerOptions().position(it).title(item.title)) }
    }

    fun goToLocation(location: Location) {
        mMap?.animateCamera(CameraUpdateFactory.newLatLng(LatLng(location.latitude, location.longitude)))
    }
}