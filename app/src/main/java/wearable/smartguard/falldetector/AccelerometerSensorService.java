package wearable.smartguard.falldetector;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by jtalusan on 10/13/2015.
 * http://stackoverflow.com/questions/5877780/orientation-from-android-accelerometer
 */
public class AccelerometerSensorService extends Service implements SensorEventListener {
    private static final String DEBUG_TAG = "D";
    private SensorManager sensorManager = null;
    private Sensor sensor = null;
    private float x, y, z = 0.0f;

    public AccelerometerSensorService() {
        super();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if(sensor != null) {
            sensorManager.registerListener(this, sensor, 1000);
        }
        Toast.makeText(this, "Service Recording", Toast.LENGTH_SHORT).show();
//        return super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        new SensorEventLoggerTask().execute(event);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private class SensorEventLoggerTask extends
            AsyncTask<SensorEvent, Void, Void> {
        @Override
        protected Void doInBackground(SensorEvent... events) {
            SensorEvent event = events[0];
            x = event.values[0];
            y = event.values[1];
            z = event.values[2];
            Log.d(DEBUG_TAG, "Sensor: " + x + "," + y + "," + z);
            return null;
        }
    }
}
