package wearable.userwatch.memorymodule;

/**
 * Created by talusan on 11/9/2015.
 * http://stackoverflow.com/questions/14002692/alarmmanager-at-specific-date-and-time
 */
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        Toast.makeText(context,
                "AlarmReceiver.onReceive()",
                Toast.LENGTH_LONG).show();
    }

}