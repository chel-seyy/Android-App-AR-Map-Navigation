package com.arjo129.artest;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.arjo129.artest.arrendering.ARScene;
import com.arjo129.artest.arrendering.DirectionInstruction;
import com.arjo129.artest.arrendering.DisplayRotationHelper;
import com.arjo129.artest.device.CompassListener;
import com.arjo129.artest.device.WifiLocation;
import com.google.ar.sceneform.ux.ArFragment;

import java.util.ArrayList;

import static java.lang.Math.abs;
import static java.lang.Math.copySign;

public class ARActivity extends AppCompatActivity {
    WifiLocation wifiLocation;
    final String TAG = "ARActivity";
    int x,y,floor;
    Runnable serverReqThread;
    private int ScanInterval = 60000; // 5 seconds by default, can be changed later
    private Handler serverHandler;
    private CompassListener compassListener;
    private DisplayRotationHelper dhelper;
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