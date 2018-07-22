package com.arjo129.artest;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
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

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set title
        alertDialogBuilder.setTitle("Calibrating the AR");

        // set dialog message
        alertDialogBuilder
                .setMessage("Readings may not be accurate if phone is not calibrated in the Figure-8 pattern.")
                .setCancelable(false)
//                .setNegativeButton("DON'T SHOW THIS AGAIN", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialogInterface, int i) {
//                        dialogInterface.cancel();
//                    }
//                })
                .setPositiveButton("Calibrate", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();

    }


    @Override
    protected void onDestroy(){
        super.onDestroy();
        //serverHandler.removeCallbacks(serverReqThread);
        navscene.destroy();
    }
}