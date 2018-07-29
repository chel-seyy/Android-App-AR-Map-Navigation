package com.arjo129.artest.datacollection;

import android.content.Context;
import android.util.Log;

import com.arjo129.artest.R;
import com.arjo129.artest.indoorLocation.SecurityProvider;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.function.Function;
import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

public class WifiFingerprintList {
    private static final String TAG = "WifiFingerprintList";
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
    public void upload(Context ctx, Function<ServerResponse,Void> responseHandler){
        Log.d(TAG,"uploading..");
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
            responseHandler.apply(ServerResponse.SERVER_RESPONSE_ERROR);
            return;
        }
        AsyncHttpClient client = new AsyncHttpClient();
        client.setSSLSocketFactory(SecurityProvider.getSocketFactory(ctx));
        StringEntity ent;
        try {
            ent = new StringEntity(encapsulation_layer.toString());
        } catch (Exception e){
            e.printStackTrace();
            responseHandler.apply(ServerResponse.SERVER_RESPONSE_ERROR);
            return;
        }
        client.post(ctx,server,ent,"application/json", new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response){
                try {
                    Log.d(TAG,response.toString());
                    ServerResponse serverResponse;
                    if (response.get("status").equals("ok")) {
                        serverResponse = ServerResponse.SERVER_RESPONSE_OK;
                        session_secret = (String)response.get("session_secret");
                    } else{
                        //Probably logged out
                        serverResponse = ServerResponse.SERVER_RESPONSE_BAD_AUTH;

                    }
                    responseHandler.apply(serverResponse);
                } catch (Exception e){
                    e.printStackTrace();
                    responseHandler.apply(ServerResponse.SERVER_RESPONSE_ERROR);
                }
            }
        });
    }
}
