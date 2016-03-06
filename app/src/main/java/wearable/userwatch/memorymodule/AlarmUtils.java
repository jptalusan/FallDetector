package wearable.userwatch.memorymodule;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import wearable.userwatch.Constants;
import wearable.userwatch.Utils;

/**
 * Created by talusan on 1/3/2016.
 */
public class AlarmUtils {
    private static final String TAG = "AlarmUtils";

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
        String MemoryType;

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
            MemoryType = memoryObject.getString(Constants.MEMORIES_MEMORYTYPE);

            if(!datesArray.equals("null")) {
                MemoryDates = datesArray.split(",");
            } else {
                MemoryDates = null;
            }

            Alarm alarmMessage = new Alarm
                    (MemoryId, MemoryName, fkUserId,
                            MemoryFreq, MemoryInstructions, MemoryDates, MemoryType);

            alarms.add(alarmMessage);
        }
        return alarms;
    }

    public static void setAlarm(Context context, Alarm alarm) {
        Log.d(TAG, "Setting Alarm");
        Intent alarmIntent = new Intent(context, AlarmService.class);
        PendingIntent pendingIntent;
        AlarmManager manager;
        if(alarm.MemoryDates != null) {
            if(alarm.MemoryDates.length > 1)  {
                alarm.MemoryFreq = Constants.ALARM_FREQUENCY_WEEKLY;
            }

            if(alarm.MemoryFreq == Constants.ALARM_FREQUENCY_WEEKLY)  {
                Log.d(TAG, "Dates[" + alarm.MemoryDates.length + "]:" + printMemoryDates(alarm));
                int index = 0;
                for(String memoryDate : alarm.MemoryDates) {
                    int daysSinceMemoryDate = Utils.getNumberOfDaysBetweenTwoTimeStamps(Utils.convertDateAndTimeToSeconds(memoryDate),
                            Utils.getCurrentTimeStampInSeconds());
                    Log.d(TAG, "memoryDate[" + index + "]: " + memoryDate + "/" + daysSinceMemoryDate);
                    if ((daysSinceMemoryDate + 1) % Constants.DAYS_IN_A_WEEK == 0) { // + 1 since alarm should be in the future
                        alarmIntent.putExtra(Constants.ALARM, alarm);
                        pendingIntent = PendingIntent.getService(context, Integer.parseInt(alarm.MemoryId + index), alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                        manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                        manager.setExact(AlarmManager.RTC_WAKEUP, (Utils.convertDateAndTimeToSeconds(memoryDate) * 1000) + ((daysSinceMemoryDate + 1) * Constants.MILLIS_IN_A_DAY), pendingIntent);
                        Log.d(TAG, "Setting weekly alarm (a) " + alarm.MemoryId + ":" + index + " on: " + Utils.convertMillisToDateAndTimeString(
                                (Utils.convertDateAndTimeToSeconds(memoryDate) * 1000) + ((daysSinceMemoryDate + 1) * Constants.MILLIS_IN_A_DAY)));
                    } else if(daysSinceMemoryDate == 0) { // Alarm is supposed to be set today.
                        if(!Utils.isTimeStampInThePast(Utils.convertDateAndTimeToSeconds(memoryDate))) { //Check if timestamp isn't in the past
                            alarmIntent.putExtra(Constants.ALARM, alarm);
                            pendingIntent = PendingIntent.getService(context, Integer.parseInt(alarm.MemoryId + index), alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                            manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                            manager.setExact(AlarmManager.RTC_WAKEUP, Utils.convertDateAndTimeToSeconds(memoryDate) * 1000, pendingIntent);
                            Log.d(TAG, "Setting weekly alarm (b) " + alarm.MemoryId + ":" + index + " on: " + Utils.convertMillisToDateAndTimeString(Utils.convertDateAndTimeToSeconds(memoryDate) * 1000));
                        } else {
                            Log.d(TAG, "Weekly alarm: " + alarm.MemoryId + ":" + index  + " is today but has already expired.");
                        }
                    } else {
                        Log.d(TAG, "Weekly alarm: " + alarm.MemoryId + ":" + index + " will not be set yet.");
                    }
                    ++index;
                }
            } else { //Once or Daily
                Log.d(TAG, "memoryDate: " + alarm.MemoryDates[0]);
                if(!Utils.isTimeStampInThePast(Utils.convertDateAndTimeToSeconds(alarm.MemoryDates[0]))) { //Check if timestamp isn't in the past
                    alarmIntent.putExtra(Constants.ALARM, alarm);
                    pendingIntent = PendingIntent.getService(context, Integer.parseInt(alarm.MemoryId), alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                    manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                    manager.setExact(AlarmManager.RTC_WAKEUP, Utils.convertDateAndTimeToSeconds(alarm.MemoryDates[0]) * 1000, pendingIntent);
                    Log.d(TAG, "Setting alarm " + alarm.MemoryId + ": " + alarm.MemoryDates[0]);
                } else {
                    Log.d(TAG, "Weekly alarm " + alarm.MemoryId + ": is in the past and will not be set.");
                }
            }
        } else {
            Log.d(TAG, "Alarm: " + alarm.MemoryId + " has no set dates.");
        }
    }

    public static void stopAlarm(Context context, Alarm alarm) {
        Log.d(TAG, "Stopping Alarm");
        Intent alarmIntent = new Intent(context, AlarmService.class);
        PendingIntent pendingIntent;
        AlarmManager manager;
        if(alarm.MemoryDates != null) {
            //TODO: must check if memoryDate is before current, if weekly update (what to do with once and daily?)
            if(alarm.MemoryDates.length > 1)  {
                alarm.MemoryFreq = Constants.ALARM_FREQUENCY_WEEKLY;
            }

            if(alarm.MemoryFreq == Constants.ALARM_FREQUENCY_WEEKLY)  {
                int index = 0;
                for(String memoryDate : alarm.MemoryDates) {
                    int daysSinceMemoryDate = Utils.getNumberOfDaysBetweenTwoTimeStamps(Utils.convertDateAndTimeToSeconds(memoryDate),
                            Utils.getCurrentTimeStampInSeconds());
                    if((daysSinceMemoryDate + 1) % Constants.DAYS_IN_A_WEEK == 0) { // + 1 since alarm should be in the future
                        alarmIntent.putExtra(Constants.ALARM, alarm);
//                        Log.d(TAG, "Stopping weekly alarm " + alarm.MemoryId + ":" + index + ": " + memoryDate);
                        pendingIntent = PendingIntent.getService(context, Integer.parseInt(alarm.MemoryId + index), alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                        manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                        manager.cancel(pendingIntent);
                        pendingIntent.cancel();
                        Log.d(TAG, "Stopping weekly alarm (a) " + alarm.MemoryId + ":" + index + " on: " + Utils.convertMillisToDateAndTimeString(
                                (Utils.convertDateAndTimeToSeconds(memoryDate) * 1000) + ((daysSinceMemoryDate + 1) * Constants.MILLIS_IN_A_DAY)));
                    } else if(daysSinceMemoryDate == 0) { // Alarm was set today.
                        if(!Utils.isTimeStampInThePast(Utils.convertDateAndTimeToSeconds(alarm.MemoryDates[0]))) { //Check if timestamp isn't in the past
                            alarmIntent.putExtra(Constants.ALARM, alarm);
                            pendingIntent = PendingIntent.getService(context, Integer.parseInt(alarm.MemoryId + index), alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                            manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                            manager.cancel(pendingIntent);
                            pendingIntent.cancel();
//                            manager.setExact(AlarmManager.RTC_WAKEUP, Utils.convertDateAndTimeToSeconds(memoryDate) * 1000, pendingIntent);
                            Log.d(TAG, "Stopping weekly alarm (b) " + alarm.MemoryId + ":" + index + " on: " + Utils.convertMillisToDateAndTimeString(Utils.convertDateAndTimeToSeconds(memoryDate) * 1000));
                        } else {
                            Log.d(TAG, "Weekly alarm: " + alarm.MemoryId + ":" + index + " was today and expired, so not set.");
                        }
                    } else {
                        Log.d(TAG, "Weekly alarm: " + alarm.MemoryId + ":" + index + " was not yet set.");
                    }
                    ++index;
                }
            } else { //Once or Daily
                if(!Utils.isTimeStampInThePast(Utils.convertDateAndTimeToSeconds(alarm.MemoryDates[0]))) { //Check if timestamp isn't in the past
                    alarmIntent.putExtra(Constants.ALARM, alarm);
                    Log.d(TAG, "Stopping alarm: " + alarm.MemoryId + ":" + alarm.MemoryDates[0]);
                    pendingIntent = PendingIntent.getService(context, Integer.parseInt(alarm.MemoryId), alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                    manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                    manager.cancel(pendingIntent);
                    pendingIntent.cancel();
                } else {
                    Log.d(TAG, "Weekly alarm: " + alarm.MemoryId + " is expired and is not set.");
                }
            }
        } else {
            Log.d(TAG, "Alarm: " + alarm.MemoryId + " was not set.");
        }
    }

    public static void cancelAllAlarms(Context context, ArrayList<Alarm> alarmsArray) {
        Log.d(TAG, "Cancelling All Alarms");
        for(Alarm alarm : alarmsArray) {
            Log.d(TAG, "Cancelling: " + alarm.getMemoryId());
            stopAlarm(context, alarm);
        }
    }

    public static void startAllAlarms(Context context, ArrayList<Alarm> alarmsArray) {
        Log.d(TAG, "Starting All Alarms");
        for(Alarm alarm : alarmsArray) {
            setAlarm(context, alarm);
        }
    }

    public void resetAlarm(Context context, ArrayList<Alarm> alarmsArray) {
        cancelAllAlarms(context, alarmsArray);
        startAllAlarms(context, alarmsArray);
    }

    public static String printMemoryDates(Alarm alarm) {
        String memoryDatesString = "";
        for(String memoryDate : alarm.MemoryDates) {
            memoryDatesString += memoryDate + ", ";
        }
        return memoryDatesString;
    }
}
