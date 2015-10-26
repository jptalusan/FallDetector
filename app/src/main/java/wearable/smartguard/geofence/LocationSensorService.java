package wearable.smartguard.geofence;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.lang.ref.WeakReference;

import wearable.smartguard.Constants;
import wearable.smartguard.Utils;
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

    private static SharedPreferences editor;
    private static String appname;

    // Location updates intervals in sec
    private static long UPDATE_INTERVAL = Constants.DEFAULT_UPDATE_INTERVAL_IN_SEC; // 10 sec
    private static long FATEST_INTERVAL = Constants.DEFAULT_FASTEST_INTERVAL_IN_SEC; // 15 sec
    private static int DISPLACEMENT = Constants.DEFAULT_DISPLACEMENT_IN_M; // 10 meters

    private static int DEFAULT_PRIORITY = 0;

    private Location userHome;

    public LocationSensorService() {
        super("GPSIntentService");
        System.out.println("Started GPSIntentService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate:Service started");
        appname = getResources().getString(R.string.app_name);
        editor = getSharedPreferences(appname, MODE_PRIVATE);
        buildGoogleApiClient();
        if(Utils.isNetworkAvailable(this) && Utils.isConnectedToHome(this, Constants.HOME_SSID)) {
            DEFAULT_PRIORITY = LocationRequest.PRIORITY_NO_POWER;
            editor.edit().putInt(Constants.PREFS_CURRENT_PRIORITY, DEFAULT_PRIORITY).apply();
            createLocationRequest(UPDATE_INTERVAL, LocationRequest.PRIORITY_NO_POWER, DISPLACEMENT);
        } else {
            DEFAULT_PRIORITY = LocationRequest.PRIORITY_HIGH_ACCURACY;
            editor.edit().putInt(Constants.PREFS_CURRENT_PRIORITY, DEFAULT_PRIORITY).apply();
            createLocationRequest(UPDATE_INTERVAL, LocationRequest.PRIORITY_HIGH_ACCURACY, DISPLACEMENT);
        }
        userHome = new Location("");//provider name is unnecessary
        previousLocation = new Location("");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        System.out.println("onHandleIntent");
        if(!mGoogleApiClient.isConnected()) {
            System.out.println("connecting...");
            mGoogleApiClient.connect();
        } else {
            System.out.println("starting loc updates...");
            startLocationUpdates();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        final LocationPriorityHandler locationPriorityHandler = new LocationPriorityHandler(this);
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    try {
                        Thread.sleep(5000);
//                        if(!mGoogleApiClient.isConnected())
//                            mGoogleApiClient.blockingConnect();
                        locationPriorityHandler.sendEmptyMessage(0);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
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
        if(mLocationRequest == null)
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
        userHome.setLatitude(Double.parseDouble(editor.getString(Constants.LATITUDE, "14.659799")));
        userHome.setLongitude(Double.parseDouble(editor.getString(Constants.LATITUDE, "121.039999")));

        Log.d(TAG, mLastLocation.toString());
        Log.d(TAG, "Priority: " + mLocationRequest.getPriority());

        if(mLastLocation.getAccuracy() > Constants.LOCATION_ACCURACY && mLastLocation.getLatitude() != 0.0 && mLastLocation.distanceTo(userHome) > Constants.FENCE_RADIUS_IN_METERS) {
            //TODO: Prompt notification for user exiting home
            Log.d(TAG, "distance: " + mLastLocation.distanceTo(userHome));
            if(mLocationRequest.getFastestInterval() != 10000) {
                if(editor.getInt(Constants.PREFS_CURRENT_PRIORITY, LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY) !=
                        editor.getInt(Constants.PREFS_NEW_PRIORITY, LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)) {
                    switchToHighAccuracy();
                }
            }
        } else if(mLastLocation.distanceTo(previousLocation) < Constants.NEGLIGIBLE_LOCATION_CHANGE) { //User has stayed in same vicinity for X seconds
            Log.d(TAG, "delaying requests");
            if(mLocationRequest.getFastestInterval() != 25000) {
                if(Utils.isNetworkAvailable(this) && !Utils.isConnectedToHome(this, Constants.HOME_SSID)) {
                    if(editor.getInt(Constants.PREFS_CURRENT_PRIORITY, LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY) !=
                            editor.getInt(Constants.PREFS_NEW_PRIORITY, LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)) {
                        switchToBalancedPower();
                    }
                }
            }
        } else {
            //TODO: just add the network thing here, add sharedprefs entry in broadcast receiver and read it here to see if it changed network
            //TODO: or place network thing outside if statement
            Log.d(TAG, "distance: " + mLastLocation.distanceTo(userHome));
        }

        previousLocation.setLatitude(mLastLocation.getLatitude());//your coordinates of course
        previousLocation.setLongitude(mLastLocation.getLongitude());
    }

    public void switchToNoPower() {
        Log.d(TAG, "Switching to no power");
        stopLocationUpdates();
        editor.edit().putInt(Constants.PREFS_CURRENT_PRIORITY, LocationRequest.PRIORITY_NO_POWER).apply();
        createLocationRequest(3600000, LocationRequest.PRIORITY_NO_POWER, 100);
        startLocationUpdates();
    }

    public void switchToHighAccuracy() {
        Log.d(TAG, "Switching to high accuracy");
        stopLocationUpdates();
        editor.edit().putInt(Constants.PREFS_CURRENT_PRIORITY, LocationRequest.PRIORITY_HIGH_ACCURACY).apply();
        createLocationRequest(10000, LocationRequest.PRIORITY_HIGH_ACCURACY, 5);
        startLocationUpdates();
    }

    public void switchToBalancedPower() {
        Log.d(TAG, "Switching to balanced power");
        stopLocationUpdates();
        editor.edit().putInt(Constants.PREFS_CURRENT_PRIORITY, LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY).apply();
        createLocationRequest(25000, LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY, 20);
        startLocationUpdates();
    }

    static class LocationPriorityHandler extends Handler {
        WeakReference<LocationSensorService> mService;
        LocationPriorityHandler(LocationSensorService aService) {
            mService = new WeakReference<>(aService);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "Handler Running");
            Log.d(TAG, "Current Priority: " + editor.getInt(Constants.PREFS_CURRENT_PRIORITY, DEFAULT_PRIORITY));
            Log.d(TAG, "New Priority: " + editor.getInt(Constants.PREFS_NEW_PRIORITY, DEFAULT_PRIORITY));
            if(editor.getInt(Constants.PREFS_CURRENT_PRIORITY, DEFAULT_PRIORITY) !=
                    editor.getInt(Constants.PREFS_NEW_PRIORITY, DEFAULT_PRIORITY)) {
                LocationSensorService locService = mService.get();
                switch (editor.getInt(Constants.PREFS_NEW_PRIORITY, DEFAULT_PRIORITY)) {
                    case LocationRequest.PRIORITY_NO_POWER:
                        locService.switchToNoPower();
                        break;
                    case LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY:
                    default:
                        locService.switchToBalancedPower();
                        break;
                    case LocationRequest.PRIORITY_HIGH_ACCURACY:
                        locService.switchToHighAccuracy();
                        break;
                }
            }
        }
    }
}