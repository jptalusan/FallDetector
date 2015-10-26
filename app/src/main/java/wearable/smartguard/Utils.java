package wearable.smartguard;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.SystemClock;
import android.util.Log;

import java.util.ArrayList;

import wearable.smartguard.falldetector.AccelerometerData;

public class Utils {
    private static final String TAG = "Utils";
    public static long getCurrentTimeStampInMillis() {
        return System.currentTimeMillis() / 1000;
    }

    public static boolean isAccelerometerArrayExceedingTimeLimit(ArrayList<AccelerometerData> a, long timeLimit) {
        return (a.size() > 1) && (a.get(a.size() - 1).getTimestamp() - a.get(0).getTimestamp() > timeLimit);
    }

    //TODO: Probably better to just insert the data as normalized instead of normalizing here
    public static int getNumberOfPeaksThatExceedThreshold(ArrayList<AccelerometerData> a, double FALL_THRESHOLD) {
        int numberOfPeaksThatExceedThreshold = 0;
        for (int i = 1; i < a.size() - 1; ++i) {
            double prev = a.get(i - 1).getNormalizedAcceleration();
            double curr = a.get(i).getNormalizedAcceleration();
            double next = a.get(i + 1).getNormalizedAcceleration();

            if (curr > prev && curr > next) {
                if (a.get(i).getNormalizedAcceleration() > FALL_THRESHOLD) {
                    Log.d("Utils", "Peak: " + a.get(i).getNormalizedAcceleration() + "/" + FALL_THRESHOLD);
                    ++numberOfPeaksThatExceedThreshold;
                }
            }
        }
        return numberOfPeaksThatExceedThreshold;
    }

    public static double getAverageNormalizedAcceleration(ArrayList<AccelerometerData> a) {
        double average = 0.0;
        for (AccelerometerData data : a) {
            average += data.getNormalizedAcceleration();
        }
        return average / a.size();
    }

    public static int identifyWhichAxisIsOrthogonalToGravity(ArrayList<AccelerometerData> a) {
        float xAverage = 0.0f;
        float yAverage = 0.0f;
        float zAverage = 0.0f;

        for (AccelerometerData data : a) {
            xAverage += Math.abs(data.getX());
            yAverage += Math.abs(data.getY());
            zAverage += Math.abs(data.getZ());
        }

        xAverage = xAverage / a.size();
        yAverage = yAverage / a.size();
        zAverage = zAverage / a.size();


        if (xAverage > yAverage && xAverage > zAverage) {
            return 0;
        } else if (yAverage > xAverage && yAverage > zAverage) {
            return 1;
        } else {
            return 2;
        }
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetworkInfo != null) {
//            Log.d(TAG, activeNetworkInfo.toString());
            return activeNetworkInfo.isConnected();
        } else {
            return false;
        }
    }

    public static boolean isConnectedToHome(Context context, String homeSSID) {
        WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifiManager.getConnectionInfo();
//        Log.d(TAG, info.getSSID());
        return info != null && homeSSID.equals(info.getSSID());
    }
}
