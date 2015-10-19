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
public class AccelerometerSensorService extends Service implements SensorEventListener,
        SQLiteDataLogger.AsyncResponse {
    private static final String DEBUG_TAG = "AccelService";
    private final ArrayList<UserFallListener> mListeners = new ArrayList<>();
    private SensorManager sensorManager = null;
    private Sensor sensor = null;
    private ArrayList<AccelerometerData> accelerometerData;
    private ArrayList<AccelerometerData> potentiallyFallenData;
    private float x, y, z = 0.0f;
    private int potentialFallCounter = 0;
    private boolean potentiallyFallen = false;
    private boolean actuallyFallen = false;
    //端末が実際に取得した加速度値。重力加速度も含まれる。This values include gravity force.
    private float[] currentOrientationValues = {0.0f, 0.0f, 0.0f};
    //ローパス、ハイパスフィルタ後の加速度値 Values after low pass and high pass filter
    private float[] currentAccelerationValues = {0.0f, 0.0f, 0.0f};
    //previous data 1つ前の値
    private float old_x = 0.0f;
    private float old_y = 0.0f;
    private float old_z = 0.0f;

    public AccelerometerSensorService() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        accelerometerData = new ArrayList<>();
        potentiallyFallenData = new ArrayList<>();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

//        if (sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null) {
//            sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
//        } else {
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
//        }

        Toast.makeText(this, "Service Recording", Toast.LENGTH_SHORT).show();
    }

    //TODO: Fix this, to just stop device gathering
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(DEBUG_TAG, "Start gathering");
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);

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
        //https://gist.github.com/tomoima525/8395322 - Remove gravity factor
        new CharacterizeActivityTask().execute(event);

        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            x = event.values[0];
            y = event.values[1];
            z = event.values[2];
        } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // ローパスフィルタで重力値を抽出　Isolate the force of gravity with the low-pass filter.
            currentOrientationValues[0] = event.values[0] * 0.1f + currentOrientationValues[0] * (1.0f - 0.1f);
            currentOrientationValues[1] = event.values[1] * 0.1f + currentOrientationValues[1] * (1.0f - 0.1f);
            currentOrientationValues[2] = event.values[2] * 0.1f + currentOrientationValues[2] * (1.0f - 0.1f);

            // 重力の値を省くRemove the gravity contribution with the high-pass filter.
            currentAccelerationValues[0] = event.values[0] - currentOrientationValues[0];
            currentAccelerationValues[1] = event.values[1] - currentOrientationValues[1];
            currentAccelerationValues[2] = event.values[2] - currentOrientationValues[2];

            // ベクトル値を求めるために差分を計算　diff for vector
            x = currentAccelerationValues[0] - old_x;
            y = currentAccelerationValues[1] - old_y;
            z = currentAccelerationValues[2] - old_z;

            // 状態更新
            old_x = currentAccelerationValues[0];
            old_y = currentAccelerationValues[1];
            old_z = currentAccelerationValues[2];
        }

        if (!Utils.isAccelerometerArrayExceedingTimeLimit(accelerometerData, Constants.FALL_DETECT_WINDOW_SECS) && !potentiallyFallen) {
            AccelerometerData a = new AccelerometerData(Utils.getCurrentTimeStampInMillis(), x, y, z);
            accelerometerData.add(a);
//            Log.d(DEBUG_TAG, a.toString() + "/" + a.getNormalizedAcceleration());
        } else if (potentiallyFallen) {
            Log.d(DEBUG_TAG, "Start Potential Fall Cycle : " + Utils.getAverageNormalizedAcceleration(accelerometerData));
            if (!Utils.isAccelerometerArrayExceedingTimeLimit(accelerometerData, Constants.VERIFY_FALL_DETECT_WINDOW_SECS)) {
                AccelerometerData a = new AccelerometerData(Utils.getCurrentTimeStampInMillis(), x, y, z);
                accelerometerData.add(a);
            } else { //Not moving past MOVE_THRESHOLD after 10 seconds
                Log.d(DEBUG_TAG, "End of 10 second potential fall cycle");
                if (Utils.getAverageNormalizedAcceleration(accelerometerData) > Constants.MOVE_THRESHOLD) {
                    Log.d(DEBUG_TAG, "Ave: " + Utils.getAverageNormalizedAcceleration(accelerometerData));
                    potentiallyFallen = false;
                    Log.d(DEBUG_TAG, "False alarm");
                    accelerometerData.clear();
                } else {
                    actuallyFallen = true;
                    //TODO: Prompt user if they are ok.
                    Log.d(DEBUG_TAG, "Actual Fall! Ave movement:" + Utils.getAverageNormalizedAcceleration(accelerometerData));
                    sensorManager.unregisterListener(this); //TEST
                    //TODO: Log potentiallyFallenData
                    SQLiteDataLogger logger = new SQLiteDataLogger(this);
                    logger.execute(accelerometerData);
                    logger.delegate = this;
                }
                potentiallyFallenData.clear();
            }
        } else if (actuallyFallen) {
            sensorManager.unregisterListener(this);
            Log.d(DEBUG_TAG, "Call contacts");
        } else {
            Log.d(DEBUG_TAG, "End of 5 second detection cycle.");
            potentialFallCounter = Utils.getNumberOfPeaksThatExceedThreshold(accelerometerData, Constants.FALL_THRESHOLD);
            Log.d(DEBUG_TAG, "potential fall count: " + potentialFallCounter);
            if (potentialFallCounter > Constants.LOWER_LIMIT_PEAK_COUNT && potentialFallCounter < Constants.UPPER_LIMIT_PEAK_COUNT) {
                potentiallyFallenData = accelerometerData;
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

    @Override
    public void processIsFinished(boolean output) {
        if (output) {
            Log.d(DEBUG_TAG, "Successfully saved to DB.");
        } else {
            Log.d(DEBUG_TAG, "Failed to save to DB, please try again.");
        }
    }

    /**
     * Calls registered event listeners
     */
    private void notifyListeners(int activity) {
        if (activity == 0) return;
        for (UserFallListener listener : mListeners) {
            listener.onUserFall(activity);
            Log.d(DEBUG_TAG, String.valueOf(activity));
        }
    }

    public interface UserFallListener {
        /**
         * Called when leg state have changed
         */
        void onUserFall(int activity);
    }
}
