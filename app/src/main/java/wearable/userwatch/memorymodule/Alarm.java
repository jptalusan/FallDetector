package wearable.userwatch.memorymodule;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import wearable.userwatch.Constants;

/**
 * Created by talusan on 11/9/2015.
 */
public class Alarm implements Parcelable {
    private static final String TAG = "AlarmObject";
    public String MemoryId = "";
    public String MemoryName = "";
    public String fkUserId = "";
    public int MemoryFreq = 0;
    public String MemoryInstructions = "";
//    public ArrayList<String> memoryDates;
    public String datesArray = "";

    public Alarm(String MemoryId,
                 String MemoryName,
                 String fkUserId,
                 int MemoryFreq,
                 String MemoryInstructions,
                 String datesArray) {
        this.MemoryId = MemoryId;
        this.MemoryName = MemoryName;
        this.fkUserId = fkUserId;
        this.MemoryFreq = MemoryFreq;
        this.MemoryInstructions = MemoryInstructions;
        this.datesArray = datesArray;
    }

    //TODO: Parse alarm correctly, see notebook. (all exact one day) no repeating
    public static ArrayList<Alarm> parseAlarmString(String alarmString) throws JSONException {
        ArrayList<Alarm> alarms = new ArrayList<>();
        String MemoryId = "";
        String MemoryName = "";
        String fkUserId = "";
        int MemoryFreq = 0;
        String MemoryInstructions = "";
        ArrayList<String> MemoryDates = new ArrayList<>();
        String datesArray = "";

        JSONObject memoriesObject = new JSONObject(alarmString);
        JSONArray memoriesArray = memoriesObject.getJSONArray(Constants.MEMORIES);
        for (int i = 0; i < memoriesArray.length(); ++i) {
            JSONObject memoryObject = (JSONObject)memoriesArray.get(i);
            MemoryId = memoryObject.getString(Constants.MEMORIES_MEMORYID);
            MemoryName = memoryObject.getString(Constants.MEMORIES_MEMORYNAME);
            fkUserId = memoryObject.getString(Constants.MEMORIES_FKUSERID);
            MemoryFreq = memoryObject.getInt(Constants.MEMORIES_MEMORYFREQ);
            MemoryInstructions = memoryObject.getString(Constants.MEMORIES_MEMORYINSTRUCTIONS);
            datesArray = memoryObject.getString(Constants.MEMORIES_MEMORYDATES);

//            if(!datesArray.equals("null")) {
//                MemoryDates = new  ArrayList<String>(Arrays.asList(datesArray.split(",")));
//            }

            Alarm alarmMessage = new Alarm
                    (MemoryId, MemoryName, fkUserId,
                            MemoryFreq, MemoryInstructions, datesArray);

            alarms.add(alarmMessage);
        }
        return alarms;
    }

    private boolean isAlarmStringValid(String alarmString) {
        return true;
    }


    public void startAlarm(Context context) {
        PendingIntent pendingIntent;
        AlarmManager manager;
        Intent alarmIntent = new Intent(context, AlarmService.class);
        alarmIntent.putExtra(Constants.ALARM, this);
        Log.d(TAG, "Starting alarm: " + MemoryId);
        pendingIntent = PendingIntent.getService(context, Integer.parseInt(MemoryId), alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        //TODO: if once only, use setexact, if repeating set corresponding interval in setRepeating
//        manager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 10000, 5000, pendingIntent);
        manager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 10000, pendingIntent);
        Log.d(TAG, "Alarm is started");
    }

    public void stopAlarm(Context context) {
        PendingIntent pendingIntent;
        AlarmManager manager;
        Intent alarmIntent = new Intent(context, AlarmService.class);
        alarmIntent.putExtra(Constants.ALARM, this);
        Log.d(TAG, "Stopping alarm: " + MemoryId);
        pendingIntent = PendingIntent.getService(context, Integer.parseInt(MemoryId), alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        manager.cancel(pendingIntent);
        pendingIntent.cancel();
        Log.d(TAG, "Alarm is stopped");
    }

    public static void cancelAllAlarms(Context context, ArrayList<Alarm> alarmsArray) {
        for(Alarm alarm : alarmsArray) {
            Log.d(TAG, "Cancelling: " + alarm.getMemoryId());
            alarm.stopAlarm(context);
        }
    }

    public static void startAllAlarms(Context context, ArrayList<Alarm> alarmsArray) {
        for(Alarm alarm : alarmsArray) {
            alarm.startAlarm(context);
        }
    }

    public void resetAlarm() {

    }

    public String getMemoryId() {
        return MemoryId;
    }

    public String getMemoryName() {
        return MemoryName;
    }

    public String getFkUserId() {
        return fkUserId;
    }

    public int getMemoryFreq() {
        return MemoryFreq;
    }

    public String getMemoryInstructions() {
        return MemoryInstructions;
    }

    @Override
    public String toString() {
        return "Alarm{" +
                ", MemoryId='" + MemoryId + '\'' +
                ", MemoryName='" + MemoryName + '\'' +
                ", fkUserId='" + fkUserId + '\'' +
                ", MemoryFreq=" + MemoryFreq +
                ", MemoryInstructions='" + MemoryInstructions + '\'' +
                ", memoryDates=" + datesArray +
                '}';
    }

    // Parcelling part
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public Alarm createFromParcel(Parcel in) {
            return new Alarm(in);
        }

        public Alarm[] newArray(int size) {
            return new Alarm[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }


    private Alarm(Parcel in) {
        MemoryId = in.readString();
        MemoryName = in.readString();
        fkUserId = in.readString();
        MemoryFreq = in.readInt();
        MemoryInstructions = in.readString();
        datesArray = in.readString();
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(MemoryId);
        dest.writeString(MemoryName);
        dest.writeString(fkUserId);
        dest.writeInt(MemoryFreq);
        dest.writeString(MemoryInstructions);
        dest.writeString(datesArray);
    }
}
