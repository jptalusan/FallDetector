package sqlitedb;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import wearable.userwatch.Constants;

/**
 * Created by jtalusan on 10/19/2015.
 */
public class SQLiteHelper extends SQLiteOpenHelper {
    public static final String TABLE_NAME = "DetectedFallPattern";
    public static final String COLUMN_ID = Constants.COLUMN_ID;
    public static final String COLUMN_TIMESSTAMP = Constants.COLUMN_TIMESSTAMP;
    public static final String COLUMN_XAXIS = Constants.COLUMN_XAXIS;
    public static final String COLUMN_YAXIS = Constants.COLUMN_YAXIS;
    public static final String COLUMN_ZAXIS = Constants.COLUMN_ZAXIS;
    private static final String DATABASE_NAME = "fallDetector.db";
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_CREATE = "CREATE TABLE IF NOT EXISTS " +
            TABLE_NAME + " (" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            COLUMN_TIMESSTAMP + " INT NOT NULL," +
            COLUMN_XAXIS + " REAL NOT NULL," +
            COLUMN_YAXIS + " REAL NOT NULL," +
            COLUMN_ZAXIS + " REAL NOT NULL);";
    private static SQLiteHelper sInstance;
    private static String DEBUG_TAG = "sqlite";

    private SQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static synchronized SQLiteHelper getInstance(Context context) {
        if (sInstance == null)
            sInstance = new SQLiteHelper(context.getApplicationContext());
        return sInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(DEBUG_TAG, "Creating database");
        db.execSQL(DATABASE_CREATE);
        Log.d(DEBUG_TAG, "Database created");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(DEBUG_TAG,
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }
}
