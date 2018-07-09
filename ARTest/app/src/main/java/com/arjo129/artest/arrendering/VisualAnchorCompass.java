package com.arjo129.artest.arrendering;

import android.util.Log;

import com.arjo129.artest.device.CompassListener;
import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;

import java.util.ArrayList;

public class VisualAnchorCompass {
    final static int SIZE = 8;
    final static String TAG = "VisualAnchorCompass";
    Anchor camAnchors[];
    float measurements[], headings[];
    CompassListener compassListener;
    int numAnchors = 0, anchorIdx = 0;
    VisualAnchorCompass(CompassListener c){
        camAnchors = new Anchor[SIZE];
        measurements = new float[SIZE];
        headings = new float[SIZE];
        compassListener = c;
    }
    public float getHeading(Session session, Frame frame){
        //Acquire camera pose
        Pose cameraPose = frame.getCamera().getPose();
        float deltaZ = getAngleFromPose(cameraPose);
        //Camera 2d
        Quaternion qt1 = Quaternion.axisAngle(Vector3.up(),deltaZ);
        Pose rotationCam = Pose.makeRotation(qt1.x,qt1.y,qt1.z,qt1.w);
        camAnchors[anchorIdx] = session.createAnchor(rotationCam);
        headings[anchorIdx] = compassListener.getBearing();
        anchorIdx++;
        anchorIdx%=SIZE;
        numAnchors = Math.min(numAnchors+1,8);
        for(int i = 0; i < numAnchors; i++){
            Pose deltaCam = camAnchors[i].getPose().inverse().compose(rotationCam);
            float currentCameraDelta = getAngleFromPose(deltaCam);
            Log.d(TAG,"camshift: "+currentCameraDelta+", heading: "+headings[i]);
        }
        return 0.0f;
    }

    private float getAngleFromPose(Pose pose){
        //Project z-axis onto x-z plane.
        Vector3 vec = new Vector3(pose.getZAxis()[0],pose.getZAxis()[1],pose.getZAxis()[2]);
        float zProjection = Vector3.dot(Vector3.back(), vec.normalized());
        float xProjection = Vector3.dot(Vector3.right(), vec.normalized());
        float deltaZ = (float) Math.toDegrees(Math.atan2(xProjection,zProjection));
        return (deltaZ+360)%360;
    }
    private float averageAngle(){
        double totalx = 0 ,totaly =0;
        for(int i = 0; i < numAnchors; i++){
            totalx += Math.cos(Math.toRadians(measurements[i]));
            totaly += Math.sin(Math.toRadians(measurements[i]));
        }
        float total = ((float)Math.toDegrees(Math.atan2(totaly,totalx))+360)%360;
        if(numAnchors >0) return total;
        else return 0;
    }
}
