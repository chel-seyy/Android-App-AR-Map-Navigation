package com.arjo129.artest.arrendering;

import android.content.Context;
import android.util.Log;
import android.util.SparseArray;

import com.arjo129.artest.DisplayRotationHelper;
import com.arjo129.artest.R;
import com.arjo129.artest.device.CompassListener;
import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.exceptions.NotTrackingException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.schemas.lull.Quat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Handler;

import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.asin;
import static java.lang.Math.atan2;
import static java.lang.Math.copySign;

public class ARScene {
    private static final String TAG = "ARScene";
    private Runnable refreshThread;
    private android.os.Handler refreshHandler;
    /*class GeoNode {
            public Renderable rend;
            public double lat,lng,h,bear;
            public Anchor anch;
            public Node node;
            public boolean render;
            GeoNode(Node n, Renderable rend, double lat, double lng, double h, double bearing){
                this.rend = rend;
                this.lat = lat;
                this.node = n;
                this.lng = lng;
                this.h = h;
                this.bear = bearing;
            }
        }*/
    private CompassListener compassListener;
    private DisplayRotationHelper dhelper;
    private ArFragment frag;
    private SparseArray<Node> items = new SparseArray<>();
    private boolean update = false, ready =false;
    private int item_counter  = 0;
    private Context context;
    private ModelRenderable testViewRenderable;
    private ArrowPath arrowPath1,arrowPath2,arrowPath3,arrowPath4;
    private Anchor prevCam = null;
    private float prevHeading = 0;
    private ArrayList<DirectionInstruction> instructions;
    private int curr_direction;
    /**
     * Constructs a new ARScene with a compass class
     * @param compass - compass listener which helps orient the device
     * @param fg - ArFragment which tells
     * @param displayRotationHelper - display rotation
     */
    public ARScene(Context ctx, CompassListener compass, ArFragment fg, DisplayRotationHelper displayRotationHelper, ArrayList<DirectionInstruction> inst){
       compassListener = compass;
       frag = fg;
       //Set the ARFragement to listen to me
       frag.getArSceneView().getScene().setOnUpdateListener(frameTime -> {
           frag.onUpdate(frameTime);
           onUpdateFrame(frameTime);
       });
       context  = ctx;
       dhelper = displayRotationHelper;
       instructions = inst;
       refreshHandler = new android.os.Handler();
       refreshThread = new Runnable() {
            @Override
            public void run() {
                try {
                    update = true;
                } finally {
                    refreshHandler.postDelayed(refreshThread,5000);
                }
            }
        };
       refreshThread.run();
       curr_direction =0;
       if(inst.size() > 0){
           inst.get(0);
           DirectionInstruction dir = instructions.get(curr_direction);
           float next_turn = 0;
           if(curr_direction+1 < instructions.size()){
               next_turn = instructions.get(curr_direction).direction;
           }
           //arrowPath1.destroy();
           arrowPath1 = new ArrowPath(context, dir.distance, dir.direction, next_turn,this);
           Log.d(TAG, "drawing....");
           //arrowPath1.construct();
           curr_direction++;
       }


    }
    /**
     * This method performs the actual update of the scene
     * @param frameTime
     */
    public void onUpdateFrame(FrameTime frameTime){
        Date now = new Date();
        //locationEngine.hasBetterLocation(now);
        Session sess = frag.getArSceneView().getSession();
        Frame frame = frag.getArSceneView().getArFrame();
        //TODO: Calculate Drift, correct drift update location
        if(update && ready){
            update = false;
            try {
                Pose currPose = frame.getCamera().getDisplayOrientedPose().compose(
                        Pose.makeInterpolated(
                                Pose.IDENTITY,
                                Pose.makeRotation(0, 0, (float) Math.sqrt(0.5f), (float) Math.sqrt(0.5f)),
                                dhelper.getRotation()));
                Anchor tmp = sess.createAnchor(currPose);
                if (prevCam != null) {
                    Pose prevPose = prevCam.getPose();
                    float[] devquat = currPose.getRotationQuaternion();
                    Quaternion deviceFrame = new Quaternion();
                    deviceFrame.set(devquat[0], devquat[1], devquat[2], devquat[3]);
                    double[] rpy = quat2rpy(deviceFrame);
                    float[] prevRpy = prevPose.getRotationQuaternion();
                    deviceFrame = new Quaternion();
                    deviceFrame.set(prevRpy[0], prevRpy[1], prevRpy[2], prevRpy[3]);
                    prevCam.detach();
                    double[] prevOrientation = quat2rpy(deviceFrame);
                    if(abs(compassListener.getBearing() - prevHeading) > 45 && abs(Math.toDegrees(rpy[1] - prevOrientation[1])) > 45 ) {
                        //User has turned
                        Log.d(TAG,"User turned!");
                        onTurn(abs(Math.toDegrees(rpy[1] - prevOrientation[1])));
                    }
                    Log.d(TAG, "Mag: " + (compassListener.getBearing() - prevHeading));
                    Log.d(TAG, "Vis: " + Math.toDegrees(rpy[1] - prevOrientation[1] ));
                    Log.d(TAG, "angle: " + compassListener.getBearing() + " world heading:" + (float) rpy[1] * 180 / 3.1415f);
                    //float arNorth = ((float)Math.toDegrees(rpy[1])+360)%360+360-compassListener.getBearing();
                   // Log.d(TAG,"ARNorth: "+arNorth%360+ ", arangle:"+(float)(Math.toDegrees(rpy[1])+360)%360+ ", heading: "+(360-compassListener.getBearing()));
                }
                prevCam = tmp;
                prevHeading = compassListener.getBearing();
            } catch(NotTrackingException t){

            }
        }
        else if(ready){
            arrowPath1.update();
        }
        //Let us know when ARCore has some idea of the world...
        if(!ready){
            Collection<Plane> trackables = sess.getAllTrackables(Plane.class);
            if(!trackables.isEmpty()){
                ready = true;
                onReady();
            }
        }
    }

