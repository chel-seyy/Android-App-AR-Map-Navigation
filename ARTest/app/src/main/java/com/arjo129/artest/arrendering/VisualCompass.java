package com.arjo129.artest.arrendering;
/**
 * This classsimplements a simple Fusion between the ROTATION_VECTOR and the visual
 */

import android.util.Log;

import com.arjo129.artest.device.CompassListener;

public class VisualCompass {
    CompassListener compassListener;
    float current_heading = 0;
    float visual_heading = 0;
    private final static String TAG = "VisualCompass";
    boolean first = true;
    VisualCompass(CompassListener c){
        compassListener = c;
        current_heading = c.getBearing();
        visual_heading = c.getBearing();
    }
    float getHeading(float dvisual){
        if(first){
            visual_heading = compassListener.getBearing();
            first = false;
        }
        else visual_heading -= dvisual;
        visual_heading =(visual_heading+360)%360;
        Log.d(TAG,"heading "+visual_heading+","+compassListener.getBearing()+","+dvisual);
        return visual_heading;
    }
}
