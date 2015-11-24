package wearable.userwatch;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.ParcelFormatException;
import android.util.Log;

import java.lang.reflect.Array;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import wearable.userwatch.falldetector.AccelerometerData;

public class Utils {
    private static final String TAG = "Utils";
    public static long getCurrentTimeStampInSeconds() {
        return System.currentTimeMillis() / 1000;
    }

    public static boolean isAccelerometerArrayExceedingTimeLimit(ArrayList<AccelerometerData> a, long timeLimit) {
        return (a.size() > 1) && (a.get(a.size() - 1).getTimestamp() - a.get(0).getTimestamp() > timeLimit);
    }

    //TODO: Probably better to just insert the data as normalized instead of normalizing here
    public static int getNumberOfPeaksThatExceedThreshold(ArrayList<AccelerometerData> a, double FALL_THRESHOLD) {
        int numberOfPeaksThatExceedThreshold = 0;
        int arraySize = a.size();
        for (int i = 1; i < arraySize - 1; ++i) {
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

    public static double[] getAverageAccelerationPerAxis(ArrayList<AccelerometerData> a) {
        double[] output = {0.0, 0.0, 0.0};
        for (AccelerometerData data : a) {
            output[0] += data.getX();
            output[1] += data.getY();
            output[2] += data.getZ();
        }
        output[0] = output[0]/a.size();
        output[1] = output[1]/a.size();
        output[2] = output[2]/a.size();
        return output;
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static boolean isConnectedToHome(Context context, String homeSSID) {
        WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifiManager.getConnectionInfo();
        return info != null && homeSSID.equals(info.getSSID());
    }

    public static long convertDateAndTimeToSeconds(String dateAndtime) {
        //sample: Sat Nov 07 2015 15:26:36 GMT+0800
        long timeInMilliseconds = 0;
        String givenDateString = "Tue Apr 23 16:08:28 GMT+0800";
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss z");
        try {
            Date mDate = sdf.parse(dateAndtime);
            timeInMilliseconds = mDate.getTime();
            System.out.println("Date in milli :: " + timeInMilliseconds);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return timeInMilliseconds / 1000;
    }

    //Abs of days between two millis (time and date)
    public static int getNumberOfDaysBetweenTwoTimeStamps(long a, long b) {
        return Math.abs((int)((a - b) / (60 * 60 * 24)));
    }
}
