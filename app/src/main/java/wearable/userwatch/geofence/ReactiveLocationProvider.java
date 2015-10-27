package wearable.userwatch.geofence;

import android.app.IntentService;
import android.content.Intent;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.location.LocationRequest;

import rx.Subscription;
import rx.functions.Action1;

/**
 * Created by talusan on 10/27/2015.
 */
public class ReactiveLocationProvider extends IntentService {
    private static final String TAG = "Reactive";
    private Subscription subscription;
    private pl.charmas.android.reactivelocation.ReactiveLocationProvider locationProvider;
    private LocationRequest locationRequest;
    public ReactiveLocationProvider() {
        super("ReactiveLocationProvider");
        System.out.println("Started ReactiveLocationProvider");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setNumUpdates(5)
                .setInterval(5000);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, startId, startId);
        Log.i("LocalService", "Received start id " + startId + ": " + intent);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        subscription.unsubscribe();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "onHandleIntent");
        locationProvider = new pl.charmas.android.reactivelocation.ReactiveLocationProvider(getApplicationContext());
        subscription = locationProvider.getUpdatedLocation(locationRequest)
                .subscribe(new Action1<Location>() {
                    @Override
                    public void call(Location location) {
                        Log.d("Reactiv", "Loc: " + location.toString());
                    }
                });
    }
}
