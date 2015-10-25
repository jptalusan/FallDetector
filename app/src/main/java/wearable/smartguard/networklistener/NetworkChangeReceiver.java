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

import wearable.smartguard.Constants;
import wearable.smartguard.falldetector.R;
import wearable.smartguard.geofence.LocationSensorService;

/**
 * Created by talusan on 10/25/2015.
 */
public class NetworkChangeReceiver extends BroadcastReceiver {
    private static final String TAG = "NetworkChange";
    private SharedPreferences editor;
    private String appname;

    @Override
    public void onReceive(final Context context, final Intent intent) {
        appname = context.getResources().getString(R.string.app_name);
        editor = context.getSharedPreferences(appname, Context.MODE_PRIVATE);

        if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
            NetworkInfo netInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            if (ConnectivityManager.TYPE_WIFI == netInfo.getType()) {
                WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
                WifiInfo info = wifiManager.getConnectionInfo();
                if(info != null) {
                    Log.i(TAG, info.getSSID());
                    if(Constants.HOME_SSID.equals(info.getSSID())) {
                        Log.i(TAG, "Currently connected to HOME SSID");
                        //TODO: Check if intent service for location is running, if yes cancel it
                        Intent alarmIntent = new Intent(context, LocationSensorService.class);
                        context.stopService(alarmIntent);
                    } else {
                        editor.edit().putBoolean("Started", true).apply();

                        //TODO: Check if intent service is not running and then run it, severly flawed
                        //fires multiple times
                        Log.i(TAG, "Currently not connected to HOME SSID");
                        Intent alarmIntent = new Intent(context, LocationSensorService.class);
                        context.startService(alarmIntent);
                    }
                }
            } else if (netInfo.getState() == NetworkInfo.State.DISCONNECTED || netInfo.getState() == NetworkInfo.State.DISCONNECTING) {
                Log.i(TAG, "Disconnecting/Disconnected");
                //TODO: Check if intent service is not running and then run it
                Intent alarmIntent = new Intent(context, LocationSensorService.class);
                PendingIntent pendingIntent =   PendingIntent.getService(context, 0, alarmIntent, PendingIntent.FLAG_NO_CREATE);
                if (pendingIntent == null){
                    context.startService(alarmIntent);
                }
            }
        }
    }
}
