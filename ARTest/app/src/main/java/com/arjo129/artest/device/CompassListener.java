    package com.arjo129.artest.device;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import com.arjo129.artest.arrendering.ARScene;

public class CompassListener implements SensorEventListener {
    private final static String TAG = "CompassListener";
    private SensorManager mSensorManager;
    public  float[] orientation = new float[3];
    private float currentHeading = 0;
    private float rotation = 0f;
    private long timestamp;
    private boolean collected_ts = false;
    public int accuracy = -1;
    public CompassListener(Context ctx){
        mSensorManager = (SensorManager)ctx.getSystemService(Context.SENSOR_SERVICE);
        startListening();
    }
    @SuppressWarnings("WeakerAccess")
    public void  startListening(){
        mSensorManager.registerListener(this,mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), SensorManager.SENSOR_DELAY_FASTEST);
    }
    public void stopListenening(){
        mSensorManager.unregisterListener(this);
    }
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sensorEvent.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            float[] rotMatrix = new float[9];
            float[] outMatrix = new float[9];
            //ARCore uses X-Z plane as the floor vs. The device ROTATION_VECTOR x-axis is east and Y-axis is north
            SensorManager.getRotationMatrixFromVector(rotMatrix, sensorEvent.values);
            boolean succ = SensorManager.remapCoordinateSystem(rotMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z,outMatrix);
            SensorManager.getOrientation(outMatrix, orientation);
            float prevHeading = currentHeading;
            currentHeading = ((float)Math.toDegrees(orientation[0])+360)%360;
            if(collected_ts) rotation = (currentHeading-prevHeading)/(sensorEvent.timestamp-timestamp);
            ARScene.fromRPY(orientation[0],orientation[1],orientation[2]);
            accuracy = sensorEvent.accuracy;
            Log.d(TAG, "" + succ + " " + currentHeading + " " +accuracy+" "+rotation);
            timestamp = sensorEvent.timestamp;
            collected_ts = true;
        }

    }
    public float getBearing(){
        // negative because positive rotation about Y rotates X away from Z
        return currentHeading;
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }
}
