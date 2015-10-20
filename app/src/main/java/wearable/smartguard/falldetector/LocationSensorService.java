package wearable.smartguard.falldetector;

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

/**
 * Created by talusan on 10/20/2015.
 */
public class LocationSensorService extends IntentService implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private static final String TAG = "Location";
    private Location mLastLocation;

    // Google client to interact with Google API
    private GoogleApiClient mGoogleApiClient;

    private LocationRequest mLocationRequest;

    private SharedPreferences editor;
    private String appname;

    // Location updates intervals in sec
    private static int UPDATE_INTERVAL = 10000; // 10 sec
    private static int FATEST_INTERVAL = 15000; // 15 sec
    private static int DISPLACEMENT = 10; // 10 meters

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
        //TODO: Need to check availability of play services first
        buildGoogleApiClient();
        createLocationRequest();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        System.out.println("onHandleIntent");
        if(!mGoogleApiClient.isConnected()) {
            System.out.println("connecting...");
            mGoogleApiClient.connect();
        } else {
            displayLocation();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent,flags,startId);
    }

    @Override
    public void onLocationChanged(Location location) {
        //TEST
        Location userHome = new Location("");//provider name is unecessary
        userHome.setLatitude(14.651489d);//your coords of course
        userHome.setLongitude(121.049309d);

        // Assign the new location
        mLastLocation = location;
        System.out.println("onLocationChanged: " + mLastLocation);
        System.out.println("provider: " + mLastLocation.getProvider());

        if(mLastLocation.distanceTo(userHome) > 100) {
            //TODO: Prompt notification
        }
        System.out.println("distance from home: " + mLastLocation.distanceTo(userHome));
        // Displaying the new location on UI
//        displayLocation();
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
        displayLocation();
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
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FATEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    /**
     * Starting the location updates
     * */
    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    /**
     * Method to display the location on UI
     * */
    private void displayLocation() {
        startLocationUpdates();

        mLastLocation = LocationServices.FusedLocationApi
                .getLastLocation(mGoogleApiClient);

        if (mLastLocation != null) {
            double latitude = mLastLocation.getLatitude();
            double longitude = mLastLocation.getLongitude();

            System.out.println(latitude + ", " + longitude);
            editor.edit().putLong(Constants.LATITUDE, Double.doubleToLongBits(latitude)).apply();;
            editor.edit().putLong(Constants.LONGITUDE, Double.doubleToLongBits(longitude)).apply();;
        } else {
            System.out.println("(Couldn't get the location. Make sure location is enabled on the device)");
        }
    }
}
