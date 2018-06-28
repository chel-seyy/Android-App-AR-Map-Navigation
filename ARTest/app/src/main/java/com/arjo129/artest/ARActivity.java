package com.arjo129.artest;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;

import com.arjo129.artest.arrendering.ARScene;
import com.arjo129.artest.arrendering.DirectionInstruction;
import com.arjo129.artest.device.CompassListener;
import com.arjo129.artest.device.WifiLocation;
import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.asin;
import static java.lang.Math.atan2;
import static java.lang.Math.copySign;

public class ARActivity extends AppCompatActivity {
    WifiLocation wifiLocation;
    final String TAG = "ARActivity";
    int x,y,floor;
    Runnable serverReqThread;
    private int ScanInterval = 60000; // 5 seconds by default, can be changed later
    private Handler serverHandler;
    private CompassListener compassListener;
    private  DisplayRotationHelper dhelper;
    private ARScene navscene;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar);
        //Use a custom compass object to reduce
        compassListener = new CompassListener(this);
        dhelper = new DisplayRotationHelper(this);
        //Get Route
        Intent routeIntent = getIntent();
        ArrayList<DirectionInstruction> directionInstructions = (ArrayList<DirectionInstruction>) routeIntent.getSerializableExtra("Directions");
        //Instantiate the ARCore stuff
        ArFragment arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ARView);
        navscene = new ARScene(this,compassListener,arFragment,dhelper, directionInstructions);
    }


    @Override
    protected void onDestroy(){
        super.onDestroy();
        //serverHandler.removeCallbacks(serverReqThread);
        navscene.destroy();
    }
}