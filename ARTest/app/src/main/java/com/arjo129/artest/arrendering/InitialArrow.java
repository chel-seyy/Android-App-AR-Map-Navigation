package com.arjo129.artest.arrendering;

import android.content.Context;
import android.util.Log;

import com.arjo129.artest.R;
import com.arjo129.artest.device.CompassListener;
import com.google.ar.core.Pose;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;

public class InitialArrow {
    CompassListener compassListener;
    float bearing;
    ModelRenderable arrow;
    ARScene scene;
    String TAG = "InitialArrow";
    int model_id = -1;
    public InitialArrow(Context ctx, ARScene scn, float heading, CompassListener cmp){
        bearing = heading;
        compassListener = cmp;
        scene =scn;
        ModelRenderable.builder()
                .setSource(ctx, R.raw.model)
                .build()
                .thenAccept(renderable -> arrow = renderable);
    }
    public void update(Pose cameraPose){
        float current_cam = compassListener.getBearing();

        if(Math.abs(current_cam-bearing) > 4) {
            if (model_id >= 0) {
                Log.d(TAG, "compass " + current_cam + ", " + bearing);
                //Log.d(TAG,"updating arrow follower");
                float quat[] = cameraPose.getRotationQuaternion();
                Quaternion qt = new Quaternion(quat[0], quat[1], quat[2], quat[3]);
                Vector3 translation = Quaternion.rotateVector(qt, Vector3.forward());
                float campos[] = cameraPose.getTranslation();
                Vector3 camerapos = new Vector3(campos[0], campos[1], campos[2]);
                scene.setPosition(model_id, Vector3.add(camerapos, translation));
            }
        }
        else{
            if(model_id >=0 ){
                scene.removeItem(model_id);
                model_id = -1;
            }
        }
    }
    public void construct(){
        float current_cam = compassListener.getBearing();
        if(Math.abs(current_cam-bearing) > 20) {
            Log.d(TAG,"constructing arrow follower");
            model_id = scene.placeItem(arrow, 1, current_cam,  bearing+90, 0, true);
        }
    }
    public boolean safeToRemove(){
        return model_id >= 0;
    }
}
