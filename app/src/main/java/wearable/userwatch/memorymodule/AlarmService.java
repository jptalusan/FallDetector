package wearable.userwatch.memorymodule;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Button;

import org.xml.sax.Parser;

import wearable.userwatch.Constants;
import wearable.userwatch.Utils;
import wearable.userwatch.accelerometer.R;

/**
 * Created by talusan on 11/9/2015.
 * http://stackoverflow.com/questions/24724859/alarmmanager-setexact-with-wakefulbroadcastreceiver-sometimes-not-exact
 * http://developer.android.com/training/scheduling/alarms.html
 * This class handles the behaviors of all alarms in the memoryModule
 */
public class AlarmService extends IntentService {
    private static final String TAG = "Wearable.AlarmService";
    private String appname;
    private SharedPreferences editor;
    public AlarmService() {
        super("ScheduledService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        appname = getResources().getString(R.string.app_name);
        editor = getSharedPreferences(appname, Context.MODE_PRIVATE);
        Log.d(TAG, "onCreate:AlarmService");
    }

    @Override
    protected  void onHandleIntent(Intent intent) {
        Bundle data = intent.getExtras();
        //Behavior when alarm, set by Alarm.java is triggered (reminder to user)
        if(data.getParcelable(Constants.ALARM) != null) {
            Alarm alarm = data.getParcelable(Constants.ALARM);
            Log.d(TAG, alarm.toString());
//            Log.d(TAG, "AlarmId: " + alarm.getMemoryId());
//            if (alarm.getMemoryDates() != null) {
//                for (int i = 0; i < alarm.getMemoryDates().length; ++i) {
//                    Log.d(TAG, "MemoryDates: " + alarm.getMemoryDates()[i]);
//                }
//            }
//            Log.d(TAG, alarm.getMemoryInstructions());
            //TODO: Add checking if alarm notification activity is currently open.
            Intent alarmIntent = new Intent(getBaseContext(), AlarmNotificationActivity.class);
            alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            alarmIntent.putExtras(intent);
            getApplication().startActivity(alarmIntent);

        }

        //Different ALARM behavior that is set when the alarm WAKE is triggered, triggers after 30 seconds
        if (data.getString(Constants.ALARM_ACTIVITY_DETECT) != null &&
                data.getString(Constants.ALARM_ACTIVITY_DETECT).equals(Constants.ALARM_ACTIVITY_DETECT)) {
            Log.d(TAG, "Alarm Activity Detect");
            Log.d(TAG, "Activity Count: " + editor.getInt(Constants.ACTIVE_COUNTER, 0));
            if(editor.getInt(Constants.ACTIVE_COUNTER, 0) == 0) {
                Log.d(TAG, "User inactive for " + Constants.AFTER_WAKE_TIMER/Constants.MILLIS_IN_A_MINUTE + " minutes.");
            } else {
                editor.edit().putInt(Constants.ACTIVE_COUNTER, 0).apply();
                editor.edit().putInt(Constants.INACTIVE_COUNTER, 0).apply();
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent,flags,startId);
    }
}