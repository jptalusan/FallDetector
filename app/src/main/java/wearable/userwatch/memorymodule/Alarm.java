package wearable.userwatch.memorymodule;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

import java.util.Calendar;

/**
 * Created by talusan on 11/9/2015.
 */
public class Alarm {
    private static final String TAG = "AlarmObject";
    private String alarmMessage;
    private Calendar cal;
    private String frequency;
    private Context context;

    public Alarm(Context context, String alarmString) {
        this.context = context;
        if (isAlarmStringValid(alarmString)) {
            parseAlarm(alarmString);
        }
    }

    private void parseAlarm(String alarmString) {
        //TODO: Parse alarm here, into message, time/date and frequency
        this.alarmMessage = alarmString;
    }

    private boolean isAlarmStringValid(String alarmString) {
        return true;
    }

    public String getAlarmMessage() {
        return alarmMessage;
    }

    public void startAlarm() {
        PendingIntent pendingIntent;
        AlarmManager manager;
        Intent alarmIntent = new Intent(context, AlarmService.class);
        alarmIntent.putExtra("Message", alarmMessage);
        pendingIntent = PendingIntent.getService(context, 1001, alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        //TODO: if once only, use setexact, if repeating set corresponding interval in setRepeating
        manager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 10000, 5000, pendingIntent);
        Log.d(TAG, "Alarm is started");
    }
}
