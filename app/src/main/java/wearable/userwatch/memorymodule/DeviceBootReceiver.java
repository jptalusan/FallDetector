package wearable.userwatch.memorymodule;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

/**
 * Created by talusan on 11/9/2015.
 * Turns on AlarmService after device reboot
 * Get alarm data from SQLite or sharedprefs then run them
 */
public class DeviceBootReceiver extends BroadcastReceiver {
    private static final String TAG = "DeviceBootReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            Intent alarmIntent = new Intent(context, AlarmService.class);
            PendingIntent pendingIntent = PendingIntent.getService(context, 1001, alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
//            manager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), Constants.SCHEDULE_INTERVAL, pendingIntent);
//            manager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000, 1000, pendingIntent);
            Log.d(TAG, "Alarm set after reboot");
        } else {
            Log.d(TAG, "No alarm set after reboot");
        }
    }
}
