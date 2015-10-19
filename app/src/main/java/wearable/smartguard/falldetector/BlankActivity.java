package wearable.smartguard.falldetector;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

public class BlankActivity extends AppCompatActivity {
    private static String DEBUG_TAG = "Activity";
    private Button startStop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blank);

        //TESTING
//        SQLiteDBInterface datasource;
//        datasource = new SQLiteDBInterface(this);
//        datasource.open();
//        datasource.insertAccelerometerDataToDB(new AccelerometerData(123, 20.0f, 0.2f, 210.0f));
//        datasource.close();
        Log.d("TAG", "Start");
//        AlarmManager scheduler = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
//        Intent intent = new Intent(getApplicationContext(), AccelerometerSensorService.class );
//        intent.putExtra("Active", true);
//        PendingIntent scheduledIntent = PendingIntent.getService(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
//        scheduler.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 10000, scheduledIntent);
//
//        startStop = (Button) findViewById(R.id.startStop);
//        startStop.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent = new Intent(getApplicationContext(), AccelerometerSensorService.class );
//                intent.putExtra("Active", false);
//                AlarmManager scheduler = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
//                PendingIntent scheduledIntent = PendingIntent.getService(getApplicationContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
//                scheduler.cancel(scheduledIntent);
////                scheduler.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 10000, scheduledIntent);
////                PendingIntent scheduledIntent = PendingIntent.getService(getApplicationContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
////                AlarmManager alarmManagerstop = (AlarmManager) getSystemService(ALARM_SERVICE);
////                alarmManagerstop.cancel(scheduledIntent);
////                scheduledIntent.cancel();
//                Log.d(DEBUG_TAG, "Alarm is stopped");
//            }
//        });
        Intent intent = new Intent(getApplicationContext(), AccelerometerSensorService.class);
        startService(intent);
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
}
