package wearable.userwatch.memorymodule;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

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
    public String MemoryDates[] = null;

    public Alarm(String MemoryId,
                 String MemoryName,
                 String fkUserId,
                 int MemoryFreq,
                 String MemoryInstructions,
                 String[] MemoryDates) {
        this.MemoryId = MemoryId;
        this.MemoryName = MemoryName;
        this.fkUserId = fkUserId;
        this.MemoryFreq = MemoryFreq;
        this.MemoryInstructions = MemoryInstructions;
        this.MemoryDates = MemoryDates;
    }

    //TODO: Parse alarm correctly, see notebook. (all exact one day) no repeating
    public static ArrayList<Alarm> parseAlarmString(String alarmString) throws JSONException {
        ArrayList<Alarm> alarms = new ArrayList<>();
        String MemoryId;
        String MemoryName;
        String fkUserId;
        int MemoryFreq;
        String MemoryInstructions;
        String MemoryDates[];
        String datesArray;

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

    public void setAlarm(Context context) {
        Intent alarmIntent = new Intent(context, AlarmService.class);
        PendingIntent pendingIntent;
        AlarmManager manager;
        if(MemoryDates != null) {
            //TODO: must check if memoryDate is before current, if weekly update (what to do with once and daily?)
            if(MemoryDates.length > 1)  {
                MemoryFreq = Constants.ALARM_FREQUENCY_WEEKLY;
            }

            if(MemoryFreq == Constants.ALARM_FREQUENCY_WEEKLY)  {
                Log.d(TAG, "Dates[" + MemoryDates.length + "]:" + printMemoryDates());
                for(String memoryDate : MemoryDates) {
                    int daysSinceMemoryDate = Utils.getNumberOfDaysBetweenTwoTimeStamps(Utils.convertDateAndTimeToSeconds(memoryDate),
                            Utils.getCurrentTimeStampInSeconds());
                    Log.d(TAG, "memoryDate[" + MemoryId + "]: " + memoryDate + "/" + daysSinceMemoryDate);
                    if ((daysSinceMemoryDate + 1) % Constants.DAYS_IN_A_WEEK == 0) { // + 1 since alarm should be in the future
                        alarmIntent.putExtra(Constants.ALARM, this);
                        pendingIntent = PendingIntent.getService(context, Integer.parseInt(MemoryId), alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                        manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                        manager.setExact(AlarmManager.RTC_WAKEUP, (Utils.convertDateAndTimeToSeconds(memoryDate) * 1000) + (daysSinceMemoryDate + 1) * Constants.MILLIS_IN_A_DAY, pendingIntent);
                        Log.d(TAG, "Setting weekly alarm " + MemoryId + " on: " + Utils.convertMillisToDateAndTimeString(
                                (Utils.convertDateAndTimeToSeconds(memoryDate) * 1000) + (daysSinceMemoryDate + 1) * Constants.MILLIS_IN_A_DAY));
                    } else if(daysSinceMemoryDate == 0) { // Alarm is supposed to be set today.
                        if(!Utils.isTimeStampInThePast(Utils.convertDateAndTimeToSeconds(memoryDate))) { //Check if timestamp isn't in the past
                            alarmIntent.putExtra(Constants.ALARM, this);
                            pendingIntent = PendingIntent.getService(context, Integer.parseInt(MemoryId), alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                            manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                            manager.setExact(AlarmManager.RTC_WAKEUP, Utils.convertDateAndTimeToSeconds(memoryDate) * 1000, pendingIntent);
                            Log.d(TAG, "Setting weekly alarm " + MemoryId + " on: " + Utils.convertMillisToDateAndTimeString(Utils.convertDateAndTimeToSeconds(memoryDate) * 1000));
                        } else {
                            Log.d(TAG, "Weekly alarm: " + MemoryId + " is today but has already expired.");
                        }
                    } else {
                        Log.d(TAG, "Weekly alarm: " + MemoryId + " will not be set yet.");
                    }
                }
            } else { //Once or Daily
                Log.d(TAG, "memoryDate: " + MemoryDates[0]);
                if(!Utils.isTimeStampInThePast(Utils.convertDateAndTimeToSeconds(MemoryDates[0]))) { //Check if timestamp isn't in the past
                    alarmIntent.putExtra(Constants.ALARM, this);
                    pendingIntent = PendingIntent.getService(context, Integer.parseInt(MemoryId), alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                    manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                    manager.setExact(AlarmManager.RTC_WAKEUP, Utils.convertDateAndTimeToSeconds(MemoryDates[0]) * 1000, pendingIntent);
                    Log.d(TAG, "Setting alarm " + MemoryId + ": " + MemoryDates[0]);
                } else {
                    Log.d(TAG, "Weekly alarm " + MemoryId + ": is in the past and will not be set.");
                }
            }
        } else {
            Log.d(TAG, "Alarm: " + MemoryId + " has no set dates.");
        }
    }

    public void stopAlarm(Context context) {
        Intent alarmIntent = new Intent(context, AlarmService.class);
        PendingIntent pendingIntent;
        AlarmManager manager;
        if(MemoryDates != null) {
            //TODO: must check if memoryDate is before current, if weekly update (what to do with once and daily?)
            if(MemoryDates.length > 1)  {
                MemoryFreq = Constants.ALARM_FREQUENCY_WEEKLY;
            }

            if(MemoryFreq == Constants.ALARM_FREQUENCY_WEEKLY)  {
                for(String memoryDate : MemoryDates) {
                    int daysSinceMemoryDate = Utils.getNumberOfDaysBetweenTwoTimeStamps(Utils.convertDateAndTimeToSeconds(memoryDate),
                            Utils.getCurrentTimeStampInSeconds());
                    if((daysSinceMemoryDate + 1) % Constants.DAYS_IN_A_WEEK == 0) { // + 1 since alarm should be in the future
                            alarmIntent.putExtra(Constants.ALARM, this);
                            Log.d(TAG, "Stopping weekly alarm " + MemoryId + ": " + memoryDate);
                            pendingIntent = PendingIntent.getService(context, Integer.parseInt(MemoryId), alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                            manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                            manager.cancel(pendingIntent);
                            pendingIntent.cancel();
                            Log.d(TAG, "Stopping weekly alarm " + MemoryId + " on: " + Utils.convertMillisToDateAndTimeString(
                                    (Utils.convertDateAndTimeToSeconds(memoryDate) * 1000) + (daysSinceMemoryDate + 1) * Constants.MILLIS_IN_A_DAY));
                    } else if(daysSinceMemoryDate == 0) { // Alarm was set today.
                        if(!Utils.isTimeStampInThePast(Utils.convertDateAndTimeToSeconds(MemoryDates[0]))) { //Check if timestamp isn't in the past
                            alarmIntent.putExtra(Constants.ALARM, this);
                            pendingIntent = PendingIntent.getService(context, Integer.parseInt(MemoryId), alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                            manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                            manager.setExact(AlarmManager.RTC_WAKEUP, Utils.convertDateAndTimeToSeconds(memoryDate) * 1000, pendingIntent);
                            Log.d(TAG, "Stopping weekly alarm " + MemoryId + " on: " + Utils.convertMillisToDateAndTimeString(Utils.convertDateAndTimeToSeconds(memoryDate) * 1000));
                        } else {
                            Log.d(TAG, "Weekly alarm: " + MemoryId + " was today and expired, so not set.");
                        }
                    } else {
                        Log.d(TAG, "Weekly alarm: " + MemoryId + " was not yet set.");
                    }
                }
            } else { //Once or Daily
                if(!Utils.isTimeStampInThePast(Utils.convertDateAndTimeToSeconds(MemoryDates[0]))) { //Check if timestamp isn't in the past
                    alarmIntent.putExtra(Constants.ALARM, this);
                    Log.d(TAG, "Stopping alarm: " + MemoryId + ":" + MemoryDates[0]);
                    pendingIntent = PendingIntent.getService(context, Integer.parseInt(MemoryId), alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                    manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                    manager.cancel(pendingIntent);
                    pendingIntent.cancel();
                } else {
                    Log.d(TAG, "Weekly alarm: " + MemoryId + " is expired and is not set.");
                }
            }
        } else {
            Log.d(TAG, "Alarm: " + MemoryId + " was not set.");
        }
    }

    public static void cancelAllAlarms(Context context, ArrayList<Alarm> alarmsArray) {
        for(Alarm alarm : alarmsArray) {
            Log.d(TAG, "Cancelling: " + alarm.getMemoryId());
            alarm.stopAlarm(context);
        }
    }

    public static void startAllAlarms(Context context, ArrayList<Alarm> alarmsArray) {
        for(Alarm alarm : alarmsArray) {
            alarm.setAlarm(context);
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
        return MemoryDates;
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

    public String printMemoryDates() {
        String memoryDatesString = "";
        for(String memoryDate : MemoryDates) {
            memoryDatesString += memoryDate + ", ";
        }
        return memoryDatesString;
    }

    @Override
    public String toString() {
        String memoryDatesString = "";
        if(MemoryDates != null) {
            for(String memoryDate : MemoryDates) {
                memoryDatesString += memoryDate + ", ";
            }
        }
        return "Alarm{"  +
                " MemoryId='" + MemoryId + '\'' +
                ", MemoryName='" + MemoryName + '\'' +
                ", fkUserId='" + fkUserId + '\'' +
                ", MemoryFreq=" + MemoryFreq +
                ", MemoryInstructions='" + MemoryInstructions + '\'' +
                ", MemoryDates=" + memoryDatesString +
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
        MemoryDates = in.createStringArray();
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(MemoryId);
        dest.writeString(MemoryName);
        dest.writeString(fkUserId);
        dest.writeInt(MemoryFreq);
        dest.writeString(MemoryInstructions);
        dest.writeStringArray(MemoryDates);
    }
}
