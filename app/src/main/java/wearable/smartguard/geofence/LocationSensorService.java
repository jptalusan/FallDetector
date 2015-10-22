package wearable.smartguard.geofence;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingApi;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.common.api.ResultCallback;

import wearable.smartguard.falldetector.Constants;
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
    private Geofence home;

    // Location updates intervals in sec
    private static int UPDATE_INTERVAL = 10000; // 10 sec
    private static int FATEST_INTERVAL = 15000; // 15 sec
    private static int DISPLACEMENT = 10; // 10 meters

    /**
     * Used when requesting to add or remove geofences.
     */
    private PendingIntent mGeofencePendingIntent;

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
        //TODO: Need to check availability of play services first
        populateGeofenceList();
        buildGoogleApiClient();
        createLocationRequest();
        previousLocation = new Location("");
        mGeofencePendingIntent = null;
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
//        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
//        if(location == null) {
//            System.out.println("location == null");
            startLocationUpdates();
//        } else {
//            System.out.println("location != null");
//            handleNewLocation(location);
//        }

        //START TEST
        //http://stackoverflow.com/questions/26633796/android-locationservices-geofencingapi-example-usage - geofences
        PendingResult<Status> result = LocationServices.GeofencingApi.addGeofences(
                mGoogleApiClient,
                getGeofencingRequest(),
                getGeofencePendingIntent());
        result.setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status status) {
                if (status.isSuccess()) {
                    Log.e(TAG, "Registered successfully geofence");
                    //successfully registered
                } else if (status.hasResolution()) {

                } else {
                    Log.e(TAG, "Registering failed: " + status.getStatusMessage());
                }
            }
        });
        //END TEST
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
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    protected void delayLocationRequest(long delay) {
        mLocationRequest.setInterval(delay);
        mLocationRequest.setFastestInterval(delay);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    /**
     * Starting the location updates
     * */
    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    private void handleNewLocation(Location location) {
        mLastLocation = location;
        Log.d(TAG, location.toString());
        //TEST
        Location userHome = new Location("");//provider name is unnecessary
        userHome.setLatitude(Double.parseDouble(editor.getString(Constants.LATITUDE, "14.651489")));//your coordinates of course
        userHome.setLongitude(Double.parseDouble(editor.getString(Constants.LATITUDE, "121.049309")));

        // Assign the new location
        mLastLocation = location;
        System.out.println("onLocationChanged: " + mLastLocation);
        System.out.println("Previous: " + previousLocation.toString() + " - Current: " + mLastLocation.toString());
        System.out.println("distance: " + mLastLocation.distanceTo(previousLocation));

        if(mLastLocation.getAccuracy() > 40 && mLastLocation.distanceTo(userHome) > 100) {
            //TODO: Prompt notification
//            delayLocationRequest(10000);
        } else if(mLastLocation.distanceTo(previousLocation) < 20) {
            Log.d(TAG, "delaying requests");
            delayLocationRequest(25000);
            startLocationUpdates();
        }
        previousLocation.setLatitude(mLastLocation.getLatitude());//your coordinates of course
        previousLocation.setLongitude(mLastLocation.getLongitude());
    }

    //TESTING GEOFENCES
    /**
     * Builds and returns a GeofencingRequest. Specifies the list of geofences to be monitored.
     * Also specifies how the geofence notifications are initially triggered.
     */
    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();

        // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
        // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
        // is already inside that geofence.
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_EXIT);

        // Add the geofences to be monitored by geofencing service.
        builder.addGeofence(home);

        // Return a GeofencingRequest.
        return builder.build();
    }

    /**
     * Gets a PendingIntent to send with the request to add or remove Geofences. Location Services
     * issues the Intent inside this PendingIntent whenever a geofence transition occurs for the
     * current list of geofences.
     *
     * @return A PendingIntent for the IntentService that handles geofence transitions.
     */
    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * This sample hard codes geofence data. A real app might dynamically create geofences based on
     * the user's location.
     */
    public void populateGeofenceList() {
        home = new Geofence.Builder()
                // Set the request ID of the geofence. This is a string to identify this
                // geofence.
                .setRequestId("home")

                .setCircularRegion(
                        Double.parseDouble(editor.getString(Constants.LATITUDE, "14.651489")),
                        Double.parseDouble(editor.getString(Constants.LATITUDE, "121.049309")),
                        Constants.FENCE_RADIUS_IN_METERS
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();
    }
}