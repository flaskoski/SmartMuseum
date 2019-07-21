package flaskoski.rs.smartmuseum.model

import com.google.android.gms.maps.model.LatLng

class User(val id: String, var name: String, var timeAvailable: Double, var location : LatLng? = null) {


}
