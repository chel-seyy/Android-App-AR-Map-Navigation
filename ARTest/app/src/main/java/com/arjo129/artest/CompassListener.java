    package com.arjo129.artest;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class CompassListener implements SensorEventListener {
    private SensorManager mSensorManager;
    private float[] mGravity = new float[3];
    private float[] mGeomagnetic = new float[3];
    public  float[] orientation = new float[3];
    public float azimuth = 0f;
    private float correctAzimuth = 0f;
    CompassListener(Context ctx){
        mSensorManager = (SensorManager)ctx.getSystemService(Context.SENSOR_SERVICE);
        startListening();
    }
    public void  startListening(){
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_GAME);
    }
    public void stopListenening(){
        mSensorManager.unregisterListener(this);
    }
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        final float alpha = 0.97f;
        if(sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            mGravity[0] = alpha*mGravity[0]+(1-alpha)*sensorEvent.values[0];
            mGravity[1] = alpha*mGravity[1]+(1-alpha)*sensorEvent.values[1];
            mGravity[2] = alpha*mGravity[2]+(1-alpha)*sensorEvent.values[2];
        }
        if(sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
            mGeomagnetic[0] = alpha*mGeomagnetic[0]+(1-alpha)*sensorEvent.values[0];
            mGeomagnetic[1] = alpha*mGeomagnetic[1]+(1-alpha)*sensorEvent.values[1];
            mGeomagnetic[2] = alpha*mGeomagnetic[2]+(1-alpha)*sensorEvent.values[2];
        }
        float R[] = new float[9];
        float I[] = new float[9];
        boolean success = SensorManager.getRotationMatrix(R,I, mGravity,mGeomagnetic);
        if(success){
            SensorManager.getOrientation(R,orientation);
            azimuth = (float)Math.toDegrees(orientation[0]);
            azimuth = (azimuth+360)%360;
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }
}
