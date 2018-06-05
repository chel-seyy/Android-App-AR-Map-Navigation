package com.arjo129.artest;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.BaseArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

public class ARActivity extends AppCompatActivity {
    ViewRenderable testViewRenderable;
    boolean planeAnchored = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar);
        ArFragment arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ARView);

        // When you build a Renderable, Sceneform loads its resources in the background while returning
        // a CompletableFuture. Call thenAccept(), handle(), or check isDone() before calling get().

        Scene scene = arFragment.getArSceneView().getScene();

        ViewRenderable.builder()
                .setView(this, R.layout.tool_tips)
                .build()
                .thenAccept(renderable -> testViewRenderable = renderable);

        scene.setOnUpdateListener(new Scene.OnUpdateListener() {
            @Override
            public void onUpdate(FrameTime frameTime) {
                Session session = arFragment.getArSceneView().getSession();
                Collection<Plane> planes = session.getAllTrackables(Plane.class);
                for(Plane p: planes){
                    //Log.d("ARActivity","got plane" );
                    Pose pose = p.getCenterPose();
                    if(planeAnchored) {
                        Log.d("ARActivity","drawn" );
                        Anchor anchor = p.createAnchor(pose);
                        AnchorNode anchorNode = new AnchorNode(anchor);
                        anchorNode.setParent(scene);
                        TransformableNode transformableNode = new TransformableNode(arFragment.getTransformationSystem());
                        transformableNode.setParent(anchorNode);
                        transformableNode.setRenderable(testViewRenderable);
                        planeAnchored = false;
                    }
                }
            }
        });
    }
}