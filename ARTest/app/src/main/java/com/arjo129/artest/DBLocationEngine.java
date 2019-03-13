package com.arjo129.artest;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import com.arjo129.artest.device.WifiLocation;
import com.arjo129.artest.indoorLocation.SecurityProvider;
import com.arjo129.artest.indoorLocation.WifiNotificationListener;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.Month;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

/**
 * Done by Chelsey
 */
public class DBLocationEngine extends LocationEngine implements WifiNotificationListener {
    public final static String TAG = "DBLocationEngine";
    public static final int DELAY = 1000 * 5; // change to 5 seconds
    private Handler mHandler;
    private Runnable wifiThread;
    private Date lastGoodLocation;
    private Double lat = 1.2948682;
    private Double lng = 103.773740;
    private Double alt = 1.0;
    private float accuracy = 55;
    private WifiLocation wifiLocation;
    private Context context;
    public Location currentBestLocation;
    private boolean loading = true;
    private boolean gps_enabled = false;
    private boolean network_enabled = false;

    DBLocationEngine(Context context){
        this.context = context;
        Log.d(TAG, "constructed engine");
        setLocation(lat, lng, 0);
        lastGoodLocation =  new Date(2014, 6, 20, 0, 0);

        enableLocation();
        mHandler = new Handler();
        wifiThread  = new Runnable() {
            @Override
            public void run() {
                requestLocationUpdates();
                mHandler.postDelayed(wifiThread, DELAY);
            }
        };

    }

    private boolean isLocationEnabled(){
        return gps_enabled && network_enabled;
    }


    @Override
    public void activate() {
        Log.d(TAG, "Activated");
        mHandler.postDelayed(wifiThread,DELAY);
    }

    @Override
    public void deactivate() {
        Log.d(TAG, "Deactivated");
        mHandler.removeCallbacks(wifiThread);
        if(wifiLocation != null){
            wifiLocation.stopListening();
        }
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
        Log.d(TAG, "get Last Location"+lat+","+lng);
        if(currentBestLocation == null){
            requestLocationUpdates();
        }
        if(lastGoodLocation.getTime()-(new Date()).getTime() > DELAY){
            requestLocationUpdates();
        }
        return currentBestLocation;
    }

    private void setLocation(Double lat, Double lng, float accuracy){
        loading = false;
        Log.d(TAG, "Setting location now: "+lat+" " +lng+" "+accuracy);
        currentBestLocation = new Location(LocationManager.GPS_PROVIDER);
        currentBestLocation.setLatitude(lat);
        currentBestLocation.setLongitude(lng);
        currentBestLocation.setAltitude(alt);
        //currentBestLocation.setAccuracy(accuracy);
        currentBestLocation.setTime(System.currentTimeMillis());
        Log.d(TAG, "Set: "+currentBestLocation.toString());
    }

    public boolean stillLoading(){
        return loading;
    }

    @Override
    public void requestLocationUpdates() throws SecurityException{
        if(!isLocationEnabled()){
            return;
        }
        Log.d(TAG, "Requesting Location Updates");
        try {
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
                }
                Date date = new Date();
                JSONObject query = new JSONObject();
                try {
                    query.put("WIFI", wifi_list);
                    AsyncHttpClient client = new AsyncHttpClient();
                    client.setSSLSocketFactory(SecurityProvider.getSocketFactory(context));
                    StringEntity ent = new StringEntity(query.toString());
                    Log.d(TAG,"Sending request");
                    Log.d(TAG, "Processing: "+lat+" " +lng+" "+accuracy);
                    Log.d(TAG,query.toString());

                    //Query location
                    client.post(context,context.getString(R.string.server_url)+"location",ent,"application/json",new JsonHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, JSONObject responseBody) {
                            Log.d(TAG,"got response: "+responseBody.toString());
                            try{
                                JSONArray predictions = responseBody.getJSONArray("predictions");
                                alt = (double)predictions.getJSONObject(0).getInt("floor");
                                Log.d(TAG,"floor: "+alt);
                                lat = predictions.getJSONObject(0).getDouble("lat");
                                lng = predictions.getJSONObject(0).getDouble("lng");
                                accuracy = (float) predictions.getJSONObject(0).getDouble("probability");
                                lastGoodLocation = date;
                                Log.d(TAG, "accuracy: " + accuracy);
                                setLocation(lat,lng,accuracy);
                            } catch(JSONException e){
                                e.printStackTrace();
                            }
                        }
                    });
                    for(LocationEngineListener l: this.locationListeners){
                        Log.d(TAG, "Calling friend");
                        l.onLocationChanged(currentBestLocation);
                    }
                } catch (Exception e){
                    Log.d(TAG,e.toString());
                }
                return null;
            });

            wifiLocation.scanWifiNetworks();


        } catch (SecurityException e){
            Toast.makeText(context, "Enable Location Permissions from Settings", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void removeLocationUpdates() {
        Log.d(TAG,"Destroying view");
        mHandler.removeCallbacks(wifiThread);
        if(wifiLocation != null){
            wifiLocation.stopListening();
        }
    }

    @Override
    public Type obtainType() {
        return null;
    }

    public void enableLocation(){
        LocationManager lm = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
        try{
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        }catch (Exception e){
            Log.d("WifiGPS", "GPS cannot be enabled");
        }
        try{
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        }catch (Exception e){
            Log.d("WifiGPS", "Network cannot be enabled");
        }

        if(!gps_enabled && !network_enabled) {
            Log.d("WifiLocation", Boolean.toString(gps_enabled)+ ", "+ Boolean.toString(network_enabled));
            AlertDialog.Builder dialog = new AlertDialog.Builder(context);
            dialog.setPositiveButton(context.getResources().getString(R.string.open_location_settings), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    Intent myIntent = new Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    context.startActivity(myIntent);
                    //get gps
                }
            });
            dialog.setNegativeButton(context.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    //onBackPressed();
                }
            });
            dialog.show();

        }
    }

    @Override
    public void onServerSendsFix(double lat, double lng, double alt) {

    }
}