    public void onReady(){
        arrowPath1.construct();
    }

    public void onTurn(double angle){
        double current_heading = compassListener.getBearing();
        Log.d(TAG, "drawing....");
        if(abs(current_heading - instructions.get(curr_direction).direction) < 45){
            if(curr_direction < instructions.size()) {
                DirectionInstruction dir = instructions.get(curr_direction);
                float next_turn = 0;
                if(curr_direction+1 < instructions.size()){
                    next_turn = instructions.get(curr_direction).direction;
                }
                arrowPath1.destroy();
                arrowPath1 = new ArrowPath(context, dir.distance, dir.direction, next_turn,this);
                Log.d(TAG, "drawing....");
                arrowPath1.construct();
                curr_direction++;
            }
        }
    }
    /**
     * Places an item in our ARScene using distance and
     * @param r - the renderable
     * @param dist - distance from camera
     * @param rotation - rotation of item in relation to the real world
     * @param height - height in y
     * @param rotate - set to true if you want to set a rotation
     * @return an item ID, can be used for removing the item
     */
    public int placeItem(Renderable r, double dist, float angle, float rotation, float height, boolean rotate){
        Session sess = frag.getArSceneView().getSession();
        Frame frame = frag.getArSceneView().getArFrame();
        //Get some anchors to anchor our item to
        Collection<Plane> trackables = sess.getAllTrackables(Plane.class);
        Anchor anchor = null;
        for(Plane t: trackables){
            anchor = t.createAnchor(t.getCenterPose());
        }
        if(anchor == null) return -1; //No trackable found yet
        Log.d(TAG,"Established Anchor");
        //Get the phone's pose in ARCore
        Pose deviceOrientedPose = frame.getCamera().getDisplayOrientedPose().compose(
                Pose.makeInterpolated(
                        Pose.IDENTITY,
                        Pose.makeRotation(0, 0, (float)Math.sqrt(0.5f), (float)Math.sqrt(0.5f)),
                        dhelper.getRotation()));
        //Get the phone's pose in relation to the real world
        float heading = compassListener.getBearing();
        float[] devquat = deviceOrientedPose.getRotationQuaternion();
        Quaternion deviceFrame = new Quaternion();
        deviceFrame.set(devquat[0],devquat[1],devquat[2],devquat[3]);
        double[] rpy = quat2rpy(deviceFrame);
        //Rotate around y axis...
        //Log.d(TAG,"angle: "+heading+" world heading:"+(float)rpy[1]*180/3.1415f);
        float arNorth = ((float)Math.toDegrees(rpy[1])+360)%360+360-compassListener.getBearing();
        //Log.d(TAG,"ARNorth: "+arNorth+ ", arangle:"+(float)Math.toDegrees(rpy[1])+ ", heading: "+heading);
        //Rotate to camera pose, then rotate to north, then rotate by x degrees
        float rotAngle = ((360-rotation)+heading+((float)Math.toDegrees(rpy[1])+360)%360)%360;
        Quaternion qt = Quaternion.axisAngle(Vector3.up(),rotAngle);
        //Build the node
        Node node = new Node();
        node.setParent(frag.getArSceneView().getScene());
        node.setRenderable(r);
        //Set rotation
        if(rotate)
        node.setWorldRotation(qt);
        //Set angle
        float angrad = ((360-angle)+heading+((float)Math.toDegrees(rpy[1])+360)%360)%360;
        Log.d(TAG,"drawing..."+rotAngle+" intended angle:"+angle+" angrad:"+angrad);
        Vector3 pos = new Vector3 (-(float)dist*(float)Math.sin(Math.toRadians(angrad)),0,-(float)dist*(float)Math.cos(Math.toRadians(angrad)));
        float[] vec = deviceOrientedPose.getTranslation();
        Vector3 camPos = new Vector3(vec[0],vec[1],vec[2]);
        pos = Vector3.add(camPos,pos);
        node.setWorldPosition(pos);
        item_counter++;
        items.put(item_counter, node);
        return item_counter;
    }

