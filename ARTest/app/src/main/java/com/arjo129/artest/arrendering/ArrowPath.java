package com.arjo129.artest.arrendering;

import android.content.Context;
import android.util.Log;
import com.arjo129.artest.R;
import com.google.ar.sceneform.rendering.ModelRenderable;
import java.util.ArrayList;

public class ArrowPath {
    private float distance;
    private float heading;
    private float next_path;
    private int lastArrow;
    private ARScene scene;
    private ModelRenderable arrow;
    private ArrayList<Integer> arrows;
    private static final String TAG = "ArrowPath";

    @SuppressWarnings("WeakerAccess")
    public ArrowPath(Context ctx, float dist, float angle, float next_angle, ARScene arScene){
        scene = arScene;
        distance = dist;
        heading = angle;
        next_path = next_angle;
        ModelRenderable.builder()
                .setSource(ctx, R.raw.model)
                .build()
                .thenAccept(renderable -> arrow = renderable);
        arrows = new ArrayList<>();
    }

    public void construct(){
        for(int i =1 ; i < distance; i+=5){
            Log.d(TAG, "drawing arrow "+i);
            int id = scene.placeItem(arrow,i,heading,heading+90,0,true);
            arrows.add(id);
        }
        lastArrow = scene.placeItem(arrow,distance,heading, next_path+90,0,true);
    }
    public void update(){
       ArrayList<Integer> tbr = new ArrayList<>();
       for(int id: arrows){
            if (scene.isInFrontOf(id)){
                Log.d(TAG,"in front of arrow "+id);
                scene.removeItem(id);
                tbr.add(id);
            }
            else {
                Log.d(TAG,"behind arrow "+id);
            }
        }
        for(int id: tbr){
            arrows.remove(Integer.valueOf(id));
        }
        //construct();
    }
    public void destroy(){
        for(int id: arrows){
            scene.removeItem(id);
        }
        scene.removeItem(lastArrow);
    }
}
