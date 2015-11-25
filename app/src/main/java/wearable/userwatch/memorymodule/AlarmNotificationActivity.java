package wearable.userwatch.memorymodule;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import wearable.userwatch.Constants;
import wearable.userwatch.Utils;
import wearable.userwatch.accelerometer.R;

/**
 * Created by talusan on 11/11/2015.
 */
public class AlarmNotificationActivity extends Activity {
    private static final String TAG = "AlarmNotifAct";
    AlarmManager alarmManager;
    private Button stopAlarm;
    private static AlarmNotificationActivity inst;
    private TextView alarmMessage;
    private String memoryId = "";
    private Alarm alarm;
    private PowerManager.WakeLock mWakeLock;
    private Ringtone r;
    private static final int WAKELOCK_TIMEOUT = 60 * 1000;
    private String memoryName = "";
    private SharedPreferences editor;
    private String appname;

    public static AlarmNotificationActivity instance() {
        return inst;
    }

    @Override
    public void onStart() {
        super.onStart();
        inst = this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_notification);

        Log.d("TAG", "Start Alarm Activity");
        appname = getResources().getString(R.string.app_name);
        editor = getSharedPreferences(appname, Context.MODE_PRIVATE);

        stopAlarm = (Button) findViewById(R.id.stopAlarm);
        alarmMessage = (TextView) findViewById(R.id.alarmMessage);
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        Bundle data = getIntent().getExtras();
        alarm = data.getParcelable(Constants.ALARM);
        if(alarm != null) {
            alarmMessage.setText(alarm.getMemoryInstructions());
            memoryId = alarm.getMemoryId();
            memoryName = alarm.getMemoryName();
        }

        //For different behaviors of alarms
        if(memoryName.equals(Constants.ALARM_WAKE)) {
            //TODO: trigger another alarm 30 minutes from now to get the measurement of activity counter and erase it after analysis
            startActivityDetectionAlarm();
            try {
                ArrayList<Alarm> alarms = Alarm.parseAlarmString(editor.getString("SampleAlarmString", ""));
                Alarm.cancelAllAlarms(getApplicationContext(), alarms);
                Log.d(TAG, "cancelling all alarms");
            } catch (JSONException e) {
                Log.e(TAG, "JSONException= " + e);
            }
        } else {
            Log.d(TAG, "Other types of alarm: " + memoryName);
        }

        Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmUri == null) {
            alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        r = RingtoneManager.getRingtone(getApplicationContext(), alarmUri);
        r.play();

        //Ensure wakelock release
        Runnable releaseWakelock = new Runnable() {

            @Override
            public void run() {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

                if (mWakeLock != null && mWakeLock.isHeld()) {
                    mWakeLock.release();
                }
            }
        };

        new Handler().postDelayed(releaseWakelock, WAKELOCK_TIMEOUT);

        stopAlarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alarm.stopAlarm(getApplicationContext());
                //Cancels the activityDetectionAlarm since user is awake (and cancelled the alarm)
                stopActivityDetectionAlarm();
                r.stop();
                finish();
            }
        });
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onResume() {
        super.onResume();

        // Set the window to keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        // Acquire wakelock
        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        if (mWakeLock == null) {
            mWakeLock = pm.newWakeLock((PowerManager.FULL_WAKE_LOCK | PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), TAG);
        }

        if (!mWakeLock.isHeld()) {
            mWakeLock.acquire();
            Log.i(TAG, "Wakelock aquired!!");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    public void startActivityDetectionAlarm() {
        PendingIntent pendingIntent;
        AlarmManager manager;
        Intent alarmIntent = new Intent(getApplicationContext(), AlarmService.class);
        alarmIntent.putExtra(Constants.ALARM_ACTIVITY_DETECT, Constants.ALARM_ACTIVITY_DETECT);
        pendingIntent = PendingIntent.getService(getApplicationContext(), Constants.ALARM_ACTIVITY_DETECT_ID, alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        manager = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        manager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + Constants.AFTER_WAKE_TIMER, pendingIntent);
    }

    public void stopActivityDetectionAlarm() {
        PendingIntent pendingIntent;
        AlarmManager manager;
        Intent alarmIntent = new Intent(getApplicationContext(), AlarmService.class);
        alarmIntent.putExtra(Constants.ALARM_ACTIVITY_DETECT, Constants.ALARM_ACTIVITY_DETECT);
        pendingIntent = PendingIntent.getService(getApplicationContext(), Constants.ALARM_ACTIVITY_DETECT_ID, alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        manager = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        manager.cancel(pendingIntent);
        pendingIntent.cancel();
    }
}