    public Vector3 getPosition(int id){
        Node pose= items.get(id, null);
        if(pose==null){
            return null;
        }
        return pose.getWorldPosition();
    }

    public Quaternion getRotation(int id){
        Node pose= items.get(id, null);
        if(pose==null){
            return null;
        }
        return pose.getWorldRotation();
    }

    public void setPosition(int id, Vector3 vc){
        Node pose= items.get(id, null);
        if(pose==null){
            return;
        }
        pose.setWorldPosition(vc);
    }
    public boolean isInFrontOf(int id){
        Vector3 forward = Vector3.left();
        Quaternion qt = getRotation(id);
        forward = Quaternion.rotateVector(qt,forward);
        Frame frame = frag.getArSceneView().getArFrame();
        Pose deviceOrientedPose = frame.getCamera().getDisplayOrientedPose().compose(
                Pose.makeInterpolated(
                        Pose.IDENTITY,
                        Pose.makeRotation(0, 0, (float)Math.sqrt(0.5f), (float)Math.sqrt(0.5f)),
                        dhelper.getRotation()));
        float[] campos = deviceOrientedPose.getTranslation();
        Vector3 cameraPos = new Vector3(campos[0],campos[1],campos[2]);
        cameraPos = Vector3.subtract(cameraPos,getPosition(id));
        //Log.d(TAG, "angle between "+id+" and camera: "+ Vector3.angleBetweenVectors(cameraPos,forward));
        return Vector3.dot(forward,cameraPos) > 0;
    }

    public void removeItem(int id){
        Scene scene = frag.getArSceneView().getScene();
        Node geoNode = items.get(id);
        scene.removeChild(geoNode);
        items.remove(id);
    }
    public void destroy(){
        compassListener.stopListenening();
        refreshHandler.removeCallbacks(refreshThread);
    }


    public static Quaternion fromRPY(double heading, double attitude, double bank) {
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
    public static double[] quat2rpy(Quaternion quaternion){
        return quat2rpy(quaternion.w,quaternion.x, quaternion.y, quaternion.z);
    }
    public static double[] quat2rpy(float qw,float qx, float qy, float qz){
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
