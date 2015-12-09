package wearable.userwatch.geofence;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.lang.ref.WeakReference;

import wearable.userwatch.Constants;
import wearable.userwatch.Utils;
import wearable.userwatch.accelerometer.R;

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
    private LocationPriorityHandler locationPriorityHandler = new LocationPriorityHandler(this);


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
        if(Utils.isNetworkAvailable(getApplicationContext()) && Utils.isConnectedToHome(getApplicationContext(), Constants.HOME_SSID)) {
            DEFAULT_PRIORITY = LocationRequest.PRIORITY_NO_POWER;
            editor.edit().putInt(Constants.PREFS_CURRENT_PRIORITY, DEFAULT_PRIORITY).apply();
            createLocationRequest(UPDATE_INTERVAL, LocationRequest.PRIORITY_NO_POWER, DISPLACEMENT);
        } else {
            DEFAULT_PRIORITY = LocationRequest.PRIORITY_HIGH_ACCURACY;
            editor.edit().putInt(Constants.PREFS_CURRENT_PRIORITY, DEFAULT_PRIORITY).apply();
            createLocationRequest(UPDATE_INTERVAL, LocationRequest.PRIORITY_HIGH_ACCURACY, DISPLACEMENT);
        }
        userHome = new Location("");
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
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    try {
                        Thread.sleep(10000);
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
        userHome.setLatitude(Double.parseDouble(editor.getString(Constants.HOME_LATITUDE, "14.659799")));
        userHome.setLongitude(Double.parseDouble(editor.getString(Constants.HOME_LATITUDE, "121.039999")));

        Log.d(TAG, mLastLocation.toString());
        Log.d(TAG, "Priority: " + mLocationRequest.getPriority());

        //
        editor.edit().putBoolean(Constants.EMERGENCY_STATUS, true).apply();
        Log.d(TAG, "Time: " + Utils.getCurrentTimeStampInSeconds());
        Log.d(TAG, "Written time: " + editor.getLong(Constants.EMERGENCY_TIMER, 0));
        if(Utils.getCurrentTimeStampInSeconds() - editor.getLong(Constants.EMERGENCY_TIMER, 0) > 60) {
            if (editor.getBoolean(Constants.EMERGENCY_STATUS, false)) {
                Log.d(TAG, "Writing emergency location");
                editor.edit().putLong(Constants.EMERGENCY_TIMER, Utils.getCurrentTimeStampInSeconds()).apply();
                editor.edit().putString(Constants.USER_CURRENT_LATITUDE, mLastLocation.getLatitude() + "").apply();
                editor.edit().putString(Constants.USER_CURRENT_LONGITUDE, mLastLocation.getLongitude() + "").apply();
            }
        } else {
            Log.d(TAG, "Time before next emergency loc: " + (60 - Utils.getCurrentTimeStampInSeconds() - editor.getLong(Constants.EMERGENCY_TIMER, 0)));
        }

        //Constants.FENCE_RADIUS_IN_METERS, once the distance from home, exceeds this, there will be a flag.
        if(mLastLocation.getAccuracy() < Constants.LOCATION_ACCURACY && mLastLocation.getLatitude() != 0.0 && mLastLocation.distanceTo(userHome) > Constants.FENCE_RADIUS_IN_METERS) {
            //TODO: Prompt notification for user exiting home
            Toast.makeText(getApplicationContext(), "User has left home!", Toast.LENGTH_SHORT).show();
            editor.edit().putBoolean(Constants.IS_USER_AT_HOME, false).apply();
            Log.d(TAG, "User has left home.");
            Log.d(TAG, "distance: " + mLastLocation.distanceTo(userHome));
            if(mLocationRequest.getFastestInterval() != 10000) {
                if(isNewPrioritySameAsCurrentPriority()) {
                    switchToHighAccuracy();
            }
            }
        } else if(mLastLocation.getAccuracy() > Constants.FENCE_RADIUS_IN_METERS && mLastLocation.getLatitude() != 0.0) {
            Log.d(TAG, "Location inconclusive.");
        } else { //if(mLastLocation.distanceTo(previousLocation) < Constants.NEGLIGIBLE_LOCATION_CHANGE) { //User has stayed in same vicinity for X seconds
            Log.d(TAG, "delaying requests");
            Log.d(TAG, "User is still at home.");
            Toast.makeText(getApplicationContext(), "User has arrived home!", Toast.LENGTH_SHORT).show();
            editor.edit().putBoolean(Constants.IS_USER_AT_HOME, true).apply();
            if(mLocationRequest.getFastestInterval() != 25000) {
                if(Utils.isNetworkAvailable(getApplicationContext()) && !Utils.isConnectedToHome(getApplicationContext(), Constants.HOME_SSID)) {
                    if(isNewPrioritySameAsCurrentPriority()) {
                        switchToBalancedPower();
                    }
                }
            }
        }
//        else {
//            Log.d(TAG, "distance: " + mLastLocation.distanceTo(userHome));
//        }

        previousLocation.setLatitude(mLastLocation.getLatitude());//your coordinates of course
        previousLocation.setLongitude(mLastLocation.getLongitude());
    }

    public void switchToNoPower() {
        Log.d(TAG, "Switching to no power");
        stopLocationUpdates();
        setCurrentPriorityAs(LocationRequest.PRIORITY_NO_POWER);
        createLocationRequest(3600000, LocationRequest.PRIORITY_NO_POWER, 100);
        startLocationUpdates();
    }

    public void switchToHighAccuracy() {
        Log.d(TAG, "Switching to high accuracy");
        stopLocationUpdates();
        setCurrentPriorityAs(LocationRequest.PRIORITY_HIGH_ACCURACY);
        createLocationRequest(10000, LocationRequest.PRIORITY_HIGH_ACCURACY, 5);
        startLocationUpdates();
    }

    public void switchToBalancedPower() {
        Log.d(TAG, "Switching to balanced power");
        stopLocationUpdates();
        setCurrentPriorityAs(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        createLocationRequest(25000, LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY, 20);
        startLocationUpdates();
    }

    private void setCurrentPriorityAs(int priority) {
        editor.edit().putInt(Constants.PREFS_CURRENT_PRIORITY, priority).apply();
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
            if(isNewPrioritySameAsCurrentPriority()) {
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

    private static boolean isNewPrioritySameAsCurrentPriority() {
        return editor.getInt(Constants.PREFS_CURRENT_PRIORITY, DEFAULT_PRIORITY) !=
                editor.getInt(Constants.PREFS_NEW_PRIORITY, DEFAULT_PRIORITY);
    }
}