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
import wearable.userwatch.Utils;

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
    public String memoryDates[] = null;
//    public String datesArray = "";

    public Alarm(String MemoryId,
                 String MemoryName,
                 String fkUserId,
                 int MemoryFreq,
                 String MemoryInstructions,
                 String[] memoryDates) {
        this.MemoryId = MemoryId;
        this.MemoryName = MemoryName;
        this.fkUserId = fkUserId;
        this.MemoryFreq = MemoryFreq;
        this.MemoryInstructions = MemoryInstructions;
        this.memoryDates = memoryDates;
//        this.datesArray = datesArray;
    }

    //TODO: Parse alarm correctly, see notebook. (all exact one day) no repeating
    public static ArrayList<Alarm> parseAlarmString(String alarmString) throws JSONException {
        ArrayList<Alarm> alarms = new ArrayList<>();
        String MemoryId = "";
        String MemoryName = "";
        String fkUserId = "";
        int MemoryFreq = 0;
        String MemoryInstructions = "";
        String MemoryDates[] = null;
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

            if(!datesArray.equals("null")) {
                MemoryDates = datesArray.split(",");
            } else {
                MemoryDates = null;
            }

            Alarm alarmMessage = new Alarm
                    (MemoryId, MemoryName, fkUserId,
                            MemoryFreq, MemoryInstructions, MemoryDates);

            alarms.add(alarmMessage);
        }
        return alarms;
    }

    private boolean isAlarmStringValid(String alarmString) {
        return true;
    }


    public void startAlarm(Context context) {
        if(memoryDates != null) {
            PendingIntent pendingIntent;
            AlarmManager manager;
            Intent alarmIntent = new Intent(context, AlarmService.class);
            alarmIntent.putExtra(Constants.ALARM, this);
            Log.d(TAG, "Starting alarm: " + MemoryId);
            pendingIntent = PendingIntent.getService(context, Integer.parseInt(MemoryId), alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            //TODO: must check if memoryDate is before current, if weekly update (what to do with once and daily?)
            //If time is before current, it alarms immediately
            manager.setExact(AlarmManager.RTC_WAKEUP, Utils.convertDateAndTimeToSeconds(memoryDates[0]) * 1000, pendingIntent);
        } else {
            Log.d(TAG, "Alarm: " + MemoryId + " has no set dates.");
        }
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

    public void resetAlarm(Context context, ArrayList<Alarm> alarmsArray) {
        cancelAllAlarms(context, alarmsArray);
        startAllAlarms(context, alarmsArray);
    }

    public String getMemoryId() {
        return MemoryId;
    }

    public String getMemoryName() {
        return MemoryName;
    }

    public String[] getMemoryDates() {
        return memoryDates;
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
        String memoryDatesString = "";
        if(memoryDates != null) {
            for(String memoryDate : memoryDates) {
                memoryDatesString += memoryDate + ", ";
            }
        }
        return "Alarm{"  +
                " MemoryId='" + MemoryId + '\'' +
                ", MemoryName='" + MemoryName + '\'' +
                ", fkUserId='" + fkUserId + '\'' +
                ", MemoryFreq=" + MemoryFreq +
                ", MemoryInstructions='" + MemoryInstructions + '\'' +
                ", memoryDates=" + memoryDatesString +
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
        memoryDates = in.createStringArray();
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(MemoryId);
        dest.writeString(MemoryName);
        dest.writeString(fkUserId);
        dest.writeInt(MemoryFreq);
        dest.writeString(MemoryInstructions);
        dest.writeStringArray(memoryDates);
    }
}
