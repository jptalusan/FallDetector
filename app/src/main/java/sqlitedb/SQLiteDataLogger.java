package sqlitedb;

import android.content.Context;
import android.os.AsyncTask;

import java.util.ArrayList;

import sqlitedb.SQLiteDBInterface;
import wearable.smartguard.falldetector.AccelerometerData;

/**
 * Created by jtalusan on 10/19/2015.
 */
public class SQLiteDataLogger extends AsyncTask<ArrayList<AccelerometerData>, Void, Void> {
    public AsyncResponse delegate = null;
    private SQLiteDBInterface datasource;

    public SQLiteDataLogger(Context context) {
        datasource = new SQLiteDBInterface(context);
    }

    @Override
    protected Void doInBackground(ArrayList<AccelerometerData>... data) {
        datasource.open();
        for (AccelerometerData a : data[0]) {
            datasource.insertAccelerometerDataToDB(a);
        }
        datasource.close();
        return null;
    }

    @Override
    protected void onPostExecute(Void v) {
        delegate.processIsFinished(true);

    }

    public interface AsyncResponse {
        void processIsFinished(boolean output);
    }
}
