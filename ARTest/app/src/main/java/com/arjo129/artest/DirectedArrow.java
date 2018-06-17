package com.arjo129.artest;

import android.content.Context;

import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.ar.core.TrackingState;

public class DirectedArrow {
    private static final float DECAY_RATE = 0.9f;
    private final CompassListener compassListener;
    private final DisplayRotationHelper displayRotationHelper;
    private float SQRT_HALF = (float)Math.sqrt(0.5);
    private Pose deviceToWorld;
    private float[] accumulated;

    DirectedArrow(CompassListener c, DisplayRotationHelper dp) {
        compassListener = c;
        displayRotationHelper = dp;
        accumulated = new float[3];
    }
    private float rotateXToEastAngle() {
        float eastX = accumulated[0];
        float eastZ = accumulated[2];
        // negative because positive rotation about Y rotates X away from Z
        return -(float)Math.atan2(eastZ, eastX);
    }
    /*
    * Extract pose of object due north
     */
    public Pose rotateXToEastPose(){
        float angle = rotateXToEastAngle();
        float sinHalf = (float)Math.sin(angle/2);
        float cosHalf = (float)Math.cos(angle/2);
        return Pose.makeRotation(0, sinHalf, 0, cosHalf);
    }

    public void onUpdate(Frame frame) {
        deviceToWorld = getDevicePose(frame).extractRotation();
        float []rotated = new float[3];
        deviceToWorld.rotateVector(compassListener.mGeomagnetic, 0, rotated,0);
        for(int i=0; i<3; ++i) {
            accumulated[i] = accumulated[i] * DECAY_RATE + rotated[i];
        }
    }
    public Pose getDevicePose(Frame frame) {
        // Cheat: Pose.makeInterpolated for rotation multiplication
        return frame.getCamera().getDisplayOrientedPose().compose(
                Pose.makeInterpolated(
                        Pose.IDENTITY,
                        Pose.makeRotation(0, 0, SQRT_HALF, SQRT_HALF),
                        displayRotationHelper.getRotation()));
    }


}
