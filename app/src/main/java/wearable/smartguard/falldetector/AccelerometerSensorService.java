package wearable.smartguard.falldetector;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by jtalusan on 10/13/2015.
 * http://stackoverflow.com/questions/5877780/orientation-from-android-accelerometer
 * https://github.com/AndroidExamples/android-sensor-example/blob/master/app/src/main/java/be/hcpl/android/sensors/service/SensorBackgroundService.java
 */
public class AccelerometerSensorService extends Service implements SensorEventListener {
    private static final String DEBUG_TAG = "AccelService";
    private static final float NS2S = 1.0f / 1000000000.0f;
    private SensorManager sensorManager = null;
    private Sensor sensor = null;
    private ArrayList<AccelerometerData> accelerometerData;
    private float x, y, z = 0.0f;
    private boolean LINEAR_ACCELEROMETER = false;
    private long timestamp = 0;
    private float alpha = 0;
    private float[] gravity = new float[]{9.81f, 9.81f, 9.81f};
    private int potentialFallCounter = 0;
    private boolean potentiallyFallen = false;
    private boolean actuallyFallen = false;
    private double FALL_THRESHOLD = 18.0;
    private double MOVE_THRESHOLD = 0.9;

    public AccelerometerSensorService() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        accelerometerData = new ArrayList<>();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        if (sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null) {
            sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
            LINEAR_ACCELEROMETER = true;
        } else {
            sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            LINEAR_ACCELEROMETER = false;
        }

        Toast.makeText(this, "Service Recording", Toast.LENGTH_SHORT).show();
    }

    //TODO: Fix this, to just stop device gathering
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(DEBUG_TAG, "Start gathering");

        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST);

//        if (intent != null) {
//            boolean status = intent.getBooleanExtra("Active", false);
//
//            Log.d(DEBUG_TAG, status + "");
//            if (!status) {
//                sensorManager.unregisterListener(this);
//            } else {
//                if (sensor != null) {
//                    sensorManager.registerListener(this, sensor, 1000);
//                }
//            }
//        }
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
        if (!Utils.isAccelerometerArrayExceedingTimeLimit(accelerometerData, 5) && !potentiallyFallen) {
            if (LINEAR_ACCELEROMETER) {
                x = event.values[0];
                y = event.values[1];
                z = event.values[2];
            } else {
                // alpha is calculated as t / (t + dT)
                // with t, the low-pass filter's time-constant
                // and dT, the event delivery rate
                alpha = 0.8f;
                gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
                gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
                gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

                x = event.values[0] - gravity[0];
                y = event.values[1] - gravity[1];
                z = event.values[2] - gravity[2];
            }
            AccelerometerData a = new AccelerometerData(Utils.getCurrentTimeStampInMillis(), x, y, z);
            accelerometerData.add(a);
//            Log.d(DEBUG_TAG, a.toString());
//            Log.d(DEBUG_TAG, a.getNormalizedAcceleration() + "");
        } else if (potentiallyFallen) {
            Log.d(DEBUG_TAG, "Start Potential Fall Cycle : " + Utils.getAverageNormalizedAcceleration(accelerometerData));
            if (!Utils.isAccelerometerArrayExceedingTimeLimit(accelerometerData, 9)) {
                if (LINEAR_ACCELEROMETER) {
                    x = event.values[0];
                    y = event.values[1];
                    z = event.values[2];
                } else {
                    // alpha is calculated as t / (t + dT)
                    // with t, the low-pass filter's time-constant
                    // and dT, the event delivery rate
                    alpha = 0.8f;
                    gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
                    gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
                    gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

                    x = event.values[0] - gravity[0];
                    y = event.values[1] - gravity[1];
                    z = event.values[2] - gravity[2];
                }
                AccelerometerData a = new AccelerometerData(Utils.getCurrentTimeStampInMillis(), x, y, z);
                accelerometerData.add(a);
            } else { //Not moving past MOVE_THRESHOLD after 10 seconds
                Log.d(DEBUG_TAG, "End of 10 second potential fall cycle");
                if (Utils.getAverageNormalizedAcceleration(accelerometerData) > MOVE_THRESHOLD) {
                    Log.d(DEBUG_TAG, "Ave: " + Utils.getAverageNormalizedAcceleration(accelerometerData));
                    potentiallyFallen = false;
                    Log.d(DEBUG_TAG, "False alarm");
                } else {
                    actuallyFallen = true;
                    //TODO: Prompt user if they are ok.
                    Log.d(DEBUG_TAG, "Actual Fall!");
                    sensorManager.unregisterListener(this); //TEST
                }
            }
        } else if (actuallyFallen) {
            sensorManager.unregisterListener(this);
            Log.d(DEBUG_TAG, "Call contacts");
        } else {
            Log.d(DEBUG_TAG, "End of 5 second detection cycle.");
            potentialFallCounter = Utils.getNumberOfPeaksThatExceedThreshold(accelerometerData, FALL_THRESHOLD);
            Log.d(DEBUG_TAG, "potential fall count: " + potentialFallCounter);
            if (potentialFallCounter > 0 && potentialFallCounter < 5) {
                potentiallyFallen = true;
                Log.d(DEBUG_TAG, "Tagged as potential fall, switching to 10 second cycle");
            } else {
                Log.d(DEBUG_TAG, "No fall detected");
            }
            potentialFallCounter = 0;
            accelerometerData.clear();
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
