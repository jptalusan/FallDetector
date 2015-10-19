package wearable.smartguard.falldetector;

import android.hardware.SensorEvent;
import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by jtalusan on 10/19/2015.
 */

//TODO: Change this so that it will just be passed the arraylist and then work on that instead.
class CharacterizeActivityTask extends AsyncTask<SensorEvent, Void, Void> {
    private static final String DEBUG_TAG = "CharacterizeTask";
    public AsyncResponse delegate = null;
    ArrayList<AccelerometerData> aList = new ArrayList<>();
    private int peakCounter = 0;
    private float x, y, z = 0.0f;

    @Override
    protected Void doInBackground(SensorEvent... events) {
        SensorEvent event = events[0];
        x = event.values[0];
        y = event.values[1];
        z = event.values[2];
        if (!Utils.isAccelerometerArrayExceedingTimeLimit(aList, Constants.CHARACTERIZE_ACTIVITY_WINDOW_SECS)) {
            aList.add(new AccelerometerData(Utils.getCurrentTimeStampInMillis(), x, y, z));
            Log.d(DEBUG_TAG, aList.size() + "");
        } else {
            peakCounter = Utils.getNumberOfPeaksThatExceedThreshold(aList, Constants.WALK_THRESHOLD);
            if (peakCounter > 8) {
                Log.d(DEBUG_TAG, "Walking/Moving");
            } else {
                switch (Utils.identifyWhichAxisIsOrthogonalToGravity(aList)) {
                    case Constants.XAXISISORTHO:
                    case Constants.ZAXISISORTHO:
                        Log.d(DEBUG_TAG, "Sleeping");
                        break;
                    case Constants.YAXISISORTHO:
                        Log.d(DEBUG_TAG, "Walking");
                        break;
                    default:
                        break;
                }
            }
            aList.clear();
        }
//        delegate.processFinish(false);
        return null;
    }

    public interface AsyncResponse {
        void processFinish(boolean output);
    }
}
