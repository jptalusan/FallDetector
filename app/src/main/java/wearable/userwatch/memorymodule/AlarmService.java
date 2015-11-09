package wearable.userwatch.memorymodule;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

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
        String message = intent.getStringExtra("Message");
        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = pm.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "TAG");
        wakeLock.acquire();
        //TODO: Perform required tasks here (alarm phone show notification)

        Log.d(TAG, "onHandleIntent: " + message);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent,flags,startId);
    }
}