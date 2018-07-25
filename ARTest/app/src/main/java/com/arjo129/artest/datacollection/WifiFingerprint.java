package com.arjo129.artest.datacollection;

import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class WifiFingerprint implements Serializable {
    private final static String TAG = "WifiFingerprint";
    private HashMap<String, Integer> wifiscans;
    private double latitude, longitude;
    private int floor;
    private Date creation_time;
    public WifiFingerprint(double lat, double lng,int floor, HashMap<String,Integer> scan){
        this.latitude = lat;
        this.longitude = lng;
        this.wifiscans = scan;
        this.floor = floor;
        this.creation_time = new Date();
    }
    public JSONObject toJSON(){
        JSONObject wifi_list = new JSONObject();
        for (Map.Entry<String, Integer> item : wifiscans.entrySet()) {
            String key = item.getKey();
            int value = item.getValue();
            try {
                wifi_list.put(key, value);
            }
            catch(Exception e){
                Log.d(TAG,e.toString());
                return null;
            }
        }
        JSONObject json = new JSONObject();
        int x_coord = (int)Math.round(((latitude-1)*110547)/3);
        int y_coord = (int)Math.round(111320*Math.cos(Math.toRadians(latitude))*longitude/3);
        try {
            json.put("x", x_coord);
            json.put("y", y_coord);
            json.put("floor", floor);
            json.put("location", "com1f"+floor+"|"+x_coord+"|"+y_coord);
            json.put("WIFI",wifi_list);
        }
        catch ( JSONException e){
            e.printStackTrace();
        }
        return json;
    }
    public String getDate(){
        return creation_time.toString();
    }
}
