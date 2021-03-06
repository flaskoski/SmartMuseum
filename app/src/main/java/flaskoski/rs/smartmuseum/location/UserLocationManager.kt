package flaskoski.rs.smartmuseum.location

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.util.Log
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
/**
 * Copyright (c) 2019 Felipe Ferreira Laskoski
 * código fonte licenciado pela MIT License - https://opensource.org/licenses/MIT
 */
class UserLocationManager(private val REQUEST_CHANGE_LOCATION_SETTINGS: Int) : LocationCallback() {


    private var locationManager: LocationManager? = null
    private var mFusedLocationClient: FusedLocationProviderClient? = null
    private var locationSettingsCorrect: Boolean = false
    private var locationRequest: LocationRequest? = null

    private val TAG: String = "UserLocationManager"

    //function that handles the user location returned
    var onUserLocationUpdateCallbacks = ArrayList<((LatLng)->Unit)?>()

    var activity: Activity? = null

    fun updateActivity(activity: Activity) {
        this.activity = activity
        updateVariablesActivity()
    }

    private fun updateVariablesActivity(){
        locationManager = activity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(activity!!)
        createLocationRequest()
    }

    val userLastKnownLocation : LatLng?
        get() {
            activity?.let {activity->
                if (ActivityCompat.checkSelfPermission(activity.applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
                    return null
                }
                val location = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                return location?.let{ LatLng(it.latitude, it.longitude) }
            }
            return null
        }


    var userLatLng: LatLng? = null

    override fun onLocationResult(locationResult: LocationResult?) {
        locationResult ?: return
        for (location in locationResult.locations) {
            val latlng = LatLng(location.latitude, location.longitude)
            onUserLocationUpdateCallbacks.forEach {it?.invoke(latlng)}
            userLatLng = latlng
        }
    }

    fun stopLocationUpdates() {
        mFusedLocationClient?.removeLocationUpdates(this)
    }

    fun startLocationUpdates() {
        activity?.applicationContext?.let {
            if( ActivityCompat.checkSelfPermission(it, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                //Toast.makeText(activity.applicationContext, "Erro: O aplicativo precisa de permissão para acessar sua localização para funcionar.", Toast.LENGTH_SHORT).show()
                Log.w(TAG,"No location permission.")
//                createLocationRequest()
                return
            }
            if(!locationSettingsCorrect){
                //Toast.makeText(activity.applicationContext, "GPS desligado", Toast.LENGTH_SHORT).show()
                Log.w(TAG,"Wrong phone location settings.")
//                createLocationRequest()
                return
            }
            mFusedLocationClient!!.requestLocationUpdates(locationRequest,
                    this,
                    null)
        }
    }

    fun createLocationRequest() {
        activity?.let {activity->
            if (Build.VERSION.SDK_INT >= 23)
                if (ContextCompat.checkSelfPermission(activity.applicationContext, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                    ActivityCompat.requestPermissions(activity, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1)

            locationRequest = LocationRequest.create()?.apply {
                interval = 1000
                fastestInterval = 800
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }
            locationRequest?.let {
                val builder = LocationSettingsRequest.Builder()
                        .addLocationRequest(it)
                val client: SettingsClient = LocationServices.getSettingsClient(activity.applicationContext)
                val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())
                task.addOnSuccessListener {
                    locationSettingsCorrect = true
                    startLocationUpdates()
                }
            task.addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    // Location settings are not satisfied. Show to the user a dialog.
                    try {
                        // then check the result in onActivityResult().
                        exception.startResolutionForResult(activity,
                                REQUEST_CHANGE_LOCATION_SETTINGS)
                    } catch (sendEx: IntentSender.SendIntentException) {
                        // Ignore the error.
                    }
                }
                }
            }
        }
    }
}


//package flaskoski.rs.smartmuseum.location
//
//import android.location.Location;
//import android.os.Looper;
//import android.util.Log;
//
//import com.google.android.gms.location.FusedLocationProviderClient;
//import com.google.android.gms.location.LocationCallback;
//import com.google.android.gms.location.LocationRequest;
//import com.google.android.gms.location.LocationResult;
//import com.google.android.gms.location.LocationServices;
//import com.google.android.gms.location.LocationSettingsRequest;
//
///**
// * Uses Google Play API for obtaining device locations
// * Created by alejandro.tkachuk
// * alejandro@calculistik.com
// * www.calculistik.com Mobile Development
// */
//
//class UserLocationManager {
//    companion object {
//        val instance : UserLocationManager = UserLocationManager();
//
//        val TAG = UserLocationManager::class.java.simpleName;
//
//    }
//    private lateinit var mFusedLocationClient: FusedLocationProviderClient
//    private lateinit var locationCallback : LocationCallback
//    private lateinit var locationRequest : LocationRequest
//    private lateinit var locationSettingsRequest : LocationSettingsRequest
//
//    private lateinit var workable : Workable<GPSPoint>
//
//    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 1000;
//    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 1000;
//
//    private Wherebouts() {
//        this.locationRequest = new LocationRequest();
//        this.locationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
//        this.locationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
//        this.locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
//
//        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
//        builder.addLocationRequest(this.locationRequest);
//        this.locationSettingsRequest = builder.build();
//
//        this.locationCallback = new LocationCallback() {
//            @Override
//            public void onLocationResult(LocationResult locationResult) {
//                super.onLocationResult(locationResult); // why? this. is. retarded. Android.
//                Location currentLocation = locationResult.getLastLocation();
//
//                GPSPoint gpsPoint = new GPSPoint(currentLocation.getLatitude(), currentLocation.getLongitude());
//                Log.i(TAG, "Location Callback results: " + gpsPoint);
//                if (null != workable)
//                    workable.work(gpsPoint);
//            }
//        };
//
//        this.mFusedLocationClient = LocationServices.getFusedLocationProviderClient(MainApplication.getAppContext());
//        this.mFusedLocationClient.requestLocationUpdates(this.locationRequest,
//                this.locationCallback, Looper.myLooper());
//    }
//
//    public static Wherebouts instance() {
//        return instance;
//    }
//
//    public void onChange(Workable<GPSPoint> workable) {
//        this.workable = workable;
//    }
//
//    public LocationSettingsRequest getLocationSettingsRequest() {
//        return this.locationSettingsRequest;
//    }
//
//    public void stop() {
//        Log.i(TAG, "stop() Stopping location tracking");
//        this.mFusedLocationClient.removeLocationUpdates(this.locationCallback);
//    }
//
//}