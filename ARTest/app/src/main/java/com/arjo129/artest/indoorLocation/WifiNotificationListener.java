package com.arjo129.artest.indoorLocation;

public interface WifiNotificationListener {
    void onServerSendsFix(double lat, double lng, double alt);
}
