package com.arjo129.artest.arrendering;

import android.content.Context;
import android.util.Log;
import android.view.Display;

import com.arjo129.artest.R;
import com.google.ar.sceneform.rendering.ModelRenderable;

public class ArrowPath {
    float distance;
    float heading;
    float next_path;
    ARScene scene;
    ModelRenderable arrow;
    final String TAG = "ArrowPath";
    ArrowPath(Context ctx, float dist, float angle, float next_angle, ARScene arScene){
        scene = arScene;
        distance = dist;
        heading = angle;
        next_path =next_angle;
        ModelRenderable.builder()
                .setSource(ctx, R.raw.model)
                .build()
                .thenAccept(renderable -> arrow = renderable);
    }

    void construct(){
        for(int i =1 ; i < distance; i+=1){
            Log.d(TAG, "drawing arrow "+i);
            scene.placeItem(arrow,i,heading,heading+180,0,true);
        }
        scene.placeItem(arrow,distance,heading, next_path+180,0,true);
    }
    void update(){

    }
}
