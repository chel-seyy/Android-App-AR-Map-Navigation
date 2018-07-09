package com.arjo129.artest.arrendering;
/**
 * This classsimplements a simple Fusion between the ROTATION_VECTOR and the visual
 */

import android.support.annotation.VisibleForTesting;
import android.util.Log;

import com.arjo129.artest.device.CompassListener;

import java.util.ArrayList;

import static java.lang.StrictMath.abs;

public class VisualCompass {
    private final static int SIZE = 10;
    private final static String TAG = "VisualCompass";
    CompassListener compassListener;
    float visual_heading = 0;
    int numberOfMeasurements = 0;
    Float measurements[];
    public VisualCompass(CompassListener c){
        compassListener = c;
        measurements = new Float[SIZE];
    }
    public float getHeading(float dvisual){
        Log.d(TAG,"began computation");
        for(int i = 0; i < numberOfMeasurements; i++){
            measurements[i] -= dvisual;
            measurements[i] = (measurements[i]+360)%360;
        }
        if(numberOfMeasurements < SIZE) {
            measurements[numberOfMeasurements] = compassListener.getBearing();
            if(compassListener.accuracy == 3) numberOfMeasurements++;
            if(numberOfMeasurements != 0)
                visual_heading = averageAngle();
            else
                visual_heading = measurements[numberOfMeasurements];
        }
        else{
            float average = averageAngle();
            float max_dev = 0;
            int index = 0;
            for(int i = 0; i < numberOfMeasurements; i++){
                float dev = Math.min(abs(measurements[i] - average),360-abs(measurements[i] - average));
                if(dev > max_dev){
                    max_dev = dev;
                    index = i;
                }
            }
            float curr_heading = compassListener.getBearing();
            measurements[index] = curr_heading;
            visual_heading = averageAngle();
        }
        String debug_float = "measurements";
        for(int i = 0; i < numberOfMeasurements; i++){
            debug_float += " ,";
            debug_float += measurements[i];
        }
       // Log.d(TAG,debug_float);
        Log.d(TAG,"heading "+visual_heading+","+compassListener.getBearing()+","+dvisual);
        return visual_heading;
    }

    private float averageAngle(){
        float totalx = 0 ,totaly =0;
        for(int i = 0; i < numberOfMeasurements; i++){
            totalx += Math.cos(Math.toRadians(measurements[i]));
            totaly += Math.sin(Math.toRadians(measurements[i]));
        }
        float total = ((float)Math.toDegrees(Math.atan2(totaly,totalx))+360)%360;
        if(numberOfMeasurements >0) return total;
        else return 0;
    }
}
