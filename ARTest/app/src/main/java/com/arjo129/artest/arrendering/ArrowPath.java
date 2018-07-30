package com.arjo129.artest.arrendering;

import android.content.Context;
import android.util.Log;
import android.widget.ImageButton;

import com.arjo129.artest.R;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ViewRenderable;

import java.util.ArrayList;

public class ArrowPath {
    private float distance;
    private float heading;
    private float next_path;
    private int lastArrow;
    private ARScene scene;
    private static ModelRenderable arrow;
    private static ViewRenderable destinationMarker, upMarker, downMarker;
    private static int numberOfObjectsLoaded = 0;
    private ArrayList<Integer> arrows;
    private static final String TAG = "ArrowPath";
    private boolean toBeRendered = false;
    public enum EndMarkerType{
        END_MARKER_TYPE_STAIRS_UP,
        END_MARKER_TYPE_STAIRS_DOWN,
        END_MARKER_TYPE_DESTINATION,
        END_MARKER_TYPE_NEXT
    }
    public EndMarkerType endMarker = EndMarkerType.END_MARKER_TYPE_NEXT;
    @SuppressWarnings("WeakerAccess")
    public ArrowPath(Context ctx, float dist, float angle, float next_angle, ARScene arScene){
        scene = arScene;
        distance = dist;
        heading = angle;
        next_path = next_angle;
        arrows = new ArrayList<>();
        numberOfObjectsLoaded = 0;
        if(numberOfObjectsLoaded < 3) {
            ModelRenderable.builder()
                    .setSource(ctx, R.raw.model)
                    .build()
                    .thenAccept(renderable -> arrow = renderable);
            ViewRenderable.builder()
                    .setView(ctx, R.layout.ar_imageview)
                    .build()
                    .thenAccept(renderable -> {
                        destinationMarker = renderable;
                        ((ImageButton) destinationMarker.getView()).setImageResource(R.drawable.destination);
                        numberOfObjectsLoaded++;
                        if(toBeRendered) construct();
                    });
            ViewRenderable.builder()
                    .setView(ctx, R.layout.ar_imageview)
                    .build()
                    .thenAccept(renderable -> {
                        upMarker = renderable;
                        ((ImageButton) upMarker.getView()).setImageResource(R.drawable.stairs_up);
                        numberOfObjectsLoaded++;
                        if(toBeRendered) construct();
                    });
            ViewRenderable.builder()
                    .setView(ctx, R.layout.ar_imageview)
                    .build()
                    .thenAccept(renderable -> {
                        downMarker = renderable;
                        ((ImageButton) downMarker.getView()).setImageResource(R.drawable.stairs_down);
                        numberOfObjectsLoaded++;
                        if(toBeRendered) construct();
                    });
        }
    }

    private void render(){
        Log.d(TAG,"heading: "+heading+"endmarker: "+endMarker);
        for(int i =1 ; i < distance-1; i+=5){
            //Log.d(TAG, "drawing arrow "+i);
           int id = scene.placeItem(arrow,i,heading,heading+90,0,true);
           arrows.add(id);
        }
        switch(endMarker) {
            case END_MARKER_TYPE_DESTINATION:
                Log.d(TAG,"Last Arrow!!");
                lastArrow = scene.placeItem(destinationMarker, distance, heading, heading, 0, true);
                break;
            case END_MARKER_TYPE_STAIRS_UP:
                lastArrow = scene.placeItem(upMarker,distance,heading,heading,0,true);
                break;
            case END_MARKER_TYPE_STAIRS_DOWN:
                lastArrow = scene.placeItem(downMarker,distance,heading,heading,0,true);
                break;
            case END_MARKER_TYPE_NEXT:
                lastArrow = scene.placeItem(arrow, distance, heading, next_path + 90, 0, true);
                Log.d(TAG, "Arrow "+arrow);
                break;
        }
    }

    public void construct(){
        if(numberOfObjectsLoaded < 3){
            toBeRendered = true;
        }
        else{
            render();
        }
    }
    public void update() throws StairException {
       ArrayList<Integer> tbr = new ArrayList<>();
       for(int id: arrows){
            if (scene.isInFrontOf(id)){
                //Log.d(TAG,"in front of arrow "+id);
                scene.removeItem(id);
                tbr.add(id);
            }
            else {
               //Log.d(TAG,"behind arrow "+id);
            }
        }
        for(int id: tbr){
            arrows.remove(Integer.valueOf(id));
        }
        if(arrows.size() ==0 && (endMarker == EndMarkerType.END_MARKER_TYPE_STAIRS_UP || endMarker == EndMarkerType.END_MARKER_TYPE_STAIRS_DOWN))
            throw new StairException();
        //construct();
    }
    public void destroy(){
        for(int id: arrows){
            scene.removeItem(id);
        }
        scene.removeItem(lastArrow);
    }
}
