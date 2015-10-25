package wearable.smartguard.geofence;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import wearable.smartguard.Constants;
import wearable.smartguard.falldetector.R;

/**
 * Created by talusan on 10/20/2015.
 * http://android-developers.blogspot.in/2015/03/google-play-services-70-places-everyone.html
 * http://stackoverflow.com/questions/4721449/how-can-i-enable-or-disable-the-gps-programmatically-on-android (attempting to turn on Locations automatically)
 * https://developers.google.com/android/reference/com/google/android/gms/location/LocationRequest Location request dev doc
 * http://blog.lemberg.co.uk/fused-location-provider fused location write up
 * http://stackoverflow.com/questions/26200690/fused-location-provider-unexpected-behavior
 * http://stackoverflow.com/questions/6775257/android-location-providers-gps-or-network-provider
 * https://github.com/googlesamples/android-play-location/tree/master/Geofencing/app/src/main/java/com/google/android/gms/location/sample/geofencing (check this)
 */
public class LocationSensorService extends IntentService implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private static final String TAG = "Location";
    private Location mLastLocation;
    private Location previousLocation;

    // Google client to interact with Google API
    private GoogleApiClient mGoogleApiClient;

    private LocationRequest mLocationRequest;

    private SharedPreferences editor;
    private String appname;

    // Location updates intervals in sec
    private static long UPDATE_INTERVAL = Constants.DEFAULT_UPDATE_INTERVAL_IN_SEC; // 10 sec
    private static long FATEST_INTERVAL = Constants.DEFAULT_FASTEST_INTERVAL_IN_SEC; // 15 sec
    private static int DISPLACEMENT = Constants.DEFAULT_DISPLACEMENT_IN_M; // 10 meters

    public LocationSensorService() {
        super("GPSIntentService");
        System.out.println("Started GPSIntentService v4");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate:Service started");
        appname = getResources().getString(R.string.app_name);
        editor = getSharedPreferences(appname, MODE_PRIVATE);
        if(editor.getBoolean("Started", false)) {
            buildGoogleApiClient();
            createLocationRequest(UPDATE_INTERVAL, LocationRequest.PRIORITY_HIGH_ACCURACY, DISPLACEMENT);
            previousLocation = new Location("");
            editor.edit().putBoolean("Started", true).apply();
        }
    }

    @Override
    public void onDestroy() {
        if(!mGoogleApiClient.isConnected()) {
            System.out.println("connecting...");
            mGoogleApiClient.connect();
        } else {
            System.out.println("staring loc updates...");
            stopLocationUpdates();
        }
        editor.edit().putBoolean("Started", false).apply();;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        System.out.println("onHandleIntent");
        if(!mGoogleApiClient.isConnected()) {
            System.out.println("connecting...");
            mGoogleApiClient.connect();
        } else {
            System.out.println("staring loc updates...");
            startLocationUpdates();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags,startId);
    }

    @Override
    public void onLocationChanged(Location location) {
        handleNewLocation(location);
    }

    /**
     * Google api callback methods
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = "
                + result.getErrorCode());
    }

    @Override
    public void onConnected(Bundle arg0) {
        System.out.println("onConnected");
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int arg0) {
        mGoogleApiClient.connect();
    }

    /**
     * Creating google api client object
     * */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
    }

    /**
     * Creating location request object
     * https://developer.android.com/training/location/receive-location-updates.html#location-request
     * */
    protected void createLocationRequest(long delay, int priority, int displacement) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(delay);
        mLocationRequest.setFastestInterval(delay);
        mLocationRequest.setPriority(priority);
        mLocationRequest.setSmallestDisplacement(displacement);
    }

    /**
     * Starting the location updates
     * */
    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }

    private void handleNewLocation(Location location) {
        mLastLocation = location;

        //Home Coordinates
        Location userHome = new Location("");//provider name is unnecessary
        userHome.setLatitude(Double.parseDouble(editor.getString(Constants.LATITUDE, "14.659799")));
        userHome.setLongitude(Double.parseDouble(editor.getString(Constants.LATITUDE, "121.039999")));

        Log.d(TAG, mLastLocation.toString());

        if(mLastLocation.getAccuracy() > Constants.LOCATION_ACCURACY && mLastLocation.getLatitude() != 0.0 && mLastLocation.distanceTo(userHome) > Constants.FENCE_RADIUS_IN_METERS) {
            //TODO: Prompt notification for user exiting home
            Log.d(TAG, "distance: " + mLastLocation.distanceTo(userHome));
            if(mLocationRequest.getFastestInterval() != 10000) {
                mGoogleApiClient.blockingConnect();
                createLocationRequest(10000, LocationRequest.PRIORITY_HIGH_ACCURACY, 5);
                buildGoogleApiClient();
            }
        } else if(mLastLocation.distanceTo(previousLocation) < Constants.NEGLIGIBLE_LOCATION_CHANGE) { //User has stayed in same vicinity for X seconds
            Log.d(TAG, "delaying requests");
            if(mLocationRequest.getFastestInterval() != 25000) {
                createLocationRequest(25000, LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY, 30);
                buildGoogleApiClient();
            }
        } else {
            //TODO: just add the network thing here, add sharedprefs entry in broadcast receiver and read it here to see if it changed network
            //TODO: or place network thing outside if statement
            Log.d(TAG, "distance: " + mLastLocation.distanceTo(userHome));
        }

        previousLocation.setLatitude(mLastLocation.getLatitude());//your coordinates of course
        previousLocation.setLongitude(mLastLocation.getLongitude());
    }
}