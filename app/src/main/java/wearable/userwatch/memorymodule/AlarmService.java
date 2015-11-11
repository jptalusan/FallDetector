package wearable.userwatch.memorymodule;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;

import wearable.userwatch.Constants;

/**
 * Created by talusan on 11/9/2015.
 * http://stackoverflow.com/questions/24724859/alarmmanager-setexact-with-wakefulbroadcastreceiver-sometimes-not-exact
 * http://developer.android.com/training/scheduling/alarms.html
 * TODO: If user has fallen (see accelerometer flag) then do not issue alarms
 */
public class AlarmService extends IntentService {
    private static final String TAG = "Wearable.AlarmService";
    public AlarmService() {
        super("ScheduledService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate:AlarmService");
    }

    @Override
    protected  void onHandleIntent(Intent intent) {
        Bundle data = intent.getExtras();
        Alarm alarm = data.getParcelable(Constants.ALARM);

        Log.d(TAG, "AlarmId: " + alarm.getMemoryId());

        //TODO: Add checking if alarm notification activity is currently open.
        Intent alarmIntent = new Intent(getBaseContext(), AlarmNotificationActivity.class);
        alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        alarmIntent.putExtras(intent);
        getApplication().startActivity(alarmIntent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent,flags,startId);
    }
}