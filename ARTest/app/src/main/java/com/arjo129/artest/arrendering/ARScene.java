package com.arjo129.artest.arrendering;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.util.SparseArray;

import com.arjo129.artest.device.CompassListener;
import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.NotTrackingException;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.ux.ArFragment;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.asin;
import static java.lang.Math.atan2;
import static java.lang.Math.copySign;

public class ARScene {
    private static final String TAG = "ARScene";
    private Runnable refreshThread;
    private android.os.Handler refreshHandler;
    private CompassListener compassListener;
    private DisplayRotationHelper dhelper;
    private ArFragment frag;
    private SparseArray<Node> items = new SparseArray<>();
    private boolean update = false, ready =false;
    private int item_counter  = 0;
    private Context context;
    private ArrowPath arrowPath1;
    private Anchor prevCam = null, prevStartPoint;
    private float prevHeading = 0;
    private ArrayList<DirectionInstruction> instructions;
    private  VisualAnchorCompass visualAnchorCompass;
    //The navigation stack
    private int curr_direction;
    private InitialArrow initialArrow;
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
       refreshThread = () -> {
           try {
               update = true;
           } finally {
               refreshHandler.postDelayed(refreshThread,3000);
           }
       };
       visualAnchorCompass = new VisualAnchorCompass(compassListener);
       refreshThread.run();
       curr_direction =0;
       if(inst.size() > 0){
           DirectionInstruction dir = instructions.get(curr_direction);
           if(dir.isConnector){
               arrowPath1 = new ArrowPath(context, dir.distance, dir.direction, 0,this);
               if(dir.goingUp)
                   arrowPath1.endMarker = ArrowPath.EndMarkerType.END_MARKER_TYPE_STAIRS_UP;
               else
                   arrowPath1.endMarker = ArrowPath.EndMarkerType.END_MARKER_TYPE_STAIRS_DOWN;
           }
           else if(curr_direction+1 < instructions.size()){
               float next_turn = instructions.get(curr_direction+1).direction;
               arrowPath1 = new ArrowPath(context, dir.distance, dir.direction, next_turn,this);
           }
           else {
               arrowPath1 = new ArrowPath(context, dir.distance, dir.direction, 0,this);
               arrowPath1.endMarker = ArrowPath.EndMarkerType.END_MARKER_TYPE_DESTINATION;
           }

           //Log.d(TAG, "drawing...."+dir.direction+","+next_turn);
           //arrowPath1.construct();
           curr_direction++;
           initialArrow = new InitialArrow(context,this, dir.direction,visualAnchorCompass);
       }
       Log.d(TAG,"--[Recieved instructions]----------");
       for(DirectionInstruction dir: instructions){
           Log.d(TAG,"Got instruction walk "+dir.distance+"m"+" due"+dir.direction+", is connector: "+ dir.connector_type);
       }
    }
    /**
     * This method performs the actual update of the scene
     * @param frameTime - passed by ARCore
     */
    @SuppressWarnings("unused")
    public void onUpdateFrame(FrameTime frameTime){
        Date now = new Date();
        //locationEngine.hasBetterLocation(now);
        Session sess = frag.getArSceneView().getSession();
        Frame frame = frag.getArSceneView().getArFrame();
        //TODO: Calculate Drift, correct drift update location
        if(update && ready){
            update = false;
            try {
                Pose currPose = frame.getCamera().getDisplayOrientedPose();
                Anchor tmp = sess.createAnchor(currPose);
                if (prevCam != null) {
                    //Compute change in quaternion
                    Pose deltaPose = prevCam.getPose().inverse().compose(currPose);
                    Vector3 cameraZ = new Vector3(deltaPose.getZAxis()[0],deltaPose.getZAxis()[1],deltaPose.getZAxis()[2]);
                    //Project z-axis onto x-z plane.
                    float zProjection = Vector3.dot(Vector3.back(), cameraZ.normalized());
                    float xProjection = Vector3.dot(Vector3.right(), cameraZ.normalized());
                    float deltaZ = (float) Math.toDegrees(Math.atan2(xProjection,zProjection));
                    if(abs(visualAnchorCompass.getHeading(sess,frame,false) - prevHeading) > 25 ) {
                        //User has turned
                        //Log.d(TAG,"User turned! VIS:"+abs(Math.toDegrees(rpy[1] - prevOrientation[1])));
                        onTurn(sess,frame);
                    }
                    prevCam.detach();
                    visualAnchorCompass.getHeading(sess,frame,true);
                    //Log.d(TAG, "Mag: " + (compassListener.getBearing() - prevHeading));
                    //Log.d(TAG, "Vis: " + Math.toDegrees(rpy[1] - prevOrientation[1] ));
                    //Log.d(TAG, "angle: " + compassListener.getBearing() + " world heading:" + (float) rpy[1] * 180 / 3.1415f);
                    //float arNorth = ((float)Math.toDegrees(rpy[1])+360)%360+360-compassListener.getBearing();
                   // Log.d(TAG,"ARNorth: "+arNorth%360+ ", arangle:"+(float)(Math.toDegrees(rpy[1])+360)%360+ ", heading: "+(360-compassListener.getBearing()));
                }
                prevCam = tmp;
                prevHeading = visualAnchorCompass.getHeading(sess,frame,false);
            } catch(NotTrackingException t){
                t.printStackTrace();
            }
        }
        else if(ready){
            Pose currPose = frame.getCamera().getDisplayOrientedPose().compose(
                    Pose.makeInterpolated(
                            Pose.IDENTITY,
                            Pose.makeRotation(0, 0, (float) Math.sqrt(0.5f), (float) Math.sqrt(0.5f)),
                            dhelper.getRotation()));
            initialArrow.update(currPose,sess,frame);
            try {
                arrowPath1.update();
            } catch (StairException st){
                Intent stairActivity= new Intent(context, StaircaseActivity.class);
                ArrayList<DirectionInstruction> directions = new ArrayList<>();
                for(int i = curr_direction+1; i < instructions.size(); i++){
                    directions.add(instructions.get(i));
                }
                stairActivity.putExtra("instructions",(Serializable)directions);
                context.startActivity(stairActivity);
            }
            Pose deviceOrientedPose = frame.getCamera().getPose();
            //Get the phone's pose in relation to the real world
            float heading = visualAnchorCompass.getHeading(sess,frame,false);
            float cameraFrame  = VisualAnchorCompass.getAngleFromPose(deviceOrientedPose);
            //Log.d(TAG,""+heading+", "+cameraFrame+", "+(heading+cameraFrame+360)%360);
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

    private void onReady(){
        initialArrow.construct(frag.getArSceneView().getSession(),frag.getArSceneView().getArFrame());
        Pose pose = frag.getArSceneView().getArFrame().getCamera().getPose();
        frag.getArSceneView().getSession().createAnchor(pose);
        prevStartPoint = frag.getArSceneView().getSession().createAnchor(pose);
        arrowPath1.construct();
    }

    /**
     * This function is called when the user turns... This forces the AR to update.
     *
     */
    private void onTurn(Session sess, Frame frame){
        double current_heading = visualAnchorCompass.getHeading(sess,frame,false);
        Quaternion currentCompass = Quaternion.axisAngle(Vector3.up(),(float) current_heading);

        if(instructions.size() > curr_direction) {
            Quaternion desiredAngle = Quaternion.axisAngle(Vector3.up(),instructions.get(curr_direction).direction);
            Vector3 currentHeading = Quaternion.rotateVector(currentCompass, Vector3.forward());
            Vector3 desiredHeading = Quaternion.rotateVector(desiredAngle, Vector3.forward());
            float angleBetweenVectors = Vector3.angleBetweenVectors(currentHeading, desiredHeading);
            //Toast.makeText(context,"Turn detected: "+angleBetweenVectors, Toast.LENGTH_SHORT);
            Log.d(TAG, "onTurn Called:"+angleBetweenVectors);
            Log.d(TAG, "Compass: "+ current_heading);
            Log.d(TAG,"Target heading:" + instructions.get(curr_direction).direction);
            if (abs(angleBetweenVectors) < 55) {
                if (curr_direction < instructions.size()) {
                    DirectionInstruction dir = instructions.get(curr_direction);
                    arrowPath1.destroy();
                    if(dir.isConnector){
                        arrowPath1 = new ArrowPath(context, dir.distance, dir.direction, dir.direction,this);
                        if(dir.goingUp)
                            arrowPath1.endMarker = ArrowPath.EndMarkerType.END_MARKER_TYPE_STAIRS_UP;
                        else
                            arrowPath1.endMarker = ArrowPath.EndMarkerType.END_MARKER_TYPE_STAIRS_DOWN;
                    }
                    else if(curr_direction+1 < instructions.size()){
                        float next_turn = instructions.get(curr_direction+1).direction;
                        arrowPath1 = new ArrowPath(context, dir.distance, dir.direction, next_turn,this);
                    }
                    else {
                        arrowPath1 = new ArrowPath(context, dir.distance, dir.direction, dir.direction,this);
                        arrowPath1.endMarker = ArrowPath.EndMarkerType.END_MARKER_TYPE_DESTINATION;
                    }
                    arrowPath1.construct();
                    curr_direction++;
                }
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
        //Get the phone's pose in ARCore
        Pose deviceOrientedPose = frame.getCamera().getPose();
        //Get the phone's pose in relation to the real world
        float heading = visualAnchorCompass.getHeading(sess,frame,false);
        float cameraFrame  = VisualAnchorCompass.getAngleFromPose(deviceOrientedPose);
        //Rotate around y axis...
        float offset_by = (heading+cameraFrame)%360;
        float from_camera = (rotation - offset_by+360)%360;
        float rotAngle = (-from_camera+360)%360;
        Quaternion qt = Quaternion.axisAngle(Vector3.up(),rotAngle);
        //Build the node
        Node node = new Node();
        node.setParent(frag.getArSceneView().getScene());
        node.setRenderable(r);
        //Set rotation
        if(rotate)
        node.setWorldRotation(qt);
        //Set angle
        float from_camera1 = (angle - offset_by+360)%360;
        float angrad = ((-from_camera1+360)%360);
        //Log.d(TAG,"drawing..."+rotation+" intended angle:"+angle+" angrad:"+heading+",campose"+cameraFrame);
        Vector3 pos = new Vector3 (-(float)dist*(float)Math.sin(Math.toRadians(angrad)),0,-(float)dist*(float)Math.cos(Math.toRadians(angrad)));
        float[] vec = deviceOrientedPose.getTranslation();
        Vector3 camPos = new Vector3(vec[0],vec[1],vec[2]);
        Log.d(TAG, "xyz: "+pos.x+","+pos.z);
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
        double cosy = 1.0 - 2.0 * (qy * qy + qz * qz);
        double yaw = atan2(siny, cosy);
        double[] rpy = new double[3];
        rpy[0] = roll;
        rpy[1] = pitch;
        rpy[2] = yaw;
        return rpy;
        //Log.d(TAG,"angle: "+roll*180/PI+","+pitch*180/PI+","+yaw*180/PI);
    }
}
