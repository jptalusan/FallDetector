package wearable.userwatch.falldetector;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;

import sqlitedb.SQLiteDataLogger;
import wearable.userwatch.Constants;
import wearable.userwatch.Utils;
import wearable.userwatch.accelerometer.R;

/**
 * Created by jtalusan on 10/13/2015.
 * http://stackoverflow.com/questions/5877780/orientation-from-android-accelerometer
 * https://github.com/AndroidExamples/android-sensor-example/blob/master/app/src/main/java/be/hcpl/android/sensors/service/SensorBackgroundService.java
 * http://developer.android.com/training/articles/perf-tips.html
 */
public class AccelerometerSensorService extends IntentService implements SensorEventListener,
        SQLiteDataLogger.AsyncResponse {
    private static final String DEBUG_TAG = "AccelService";
    private final ArrayList<UserFallListener> mListeners = new ArrayList<>();
    private SensorManager sensorManager = null;
    private Sensor sensor = null;
    private ArrayList<AccelerometerData> accelerometerData;
    private ArrayList<AccelerometerData> activityProtocolData;
    private ArrayList<AccelerometerData> activityProtocolRawData;
    private ArrayList<AccelerometerData> potentiallyFallenData;
    private ArrayList<AccelerometerData> potentiallyFallenRawData;
    private float x, y, z = 0.0f;
    private boolean potentiallyFallen = false;
    private float[] currentOrientationValues = {0.0f, 0.0f, 0.0f};
    private float[] currentAccelerationValues = {0.0f, 0.0f, 0.0f};
    private float old_x = 0.0f;
    private float old_y = 0.0f;
    private float old_z = 0.0f;

    private SharedPreferences editor;

    public AccelerometerSensorService() {
        super("AccelerometerSensorService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(DEBUG_TAG, "onCreate");
        accelerometerData = new ArrayList<>();
        potentiallyFallenData = new ArrayList<>();
        activityProtocolData = new ArrayList<>();
        activityProtocolRawData = new ArrayList<>();
        potentiallyFallenRawData = new ArrayList<>();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        String appname = getApplicationContext().getResources().getString(R.string.app_name);
        editor = getApplicationContext().getSharedPreferences(appname, Context.MODE_PRIVATE);

        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        Toast.makeText(this, "Service Recording", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // This describes what will happen when service is triggered
    }

    //TODO: Fix this, to just stop device gathering
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(DEBUG_TAG, "Start gathering v3");
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(DEBUG_TAG, "onDestroy");
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (editor.getInt(Constants.PREFS_SOS_PROTOCOL_ACTIVITY,
                Constants.SOS_PROTOCOL_ACTIVITY_OFF) == Constants.SOS_PROTOCOL_ACTIVITY_OFF) { //TODO: Should turn SOS ON in other method
            float[] rawAcceleration = event.values.clone();

            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                currentOrientationValues[0] = event.values[0] * 0.1f + currentOrientationValues[0] * (1.0f - 0.1f);
                currentOrientationValues[1] = event.values[1] * 0.1f + currentOrientationValues[1] * (1.0f - 0.1f);
                currentOrientationValues[2] = event.values[2] * 0.1f + currentOrientationValues[2] * (1.0f - 0.1f);

                currentAccelerationValues[0] = event.values[0] - currentOrientationValues[0];
                currentAccelerationValues[1] = event.values[1] - currentOrientationValues[1];
                currentAccelerationValues[2] = event.values[2] - currentOrientationValues[2];

                x = currentAccelerationValues[0] - old_x;
                y = currentAccelerationValues[1] - old_y;
                z = currentAccelerationValues[2] - old_z;

                old_x = currentAccelerationValues[0];
                old_y = currentAccelerationValues[1];
                old_z = currentAccelerationValues[2];
            }

            float[] processedAcceleration = {x, y, z};

            //ACTIVITY SENSOR
            switch (getCurrentUserActivity(rawAcceleration, processedAcceleration)) {
                case Constants.ACT_PROTOCOL_GATHERING_DATA:
                    break;
                case Constants.ACT_PROTOCOL_ACTIVE_ACTIVE:
                    Log.d(DEBUG_TAG, "Active: Active");
                    break;
                case Constants.ACT_PROTOCOL_ACTIVE_VERY_ACTIVE:
                    Log.d(DEBUG_TAG, "Active: Very Active");
                    break;
                case Constants.ACT_PROTOCOL_INACTIVE_HORIZONTAL:
                    Log.d(DEBUG_TAG, "Inactive: Horizontal");
                    break;
                case Constants.ACT_PROTOCOL_INACTIVE_VERTICAL:
                    Log.d(DEBUG_TAG, "Inactive: Vertical");
                    break;
                default:
                    break;
            }
            //END ACTIVITY SENSOR

            //START FALL DETECTOR
            if(hasUserFallen(rawAcceleration, processedAcceleration)) {
                //TODO: Prompt user if they are ok.
                Log.d(DEBUG_TAG, "Actual Fall! Ave movement:" + Utils.getAverageNormalizedAcceleration(accelerometerData));
                sensorManager.unregisterListener(this);
                SQLiteDataLogger logger = new SQLiteDataLogger(this);
                logger.execute(accelerometerData);
                logger.delegate = this;
            }
            //END FALL DETECTOR
        } else { //Reset flags
            accelerometerData.clear();
            activityProtocolRawData.clear();
            potentiallyFallenRawData.clear();
            potentiallyFallenData.clear();
            potentiallyFallen = false;
        }
    }

    private boolean hasUserFallen(float[] rawAcceleration, float[] processedAcceleration) {
        boolean hasUserFallen = false;
        if (!Utils.isAccelerometerArrayExceedingTimeLimit(accelerometerData, Constants.FALL_DETECT_WINDOW_SECS) && !potentiallyFallen) {
            AccelerometerData a = new AccelerometerData(Utils.getCurrentTimeStampInMillis(), processedAcceleration[0], processedAcceleration[1], processedAcceleration[2]);
            accelerometerData.add(a);
        } else if (potentiallyFallen) {
            Log.d(DEBUG_TAG, "Start Potential Fall Cycle : " + Utils.getAverageNormalizedAcceleration(accelerometerData));
            if (!Utils.isAccelerometerArrayExceedingTimeLimit(accelerometerData, Constants.VERIFY_FALL_DETECT_WINDOW_SECS)) {
                AccelerometerData a = new AccelerometerData(Utils.getCurrentTimeStampInMillis(), x, y, z);
                accelerometerData.add(a);
                AccelerometerData raw = new AccelerometerData(Utils.getCurrentTimeStampInMillis(), rawAcceleration[0], rawAcceleration[1], rawAcceleration[2]);
                Log.d(DEBUG_TAG, "Raw:" + rawAcceleration[0] + "," + rawAcceleration[1] + "," + rawAcceleration[2]);
                potentiallyFallenRawData.add(raw);
            } else { //Not moving past MOVE_THRESHOLD after 10 seconds
                Log.d(DEBUG_TAG, "End of 10 second potential fall cycle");
                double[] rawAccelerometerData = Utils.getAverageAccelerationPerAxis(potentiallyFallenRawData);
                Log.d(DEBUG_TAG, "Raw:" + rawAccelerometerData[0] + "," + rawAccelerometerData[1] + "," + rawAccelerometerData[2]);
                if (Utils.getAverageNormalizedAcceleration(accelerometerData) > Constants.MOVE_THRESHOLD) {
                    Log.d(DEBUG_TAG, "Ave: " + Utils.getAverageNormalizedAcceleration(accelerometerData));
                    potentiallyFallen = false;
                    Log.d(DEBUG_TAG, "False alarm 1");
                    accelerometerData.clear();
                } else if (checkIfDeviceIsHorizontalToGround(rawAccelerometerData)) {
                    //Do not erase accelerometer data here
                    hasUserFallen = true;
                } else { //TODO: just catch inadvertent conditions (RESET)
                    potentiallyFallen = false;
                    Log.d(DEBUG_TAG, "False alarm 2");
                    accelerometerData.clear();
                }
                potentiallyFallenData.clear();
            }
        } else {
            Log.d(DEBUG_TAG, "End of 5 second detection cycle.");
            int potentialFallCounter = Utils.getNumberOfPeaksThatExceedThreshold(accelerometerData, Constants.FALL_THRESHOLD);
            Log.d(DEBUG_TAG, "potential fall count: " + potentialFallCounter);
            if (potentialFallCounter > Constants.LOWER_LIMIT_PEAK_COUNT && potentialFallCounter < Constants.UPPER_LIMIT_PEAK_COUNT) {
                potentiallyFallenData = accelerometerData;
                potentiallyFallenData.clear();
                potentiallyFallen = true;
                Log.d(DEBUG_TAG, "Tagged as potential fall, switching to 10 second cycle");
            }
            accelerometerData.clear();
        }
        return hasUserFallen;
    }

    private int getCurrentUserActivity(float[] rawAcceleration, float[] processedAcceleration) {
        int currentActivity;
        if (!Utils.isAccelerometerArrayExceedingTimeLimit(activityProtocolRawData, Constants.CHARACTERIZE_ACTIVITY_WINDOW_SECS)) {
            activityProtocolRawData.add(new AccelerometerData(Utils.getCurrentTimeStampInMillis(), rawAcceleration[0], rawAcceleration[1], rawAcceleration[2]));
            activityProtocolData.add(new AccelerometerData(Utils.getCurrentTimeStampInMillis(), processedAcceleration[0], processedAcceleration[1], processedAcceleration[2]));
            currentActivity = Constants.ACT_PROTOCOL_GATHERING_DATA;
        } else { //End of activity protocol sensor window (CHARACTERIZE_ACTIVITY_WINDOW_SECS)
            Log.d(DEBUG_TAG, "End of characterizing activity window");
            double[] rawActivityProtocolAccelerationPerAxis = Utils.getAverageAccelerationPerAxis(activityProtocolRawData);
            Log.d(DEBUG_TAG, "Ave for activity: " + Utils.getAverageNormalizedAcceleration(activityProtocolData));
            if ((Utils.getAverageNormalizedAcceleration(activityProtocolData) > Constants.ACTIVE_THRESHOLD) &&
                    Utils.getAverageNormalizedAcceleration(activityProtocolData) < Constants.VERY_ACTIVE_THRESHOLD) { //ACTIVE
                currentActivity = Constants.ACT_PROTOCOL_ACTIVE_ACTIVE;
            } else if(Utils.getAverageNormalizedAcceleration(activityProtocolData) > Constants.VERY_ACTIVE_THRESHOLD) { //VERY ACTIVE
                currentActivity = Constants.ACT_PROTOCOL_ACTIVE_VERY_ACTIVE;
            } else { //INACTIVE
                if(checkIfDeviceIsHorizontalToGround(rawActivityProtocolAccelerationPerAxis)) {
                    currentActivity = Constants.ACT_PROTOCOL_INACTIVE_HORIZONTAL;
                } else {
                    currentActivity = Constants.ACT_PROTOCOL_INACTIVE_VERTICAL;
                }
            }
            //TODO: Log to SQLiteDB
            activityProtocolRawData.clear();
            activityProtocolData.clear();
        }
        return currentActivity;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void processIsFinished(boolean output) {
        if (output) {
//            editor.edit().putInt(Constants.PREFS_SOS_PROTOCOL_ACTIVITY, Constants.SOS_PROTOCOL_ACTIVITY_ON).apply();
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

    private boolean checkIfDeviceIsHorizontalToGround(double[] averageAcceleration) {
        double hypotenuse = Math.sqrt(Math.pow(averageAcceleration[1], 2) + Math.pow(averageAcceleration[2], 2));
        if(hypotenuse > (Constants.GRAVITY * Constants.EIGHTYPERCENT)) {
            Log.d(DEBUG_TAG, "Hypotenuse: " + hypotenuse);
            return true;
        } else {
            return false;
        }
    }
}
