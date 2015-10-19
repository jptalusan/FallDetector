package sqlitedb;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import wearable.smartguard.falldetector.AccelerometerData;
import wearable.smartguard.falldetector.Constants;

/**
 * Created by jtalusan on 10/19/2015.
 */
public class SQLiteDBInterface {
    private static String DEBUG_TAG = "SQLiteDBInterface";

    // Database fields
    private SQLiteDatabase database;
    private SQLiteHelper dbHelper;
    private String[] allColumns = {
            SQLiteHelper.COLUMN_ID,
            SQLiteHelper.COLUMN_TIMESSTAMP,
            SQLiteHelper.COLUMN_XAXIS,
            SQLiteHelper.COLUMN_YAXIS,
            SQLiteHelper.COLUMN_ZAXIS
    };

    public SQLiteDBInterface(Context context) {
        dbHelper = SQLiteHelper.getInstance(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public boolean isOpen() {
        return database.isOpen();
    }

    public void insertAccelerometerDataToDB(AccelerometerData a) {
        Log.d(DEBUG_TAG, "Accelerometer result is unique in SQLiteDB");

        ContentValues v = new ContentValues();
        v.put(Constants.TIMESTAMP, a.getTimestamp());
        v.put(Constants.XAXIS, a.getX());
        v.put(Constants.YAXIS, a.getY());
        v.put(Constants.ZAXIS, a.getZ());

        long insertId = database.insert(SQLiteHelper.TABLE_NAME, null, v);

        String[] whereArgs = new String[]{
                Long.toString(insertId)
        };
        Cursor cursor = database.query(SQLiteHelper.TABLE_NAME,
                allColumns, SQLiteHelper.COLUMN_ID + " = ?", whereArgs,
                null, null, null);

        cursor.moveToFirst();
        cursor.close();
    }

    public void deleteAllData() {
        database.delete(SQLiteHelper.TABLE_NAME, null, null);
        Log.d(DEBUG_TAG, "deleteAllResults()");
    }

    private AccelerometerData cursorToResult(Cursor cursor) {
        return new AccelerometerData(cursor.getLong(1),
                cursor.getFloat(2),
                cursor.getFloat(3),
                cursor.getFloat(4));
    }
}
