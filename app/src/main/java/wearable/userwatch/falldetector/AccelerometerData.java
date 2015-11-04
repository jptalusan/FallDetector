package wearable.userwatch.falldetector;

public class AccelerometerData {
    private long timestamp;
    private float x;
    private float y;
    private float z;

    public AccelerometerData(long timestamp, float x, float y, float z) {
        this.timestamp = timestamp;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public double getNormalizedAcceleration() {
        return Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));
    }

    @Override
    public String toString() {
        return timestamp + "," + x + "," + y + "," + z;
    }
}
