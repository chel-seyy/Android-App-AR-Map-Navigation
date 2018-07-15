package com.arjo129.artest.arrendering;

import android.hardware.SensorManager;
import android.util.Log;

import com.arjo129.artest.device.CompassListener;
import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;


public class VisualAnchorCompass {
    private final static int SIZE = 20;
    private final static String TAG = "VisualAnchorCompass";
    private Anchor camAnchors[];
    private float measurements[], headings[];
    private CompassListener compassListener;
    private int numAnchors = 0, anchorIdx = 0;

    @SuppressWarnings("WeakerAccess")
    VisualAnchorCompass(CompassListener c){
        camAnchors = new Anchor[SIZE];
        measurements = new float[SIZE];
        headings = new float[SIZE];
        compassListener = c;
    }
    public float getHeading(Session session, Frame frame, boolean update){
        //Acquire camera pose
        Pose cameraPose = frame.getCamera().getPose();
        float deltaZ = getAngleFromPose(cameraPose);
        //Camera 2d
        Quaternion qt1 = Quaternion.axisAngle(Vector3.up(),deltaZ);
        Pose rotationCam = Pose.makeRotation(qt1.x,qt1.y,qt1.z,qt1.w);
        if(update && compassListener.accuracy == SensorManager.SENSOR_STATUS_ACCURACY_HIGH) {
            if(camAnchors[anchorIdx] != null)
                camAnchors[anchorIdx].detach();
            camAnchors[anchorIdx] = session.createAnchor(rotationCam);
            headings[anchorIdx] = compassListener.getBearing();
            anchorIdx++;
            anchorIdx %= SIZE;
            numAnchors = Math.min(numAnchors + 1, SIZE);
        }
        for(int i = 0; i < numAnchors; i++){
            Pose deltaCam = camAnchors[i].getPose().inverse().compose(rotationCam);
            float currentCameraDelta = getAngleFromPose(deltaCam);
            measurements[i]= ((360-currentCameraDelta)+headings[i]+360)%360;
        }
        return averageAngle(update);
    }

    public static float getAngleFromPose(Pose pose){
        //Project z-axis onto x-z plane.
        Vector3 vec = new Vector3(pose.getZAxis()[0],pose.getZAxis()[1],pose.getZAxis()[2]);
        float zProjection = Vector3.dot(Vector3.back(), vec.normalized());
        float xProjection = Vector3.dot(Vector3.right(), vec.normalized());
        float deltaZ = (float) Math.toDegrees(Math.atan2(xProjection,zProjection));
        return (deltaZ+360)%360;
    }
    private float averageAngle(boolean update){
        double totalx = 0 ,totaly =0;
        for(int i = 0; i < numAnchors; i++){
            totalx += Math.cos(Math.toRadians(measurements[i]));
            totaly += Math.sin(Math.toRadians(measurements[i]));
        }
        if(!update || numAnchors == 0){
            totalx += Math.cos(Math.toRadians(compassListener.getBearing()));
            totaly += Math.sin(Math.toRadians(compassListener.getBearing()));
        }
        float total = ((float)Math.toDegrees(Math.atan2(totaly,totalx))+360)%360;
        Log.d(TAG, "Angle "+total+" calculated from "+numAnchors+" anchors "+compassListener.getBearing());
        return total;
    }
}
