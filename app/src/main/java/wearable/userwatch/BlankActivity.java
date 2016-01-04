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
import wearable.userwatch.memorymodule.AlarmUtils;

public class BlankActivity extends AppCompatActivity {
    private static String DEBUG_TAG = "BlankActivity";
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;
    private SharedPreferences editor;
    private String appname;
    private Button cancelAlarm;
    private Alarm alarmSample;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blank);

        Log.d(DEBUG_TAG, "Start");
        appname = getResources().getString(R.string.app_name);
        editor = getSharedPreferences(appname, Context.MODE_PRIVATE);

        cancelAlarm = (Button) findViewById(R.id.cancelAlarm);

        cancelAlarm.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                AlarmUtils.stopAlarm(getApplicationContext(), alarmSample);
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
        //Note: 12 mn is 00:00:00
        editor.edit().putString("SampleAlarmString", "{\"memories\": [\n" +
                "{\n" +
                "\"MemoryId\": 1,\n" +
                "\"MemoryName\": \"Sleep\",\n" +
                "\"fkUserId\": 4,\n" +
                "\"MemoryFreq\": 1,\n" +
                "\"MemoryInstructions\": \"Wake up and wear smartguard watch.\",\n" +
                "\"MemoryDates\": \"Tue Jan 05 2016 00:07:00 GMT+0800,Tue Jan 05 2016 00:09:00 GMT+0800\",\n" +
//                "\"MemoryDates\": \"Mon Jan 04 2016 23:34:00 GMT+0800\",\n" +
                "\"MemoryType\": 0\n" +
                "}\n" +
                "]}").apply();

//        editor.edit().putString("SampleAlarmString",
//                "{\n" +
//                "\"memories\": [\n" +
//                "{\n" +
//                "\"MemoryId\": 1,\n" +
//                "\"MemoryName\": \"Wake\",\n" +
//                "\"fkUserId\": 4,\n" +
//                "\"MemoryFreq\": 1,\n" +
//                "\"MemoryInstructions\": \"Wake up and wear smartguard watch.\",\n" +
//                "\"MemoryDates\": \"Mon Jan 04 2016 23:24:00 GMT+0800,Mon Jan 04 2016 08:00:57 GMT+0800,Tue Jan 05 2016 08:00:57 GMT+0800,Wed Jan 06 2016 08:00:57 GMT+0800,Thu Jan 07 2016 08:00:57 GMT+0800,Fri Jan 08 2016 08:00:57 GMT+0800,Sat Jan 02 2016 13:12:37 GMT+0800,\",\n" +
//                "\"MemoryType\": 0\n" +
//                "},\n" +
//                "{\n" +
//                "\"MemoryId\": 4,\n" +
//                "\"MemoryName\": \"Sleep\",\n" +
//                "\"fkUserId\": 4,\n" +
//                "\"MemoryFreq\": 1,\n" +
//                "\"MemoryInstructions\": \"Please put your smartguard into the docking station\",\n" +
//                "\"MemoryDates\": \"Tue Dec 29 2015 23:00:07 GMT+0800,\",\n" +
//                "\"MemoryType\": 0\n" +
//                "},\n" +
//                "{\n" +
//                "\"MemoryId\": 24,\n" +
//                "\"MemoryName\": \"Fitminutes\",\n" +
//                "\"fkUserId\": 4,\n" +
//                "\"MemoryFreq\": 1,\n" +
//                "\"MemoryInstructions\": \"Go for a short walk\",\n" +
//                "\"MemoryDates\": \"Wed Dec 30 2015 09:13:41 GMT+0800,\",\n" +
//                "\"MemoryType\": 0\n" +
//                "}\n" +
//                "]}").apply();

        ArrayList<Alarm> alarms = AlarmUtils.parseAlarmString(editor.getString("SampleAlarmString", ""));
        for(Alarm a : alarms) {
            Log.d(DEBUG_TAG, a.toString());
        }

        AlarmUtils.cancelAllAlarms(getApplicationContext(), alarms);
        AlarmUtils.startAllAlarms(getApplicationContext(), alarms);
//        Alarm.cancelAllAlarms(getApplicationContext(), alarms);
//        Alarm.startAllAlarms(getApplicationContext(), alarms);
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
