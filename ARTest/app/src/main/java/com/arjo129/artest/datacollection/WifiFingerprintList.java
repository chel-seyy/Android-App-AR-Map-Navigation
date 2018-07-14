package com.arjo129.artest.datacollection;

import android.content.Context;

import com.arjo129.artest.R;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

public class WifiFingerprintList {
    private static final WifiFingerprintList ourInstance = new WifiFingerprintList();
    public static WifiFingerprintList getInstance() {
        return ourInstance;
    }
    public String session_secret,session_id;
    public ArrayList<WifiFingerprint> wifiFingerprints;
    private WifiFingerprintList() {
        if(wifiFingerprints == null) wifiFingerprints = new ArrayList<>();
    }
    public void removeFingerprint(int i){
        wifiFingerprints.remove(i);
    }
    public void addFingerprint(WifiFingerprint wifiFingerprint){
        wifiFingerprints.add(wifiFingerprint);
    }
    public boolean upload(Context ctx){
        String APIKEY = ctx.getString(R.string.server_api_key);
        String server = ctx.getString(R.string.server_url)+"batch_learn";
        JSONObject encapsulation_layer = new JSONObject();
        try {
            encapsulation_layer.put("session_id", session_id);
            encapsulation_layer.put("session_secret", session_secret);
            encapsulation_layer.put("api_key", APIKEY);
            JSONArray measurements = new JSONArray();
            for(WifiFingerprint measurement: wifiFingerprints){
                measurements.put(measurement.toJSON());
            }
            encapsulation_layer.put("measurements", measurements);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        AsyncHttpClient client = new AsyncHttpClient();
        StringEntity ent;
        try {
            ent = new StringEntity(encapsulation_layer.toString());
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
        client.post(ctx,server,ent,"application/json", new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response){
                try {
                    if (response.get("status").equals("ok")) {

                    } else{
                        //Probably logged out
                    }
                } catch (Exception e){
                    e.printStackTrace();
                    return;
                }
            }
        });
        return true;
    }
}
