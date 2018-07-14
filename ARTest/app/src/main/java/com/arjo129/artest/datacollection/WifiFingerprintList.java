package com.arjo129.artest.datacollection;

import android.content.Context;

import com.arjo129.artest.R;

import java.util.ArrayList;

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
    public void upload(Context ctx){
        = ctx.getString(R.string.server_api_key);
    }
}
