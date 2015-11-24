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
 * TODO: If user has fallen (see accelerometer flag) then do not issue alarms
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
        if(data.getParcelable(Constants.ALARM) != null) {
            Alarm alarm = data.getParcelable(Constants.ALARM);
            Log.d(TAG, "AlarmId: " + alarm.getMemoryId());
            if (alarm.getMemoryDates() != null) {
                for (int i = 0; i < alarm.getMemoryDates().length; ++i) {
                    Log.d(TAG, "MemoryDates: " + alarm.getMemoryDates()[i]);
                    Log.d(TAG, "Day diff from today: " + Utils.getNumberOfDaysBetweenTwoTimeStamps(Utils.convertDateAndTimeToSeconds(alarm.getMemoryDates()[i]),
                            Utils.getCurrentTimeStampInSeconds()));
                }
            }
            //TODO: Add checking if alarm notification activity is currently open.
            Intent alarmIntent = new Intent(getBaseContext(), AlarmNotificationActivity.class);
            alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            alarmIntent.putExtras(intent);
            getApplication().startActivity(alarmIntent);

        }
        if (data.getString(Constants.ALARM_ACTIVITY_DETECT) != null &&
                data.getString(Constants.ALARM_ACTIVITY_DETECT).equals(Constants.ALARM_ACTIVITY_DETECT)) {
            Log.d(TAG, "Alarm Activity Detect");
            Log.d(TAG, "Activity Count: " + editor.getInt(Constants.ACTIVE_COUNTER, 0));
            if(editor.getInt(Constants.ACTIVE_COUNTER, 0) == 0) {
                Log.d(TAG, "User inactive for " + Constants.AFTER_WAKE_TIMER/1000 + " minutes.");
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent,flags,startId);
    }
}