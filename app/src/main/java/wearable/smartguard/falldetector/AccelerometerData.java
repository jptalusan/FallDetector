package wearable.smartguard.falldetector;

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

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getZ() {
        return z;
    }

    public void setZ(float z) {
        this.z = z;
    }

    public double getNormalizedAcceleration() {
        return Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));
    }

//    @Override
//    public String toString() {
//        return "AccelerometerData{" +
//                "timestamp=" + timestamp +
//                ", x=" + x +
//                ", y=" + y +
//                ", z=" + z +
//                '}';
//    }

    @Override
    public String toString() {
        return timestamp + "," + x + "," + y + "," + z;
    }
}
