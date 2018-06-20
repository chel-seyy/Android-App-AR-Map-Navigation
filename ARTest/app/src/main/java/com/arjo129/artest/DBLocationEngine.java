package com.arjo129.artest;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.util.Log;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.mapbox.android.core.location.LocationEngine;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;


public class DBLocationEngine extends LocationEngine {
    public final static String TAG = "DBLocationEngine";
    public static final int TWO_MINUTES = 1000*60*2;
    private static Double lat = 1.2953;
    private static Double lng = 103.7735;
    private static float accuracy = 55;
    private WifiLocation wifiLocation;
    private Context context;
    public Location currentBestLocation;

    DBLocationEngine(Context context){
        this.context = context;
        Log.d(TAG, "constructed engine");
    }


    @Override
    public void activate() {
        Log.d(TAG, "Activated");
    }

    @Override
    public void deactivate() {
        Log.d(TAG, "Deactivated");
    }

    @Override
    public boolean isConnected() {
        Log.d(TAG, "is Connected");
        if (!lat.equals(null) && !lng.equals(null) && wifiLocation.isConnected()){
            return true;
        }
        return false;
    }

    @SuppressLint("MissingPermission")
    @Override
    public Location getLastLocation() {
        Log.d(TAG, "get Last Location");
        if(currentBestLocation == null){
            requestLocationUpdates();
        }
        return currentBestLocation;
    }

    public void setLocation(Double lat, Double lng, float accuracy){
        Log.d(TAG, "Setting location now: "+lat+" " +lng+" "+accuracy);
        currentBestLocation = new Location(LocationManager.GPS_PROVIDER);
        currentBestLocation.setLatitude(lat);
        currentBestLocation.setLongitude(lng);
        currentBestLocation.setAccuracy(accuracy);
        currentBestLocation.setTime(System.currentTimeMillis());
        Log.d(TAG, "Set: "+currentBestLocation.toString());
    }

    @Override
    public void requestLocationUpdates() throws SecurityException{
        Log.d(TAG, "Requesting Location Updates");
        try{
            // Works here:
//            Log.d(TAG, "Finished: "+lat+" " +lng+" "+accuracy);
            setLocation(lat, lng, accuracy);

            //Build a wifiLocation request
            wifiLocation = new WifiLocation(context, (HashMap<String, Integer> map)->{
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
                    Log.d(TAG, "Processing: "+lat+" " +lng+" "+accuracy);
                    Log.d(TAG,query.toString());

                    //Query location
//                    client.post(context,context.getString(R.string.server_url)+"location",ent,"application/json",new JsonHttpResponseHandler() {
//                        @Override
//                        public void onSuccess(int statusCode, Header[] headers, JSONObject responseBody) {
//                            Log.d(TAG,"got response: "+responseBody.toString());
//                            try{
//                                JSONArray predictions = responseBody.getJSONArray("predictions");
//                                currentFloor = predictions.getJSONObject(0).getInt("floor");
//                                lat = predictions.getJSONObject(0).getDouble("lat");
//                                lng = predictions.getJSONObject(0).getDouble("lng");
//                                accuracy = (float) predictions.getJSONObject(0).getDouble("probability");
                                  // Does not work here
//                            } catch(JSONException e){
//                                e.printStackTrace();
//                            }
//                        }
//                    });
                } catch (Exception e){
                    Log.d(TAG,e.toString());
                }
                return null;
            });
        } catch (SecurityException e){
            Toast.makeText(context, "Enable Location Permissions from Settings", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void removeLocationUpdates() {

    }

    @Override
    public Type obtainType() {
        return null;
    }
}
