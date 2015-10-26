package wearable.smartguard;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;
import android.app.AlertDialog;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import wearable.smartguard.falldetector.AccelerometerSensorService;
import wearable.smartguard.falldetector.R;
import wearable.smartguard.geofence.LocationSensorService;

public class BlankActivity extends AppCompatActivity {
    private static String DEBUG_TAG = "Activity";
    private Button startStop;
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;
    private SharedPreferences editor;
    private String appname;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blank);

        Log.d("TAG", "Start");
        appname = getResources().getString(R.string.app_name);
        editor = getSharedPreferences(appname, Context.MODE_PRIVATE);
        Intent intent = new Intent(getApplicationContext(), AccelerometerSensorService.class);
        startService(intent);

        checkPlayServices();

        Log.d(DEBUG_TAG, "Starting location service");
        if(checkIfLocationIsEnabled()) {
            startAlarmManager();
        }
//        if(Utils.isNetworkAvailable(this) && Utils.isConnectedToHome(this, Constants.HOME_SSID)) {
//            Log.d(DEBUG_TAG, "Not starting location service");
//            //do not start location service
//            editor.edit().putBoolean("Started", false).apply();
//        } else {
//            Log.d(DEBUG_TAG, "Starting location service");
//            if(checkIfLocationIsEnabled()) {
//                startAlarmManager();
//            }
//        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_blank, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Method to verify google play services on the device
     * */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Toast.makeText(getApplicationContext(),
                        "This device is not supported.", Toast.LENGTH_LONG)
                        .show();
                finish();
            }
            return false;
        }
        return true;
    }

    private boolean checkIfLocationIsEnabled() {
        LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception ex) {}

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch(Exception ex) {}

        if(!gps_enabled && !network_enabled) {
            // notify user
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setMessage("GPS network not enabled");
            dialog.setPositiveButton("Open Location settings", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    // TODO Auto-generated method stub
                    Intent myIntent = new Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(myIntent);
                    startAlarmManager();
                }
            });
            dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    whenLocationIsNotSet();
                }
            });
            dialog.show();
        } else {
            return true;
        }
        return false;
    }

    private void startAlarmManager() {
        Intent alarmIntent = new Intent(this, LocationSensorService.class);
        this.startService(alarmIntent);
    }

    private void whenLocationIsNotSet() {
        Log.d(DEBUG_TAG, "Location disabled");
    }
}
