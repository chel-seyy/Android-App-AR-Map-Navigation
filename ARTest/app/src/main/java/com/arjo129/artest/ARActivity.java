package com.arjo129.artest;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;

import com.arjo129.artest.arrendering.ARScene;
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
    ModelRenderable testViewRenderable;
    WifiLocation wifiLocation;
    final String TAG = "ARActivity";
    int x,y,floor;
    boolean planeAnchored = true;
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
        //Build a wifiLocation request...
        wifiLocation = new WifiLocation(this, (HashMap<String, Integer> map)->{
            //Ths lambda function is triggered whenever a wifi scan is completed
            //Hashmap<String, int> consists of the WiFi BSSID and the value is the strength
            //Format WIFI list to pretty JSON
            JSONObject wifi_list = new JSONObject();
            //Iterate through the hashmap and convert into JSON format
            //JSON should look like "WIFI" :{BSSID1: STRENGTH1, BSSID2: STRENGTH2, ...}
            //   i.e. "WIFI" : {"AA:EE:BB:11:CC": -150,"11:22:33:44:55":-20}
            for (Map.Entry<String, Integer> item : map.entrySet()) {
                String key = item.getKey();
                int value = item.getValue();
                try {
                    wifi_list.put(key, value);
                }
                catch(Exception e){
                    Log.d(TAG,e.toString());
                    return null;
                }
                Log.d(TAG, "Got wife bssid: "+ key +" , RSSI:"+ value + "session_secret");
            }
            JSONObject query = new JSONObject();
            try {
                query.put("WIFI", wifi_list);
                AsyncHttpClient client = new AsyncHttpClient();
                StringEntity ent = new StringEntity(query.toString());
                Log.d(TAG,"Sending request");
                Log.d(TAG,query.toString());
                //Query location at server_url/location
                client.post(this,getString(R.string.server_url)+"location",ent,"application/json",new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject responseBody) {
                        Log.d(TAG,"got response: "+responseBody.toString());
                    }
                });
            } catch (Exception e){
                Log.d(TAG,e.toString());
            }
            return null;
        });

        //Scan for wifi locations and determine location in a background thread
        serverHandler = new Handler();
        serverReqThread = new Runnable() {
            @Override
            public void run() {
                try {
                    wifiLocation.scanWifiNetworks();
                } finally {
                    serverHandler.postDelayed(serverReqThread,ScanInterval);
                }
            }
        };
        serverReqThread.run();
        //Instantiate the ARCore stuff
        ArFragment arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ARView);
        Scene scene = arFragment.getArSceneView().getScene();
        ARScene arScene = new ARScene(this,compassListener,arFragment,dhelper);
        // Build the View renderable froim an android resource file
        ModelRenderable.builder()
                .setSource(this, R.raw.model)
                .build()
                .thenAccept(renderable -> testViewRenderable = renderable);
        //Onclick render the arrow add the spot clicked
    }


    @Override
    protected void onDestroy(){
        super.onDestroy();
        serverHandler.removeCallbacks(serverReqThread);
    }
}