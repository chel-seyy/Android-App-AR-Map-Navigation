package com.arjo129.artest.indoorLocation;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.arjo129.artest.R;
import com.arjo129.artest.device.WifiLocation;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.mapbox.android.core.location.LocationEngineListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

public class WifiService extends Service {
    private static final String TAG = "indoorLocation.WifiService";
    private final IBinder mBinder = new WifiBinder();
    private double lat, lng, alt;
    WifiLocation wifiLocation;
    float accuracy;
    private Date lastGoodLocation;
    public class WifiBinder extends Binder {
        WifiService getService(){
            return WifiService.this;
        }
    }
    public WifiService() {

    }
    @Override
    public void onCreate() {
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
            }
            Date date = new Date();
            JSONObject query = new JSONObject();
            try {
                query.put("WIFI", wifi_list);
                AsyncHttpClient client = new AsyncHttpClient();
                client.setSSLSocketFactory(SecurityProvider.getSocketFactory(this));
                StringEntity ent = new StringEntity(query.toString());
                Log.d(TAG,"Sending request");
                Log.d(TAG, "Processing: "+lat+" " +lng+" "+accuracy);
                Log.d(TAG,query.toString());

                //Query location
                client.post(getApplicationContext(),"https://ec2-18-191-20-227.us-east-2.compute.amazonaws.com/"+"location",ent,"application/json",new JsonHttpResponseHandler() {
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
                            //setLocation(lat,lng,accuracy);
                        } catch(JSONException e){
                            e.printStackTrace();
                        }
                    }
                });

            } catch (Exception e){
                Log.d(TAG,e.toString());
            }
            return null;
        });

        wifiLocation.scanWifiNetworks();
    }
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return mBinder;
    }
    public void setListener(WifiNotificationListener wifiNotificationListener){

    }
    public void unbindListener(){

    }
}
