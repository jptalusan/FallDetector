package wearable.smartguard.networklistener;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.google.android.gms.location.LocationRequest;

import wearable.smartguard.Constants;
import wearable.smartguard.falldetector.R;

/**
 * Created by talusan on 10/25/2015.
 */
public class NetworkChangeReceiver extends BroadcastReceiver {
    private static final String TAG = "NetworkChange";

    @Override
    public void onReceive(final Context context, final Intent intent) {
        String appname = context.getResources().getString(R.string.app_name);
        SharedPreferences editor = context.getSharedPreferences(appname, Context.MODE_PRIVATE);

        if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
            NetworkInfo netInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            if (ConnectivityManager.TYPE_WIFI == netInfo.getType()) {
                WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
                WifiInfo info = wifiManager.getConnectionInfo();
                if(info != null) {
                    if(Constants.HOME_SSID.equals(info.getSSID())) {
                        //TODO: Check if intent service for location is running, if yes cancel it
                        Log.i(TAG, "Currently connected to HOME SSID");
                        editor.edit().putInt(Constants.PREFS_NEW_PRIORITY, LocationRequest.PRIORITY_NO_POWER).apply();
                    } else {
                        Log.i(TAG, "Currently not connected to HOME SSID");
                        editor.edit().putInt(Constants.PREFS_NEW_PRIORITY, LocationRequest.PRIORITY_HIGH_ACCURACY).apply();
                    }
                }
            } else if (netInfo.getState() == NetworkInfo.State.DISCONNECTED || netInfo.getState() == NetworkInfo.State.DISCONNECTING) {
                Log.i(TAG, "Disconnecting/Disconnected");
                //TODO: Check if intent service is not running and then run it
                editor.edit().putInt(Constants.PREFS_NEW_PRIORITY, LocationRequest.PRIORITY_HIGH_ACCURACY).apply();
            }
        }
    }
}
