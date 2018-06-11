package com.arjo129.artest;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
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
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.asin;
import static java.lang.Math.atan2;
import static java.lang.Math.copySign;

public class ARActivity extends AppCompatActivity {
    ViewRenderable testViewRenderable;
    WifiLocation wifiLocation;
    final String TAG = "ARActivity";
    int x,y,floor;
    boolean planeAnchored = true;
    Runnable serverReqThread;
    private int ScanInterval = 60000; // 5 seconds by default, can be changed later
    private Handler serverHandler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar);
        //Build a wifiLocation request
        wifiLocation = new WifiLocation(this, (HashMap<String, Integer> map)->{
            //Format WIFI list to pretty JSON
            JSONObject wifi_list = new JSONObject();
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
                //Query location
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

        //Scan for wifi locations and determine location
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
                arFragment.onUpdate(frameTime);
                Session session = arFragment.getArSceneView().getSession();
                Frame frame = arFragment.getArSceneView().getArFrame();
                Collection<Plane> planes = session.getAllTrackables(Plane.class);
                for(Plane p: planes){
                    //Log.d("ARActivity","got plane" );
                    Pose pose = p.getCenterPose();
                    if(planeAnchored) {
                        Log.d("ARActivity","drawn" );
                        Anchor anchor = p.createAnchor(pose);
                        float qw = pose.qw();
                        float qx = pose.qx();
                        float qy = pose.qy();
                        float qz = pose.qz();
                        double[] rpy = quat2rpy(qw,qx,qy,qz);
                        Log.d(TAG, "Plane rpy:"+rpy[0]+" , "+rpy[1]+" , "+rpy[2]);
                        AnchorNode anchorNode = new AnchorNode(anchor);
                        anchorNode.setParent(scene);
                        TransformableNode transformableNode = new TransformableNode(arFragment.getTransformationSystem());
                        transformableNode.setParent(anchorNode);
                        transformableNode.setRenderable(testViewRenderable);
                        planeAnchored = false;
                    }
                }
                Camera camera = frame.getCamera();
                Pose cameraPose = camera.getPose();
                //convert quaternion to rpy
                float qw = cameraPose.qw();
                float qx = cameraPose.qx();
                float qy = cameraPose.qy();
                float qz = cameraPose.qz();
                double[] rpy = quat2rpy(qw,qx,qy,qz);
                //pitch correspons to
            }
        });
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        serverHandler.removeCallbacks(serverReqThread);
    }

    double[] quat2rpy(float qw,float qx, float qy, float qz){
        double sinr = 2.0 * (qw * qx + qy * qz);
        double cosr = 1.0 - 2.0 * (qx * qx + qy * qy);
        double roll = atan2(sinr, cosr);
        double sinp = 2.0 * (qw * qy - qz * qx);
        double pitch;
        if (abs(sinp) >= 1)
            pitch = copySign(PI / 2, sinp); // use 90 degrees if out of range
        else
            pitch = asin(sinp);
        double siny = 2.0 * (qw * qz + qx * qy);
        double cosy = 1.0 - 2.0 * (qy * qy + qz * qz);;
        double yaw = atan2(siny, cosy);
        double[] rpy = new double[3];
        rpy[0] = roll;
        rpy[1] = pitch;
        rpy[2] = yaw;
        return rpy;
        //Log.d(TAG,"angle: "+roll*180/PI+","+pitch*180/PI+","+yaw*180/PI);
    }
}