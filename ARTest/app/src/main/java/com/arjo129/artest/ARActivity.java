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
        ARScene arScene = new ARScene(this,compassListener,arFragment,null,dhelper);
        // Build the View renderable froim an android resource file
        ModelRenderable.builder()
                .setSource(this, R.raw.model)
                .build()
                .thenAccept(renderable -> testViewRenderable = renderable);
        //Onclick render the arrow add the spot clicked
        arFragment.setOnTapArPlaneListener(
                (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
                    if (testViewRenderable == null) {
                        return;
                    }

                    if (plane.getType() != Plane.Type.HORIZONTAL_UPWARD_FACING) {
                        return;
                    }

                    // Create the Anchor.
                    Anchor anchor = hitResult.createAnchor();
                    Node anchorNode = new AnchorNode(anchor);
                    anchorNode.setParent(arFragment.getArSceneView().getScene());
                    //Create Y-Axis rotation opposite of heading 0 Degrees = west for some reason
                    Vector3 vc = new Vector3(0,1,0);
                    float  bearing = compassListener.getBearing(); // Get the phone's bearing
                    Log.d(TAG,"Bearing: "+bearing);
                    //Get rotation from camera
                    Frame frame = arFragment.getArSceneView().getArFrame();
                    Pose deviceOrientedPose = frame.getCamera().getDisplayOrientedPose().compose(
                            Pose.makeInterpolated(
                                    Pose.IDENTITY,
                                    Pose.makeRotation(0, 0, (float)Math.sqrt(0.5f), (float)Math.sqrt(0.5f)),
                                    dhelper.getRotation()));
                    float[] devquat = deviceOrientedPose.getRotationQuaternion();
                    Quaternion deviceFrame = new Quaternion();
                    deviceFrame.set(devquat[0],devquat[1],devquat[2],devquat[3]);
                    double[] rpy = quat2rpy(deviceFrame);
                    Log.d(TAG,"DeviceFrame: "+rpy[0]*180/3.1415+"."+rpy[1]*180/3.1415+","+rpy[2]*180/3.1415+","+bearing);
                    //Rotate around y axis.,,
                    Quaternion qt = Quaternion.axisAngle(vc,bearing+(float)rpy[1]*180/3.1415f+180);
                    //Vector3 zaxis = new Vector3(0,0,1);
                    // Create the transformable andy and add it to the anchor.
                    Node andy = new Node();
                    andy.setParent(anchorNode);
                    andy.setWorldRotation(qt);
                    float angrad = bearing/180*3.1415f + (float)rpy[1];
                    Vector3 pos = new Vector3 (0.9f*(float)Math.cos(angrad),0,-0.9f*(float)Math.sin(angrad));
                    float[] vec = deviceOrientedPose.getTranslation();
                    Vector3 camPos = new Vector3(vec[0],vec[1],vec[2]);
                    pos = Vector3.add(camPos,pos);
                    andy.setWorldPosition(pos);
                    andy.setRenderable(testViewRenderable);
                    // andy.select();
                });

    }


    @Override
    protected void onDestroy(){
        super.onDestroy();
        serverHandler.removeCallbacks(serverReqThread);
    }

    public static double bearing(double lat1, double lon1, double lat2, double lon2){
        double longitude1 = lon1;
        double longitude2 = lon2;
        double latitude1 = Math.toRadians(lat1);
        double latitude2 = Math.toRadians(lat2);
        double longDiff = Math.toRadians(longitude2-longitude1);
        double y = Math.sin(longDiff)*Math.cos(latitude2);
        double x = Math.cos(latitude1)*Math.sin(latitude2)-Math.sin(latitude1)*Math.cos(latitude2)*Math.cos(longDiff);

        return (Math.toDegrees(Math.atan2(y, x))+360)%360;
    }

    Quaternion fromRPY(double heading, double attitude, double bank) {
        // Assuming the angles are in radians.
        double c1 = Math.cos(heading/2);
        double s1 = Math.sin(heading/2);
        double c2 = Math.cos(attitude/2);
        double s2 = Math.sin(attitude/2);
        double c3 = Math.cos(bank/2);
        double s3 = Math.sin(bank/2);
        double c1c2 = c1*c2;
        double s1s2 = s1*s2;
        double w =c1c2*c3 - s1s2*s3;
        double x =c1c2*s3 + s1s2*c3;
        double y =s1*c2*c3 + c1*s2*s3;
        double z =c1*s2*c3 - s1*c2*s3;
        double angle = 2 * Math.acos(w);
        double norm = x*x+y*y+z*z;
        if (norm < 0.000001) { // when all euler angles are zero angle =0 so
            // we can set axis to anything to avoid divide by zero
            x=1;
            y=z=0;
        } else {
            norm = Math.sqrt(norm);
            x /= norm;
            y /= norm;
            z /= norm;
        }
        Vector3 vec = new Vector3((float)x,(float)y, (float)z);
        Quaternion qt = Quaternion.axisAngle(vec,(float)(angle*57.29));
        return qt;
    }
    double[] quat2rpy(Quaternion quaternion){
        return quat2rpy(quaternion.w,quaternion.x, quaternion.y, quaternion.z);
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