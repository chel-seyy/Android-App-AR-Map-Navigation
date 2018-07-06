package com.arjo129.artest.datacollection;

import java.util.ArrayList;

public class WifiFingerprintList {
    private static final WifiFingerprintList ourInstance = new WifiFingerprintList();

    public static WifiFingerprintList getInstance() {
        return ourInstance;
    }

    public ArrayList<WifiFingerprint> wifiFingerprints;
    private WifiFingerprintList() {
    }
    public void removeFingerprint(int i){
        wifiFingerprints.remove(i);
    }
    public void addFingerprint(WifiFingerprint wifiFingerprint){
        wifiFingerprints.add(wifiFingerprint);
    }
    public void upload(){

    }
}
