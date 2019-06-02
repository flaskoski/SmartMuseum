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
//class UserPosition {
//    companion object {
//        val instance : UserPosition = UserPosition();
//
//        val TAG = UserPosition::class.java.simpleName;
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