package wearable.userwatch;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;
import android.app.AlertDialog;
import android.view.View.OnClickListener;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import org.json.JSONException;

import java.lang.reflect.Array;
import java.util.ArrayList;

import wearable.userwatch.falldetector.AccelerometerSensorService;
import wearable.userwatch.accelerometer.R;
import wearable.userwatch.geofence.LocationSensorService;
import wearable.userwatch.memorymodule.Alarm;

public class BlankActivity extends AppCompatActivity {
    private static String DEBUG_TAG = "Activity";
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;
    private SharedPreferences editor;
    private String appname;
    private Button cancelAlarm;
    private Alarm alarmSample;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blank);

        Log.d("TAG", "Start");
        appname = getResources().getString(R.string.app_name);
        editor = getSharedPreferences(appname, Context.MODE_PRIVATE);

        cancelAlarm = (Button) findViewById(R.id.cancelAlarm);

        cancelAlarm.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                alarmSample.stopAlarm(getApplicationContext());
            }

        });

        Intent intent = new Intent(getApplicationContext(), AccelerometerSensorService.class);
        startService(intent);

        checkPlayServices();

        if(checkIfLocationIsEnabled()) {
            Log.d(DEBUG_TAG, "Starting location service v1");
            startAlarmManager();
        }

        try {
            startAlarmDemo();
        } catch (JSONException e) {
            Log.e(DEBUG_TAG, "Error", e);
        }
    }

    private void startAlarmDemo() throws JSONException {
        editor.edit().putString("SampleAlarmString", "{\"memories\": [\n" +
                "{\n" +
                "\"MemoryId\": 1,\n" +
                "\"MemoryName\": \"Wake\",\n" +
                "\"fkUserId\": 4,\n" +
                "\"MemoryFreq\": 1,\n" +
                "\"MemoryInstructions\": \"Wake up and wear smartguard watch.\",\n" +
                "\"MemoryDates\": \"Fri Nov 20 2015 16:09:08 GMT+0800,Thu Nov 26 2015 23:00:00 GMT+0800,Tue Dec 01 2015 16:12:40 GMT+0800,Wed Dec 02 2015 16:12:40 GMT+0800,Fri Dec 04 2015 16:12:40 GMT+0800,Thu Nov 26 2015 22:50:00 GMT+0800\"\n" +
                "},\n" +
                "{\n" +
                "\"MemoryId\": 4,\n" +
                "\"MemoryName\": \"Sleep\",\n" +
                "\"fkUserId\": 4,\n" +
                "\"MemoryFreq\": 2,\n" +
                "\"MemoryInstructions\": \"This activity cannot be renamed or deleted. You may only change its schedule.\",\n" +
                "\"MemoryDates\": \"Thu Nov 26 2015 22:55:00 GMT+0800\"\n" +
                "}\n" +
                "]}").apply();
//        editor.edit().putString("SampleAlarmString", "{\n" +
//                "\"memories\": [\n" +
//                "{\n" +
//                "\"MemoryId\": 1,\n" +
//                "\"MemoryName\": \"Wake\",\n" +
//                "\"fkUserId\": 4,\n" +
//                "\"MemoryFreq\": 2,\n" +
//                "\"MemoryInstructions\": \"This activity cannot be renamed or deleted. You may only change its schedule.\",\n" +
//                "\"MemoryDates\": \"Thu Nov 05 2015 00:28:30 GMT+0800,Sun Nov 08 2015 15:26:36 GMT+0800,Mon Nov 09 2015 15:26:36 GMT+0800\"\n" +
//                "},\n" +
//                "{\n" +
//                "\"MemoryId\": 4,\n" +
//                "\"MemoryName\": \"Sleep\",\n" +
//                "\"fkUserId\": 4,\n" +
//                "\"MemoryFreq\": 2,\n" +
//                "\"MemoryInstructions\": \"Alarm 2: Sleep.\",\n" +
//                "\"MemoryDates\": \"Thu Nov 26 2015 00:28:11 GMT+0800\"\n" +
//                "},\n" +
//                "{\n" +
//                "\"MemoryId\": 5,\n" +
//                "\"MemoryName\": \"Drink medicine\",\n" +
//                "\"fkUserId\": 4,\n" +
//                "\"MemoryFreq\": 1,\n" +
//                "\"MemoryInstructions\": \"check quantity left\",\n" +
//                "\"MemoryDates\": null\n" +
//                "},\n" +
//                "{\n" +
//                "\"MemoryId\": 6,\n" +
//                "\"MemoryName\": \"TEST the system Konrad\",\n" +
//                "\"fkUserId\": 4,\n" +
//                "\"MemoryFreq\": 2,\n" +
//                "\"MemoryInstructions\": \"Test, schould be possible to run more than once a day with different set times.\",\n" +
//                "\"MemoryDates\": null\n" +
//                "},\n" +
//                "{\n" +
//                "\"MemoryId\": 10,\n" +
//                "\"MemoryName\": \"Eat breakfast\",\n" +
//                "\"fkUserId\": 4,\n" +
//                "\"MemoryFreq\": 1,\n" +
//                "\"MemoryInstructions\": \"Take a nap. Recharge smart guard.\",\n" +
//                "\"MemoryDates\": \"Fri Nov 06 2015 20:05:11 GMT+0800,Fri Nov 06 2015 20:05:11 GMT+0800,Fri Nov 06 2015 20:05:11 GMT+0800\"\n" +
//                "}\n" +
//                "]\n" +
//                "}").apply();
        ArrayList<Alarm> alarms = Alarm.parseAlarmString(editor.getString("SampleAlarmString", ""));
        for(Alarm a : alarms) {
            Log.d(DEBUG_TAG, a.toString());
        }

        Alarm.cancelAllAlarms(getApplicationContext(), alarms);

        Alarm.startAllAlarms(getApplicationContext(), alarms);
//        alarmSample = alarms.get(0);
//        alarmSample.setAlarm(getApplicationContext());

//        alarms.get(1).setAlarm(getApplicationContext());
//        Alarm alarm = new Alarm(getApplicationContext(), "This is a test message");
//        alarm.setAlarm();
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
        startService(alarmIntent);
    }

    private void whenLocationIsNotSet() {
        Log.d(DEBUG_TAG, "Location disabled");
    }
}
