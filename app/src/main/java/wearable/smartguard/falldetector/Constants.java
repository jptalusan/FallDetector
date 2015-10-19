package wearable.smartguard.falldetector;

/**
 * Created by jtalusan on 10/19/2015.
 */
public class Constants {
    //COLUMN_NAMES
    public static final String COLUMN_ID = "ID";
    public static final String COLUMN_TIMESSTAMP = "TIMESTAMP";
    public static final String COLUMN_XAXIS = "XAXIS";
    public static final String COLUMN_YAXIS = "YAXIS";
    public static final String COLUMN_ZAXIS = "ZAXIS";

    //KEYS
    public static final String TIMESTAMP = "TIMESTAMP";
    public static final String XAXIS = "XAXIS";
    public static final String YAXIS = "YAXIS";
    public static final String ZAXIS = "ZAXIS";

    //IDs
    public static final int XAXISISORTHO = 0;
    public static final int YAXISISORTHO = 1;
    public static final int ZAXISISORTHO = 2;

    //SETTINGS
    public static final double FALL_THRESHOLD = 18.0; //FOR BOTH LINEAR and ACCELEROMETER
    public static final double MOVE_THRESHOLD = 0.95; //0.95 for linear, 0.60~ for accelerometer (more sensitive)
    public static final double WALK_THRESHOLD = 5.0;
    public static final int UPPER_LIMIT_PEAK_COUNT = 5;
    public static final int LOWER_LIMIT_PEAK_COUNT = 0;
    public static final long FALL_DETECT_WINDOW_SECS = 5;
    public static final long VERIFY_FALL_DETECT_WINDOW_SECS = 9;
    public static final long CHARACTERIZE_ACTIVITY_WINDOW_SECS = 9;
}
