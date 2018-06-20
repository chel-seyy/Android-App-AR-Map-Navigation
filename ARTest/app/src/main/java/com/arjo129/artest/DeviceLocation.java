package com.arjo129.artest;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineListener;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.geometry.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

public class DeviceLocation extends LocationEngine implements LocationListener {
    public static final int TWO_MINUTES = 1000*60*2;
    private double lat = 1.2953;
    private double lng = 103.7735;
    final String TAG = "DeviceLocation";
    WifiLocation wifiLocation;
    private Context context;

    public Location currentBestLocation;
    public int currentFloor;


    @SuppressLint("MissingPermission")
    public DeviceLocation(Context context){
        Log.d(TAG, "Constructing Device Location");
        this.context = context;
        requestLocationUpdates();
    }


    @Override
    public void activate() {
        requestLocationUpdates();
    }

    @Override
    public void deactivate() {

    }

    @Override
    public boolean isConnected() {
        return wifiLocation.isConnected();
    }

    @SuppressLint("MissingPermission")
    @Override
    public Location getLastLocation() {
        return currentBestLocation;
    }

    public void requestLocationUpdates() throws SecurityException{

        try{
            Log.d(TAG, "Requesting Location Updates");
            //Build a wifiLocation request
            wifiLocation = new WifiLocation(context, (HashMap<String, Integer> map)->{
                //Format WIFI list to pretty JSON
                Log.d(TAG, "Start of Wifi List");
                JSONObject wifi_list = new JSONObject();
                Log.d(TAG, "About to put into wifi list");
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

                    currentFloor = 1;
                    currentBestLocation = new Location(LocationManager.GPS_PROVIDER);
                    currentBestLocation.setLatitude(lat);
                    currentBestLocation.setLongitude(lng);
                    currentBestLocation.setAccuracy(55);
                    currentBestLocation.setTime(System.currentTimeMillis());
                    Log.d(TAG, "Setting current location");
                    //Query location
//                    client.post(context,context.getString(R.string.server_url)+"location",ent,"application/json",new JsonHttpResponseHandler() {
//                        @Override
//                        public void onSuccess(int statusCode, Header[] headers, JSONObject responseBody) {
//                            Log.d(TAG,"got response: "+responseBody.toString());
//                            try{
//                                JSONArray predictions = responseBody.getJSONArray("predictions");
//                                Double lat = predictions.getJSONObject(0).getDouble("lat");
//                                Double lng = predictions.getJSONObject(0).getDouble("lng");
//                                float accuracy = (float) predictions.getJSONObject(0).getDouble("probability");
//                                currentFloor = predictions.getJSONObject(0).getInt("floor");
//                                currentBestLocation.setLatitude(lat);
//                                currentBestLocation.setLongitude(lng);
//                                currentBestLocation.setAccuracy(accuracy);
//                                currentBestLocation.setTime(System.currentTimeMillis());
//
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


    @Override
    public void onLocationChanged(Location location) {
        if(!isBetterLocation(location)){
            currentBestLocation = location;
            requestLocationUpdates();
        }
    }

    private boolean isBetterLocation(Location location) {
        if(currentBestLocation == null){
            return true;
        }
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        Log.d(TAG, "time diff: "+timeDelta);
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }
        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;


        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate) {
            return true;
        }
        return false;
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {
        try{
            requestLocationUpdates();
        }catch (SecurityException e){
            e.printStackTrace();
        }
    }

    @Override
    public void onProviderDisabled(String s) {

    }
}
