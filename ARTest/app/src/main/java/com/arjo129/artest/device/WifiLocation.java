package com.arjo129.artest.device;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import com.arjo129.artest.R;

import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

public class WifiLocation {
    private BroadcastReceiver wifi_receiver;
    private List<ScanResult> results;
    private Context context;
    private WifiManager wifi;
    private boolean scan_finished = false;
    private boolean destroyed = true;
    private boolean isRegistered = false;
    private Function<HashMap<String,Integer>, Void> function;

    public WifiLocation(Context ctx, Function<HashMap<String, Integer>, Void> handler) {
        context = ctx;
        function = handler;
        wifi = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
    }
    public void scanWifiNetworks(){
        scan_finished = false;
        wifi_receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                HashMap<String, Integer> map;
                map = new HashMap<String, Integer>();
                Log.d("WifiScanner", "onReceive");
                results = wifi.getScanResults();
                context.unregisterReceiver(this);
                Log.d("WifiScanner", "size: " + results.size());
                for (int i = 0; i < results.size(); i++) {
                    ScanResult scanResult = results.get(i);
                    map.put(scanResult.BSSID,scanResult.level);
                }
                function.apply(map);
                scan_finished = true;
            }
        };

        context.registerReceiver(wifi_receiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        isRegistered = true;
        wifi.startScan();
        destroyed = false;
//            Toast.makeText(context, "Scanning....", Toast.LENGTH_SHORT).show();

    }

    public boolean isConnected(){
        return scan_finished;
    }

    public void stopListening(){
        if(!destroyed && isRegistered) {
            context.unregisterReceiver(wifi_receiver);
            destroyed = true;
            isRegistered = false;
        }
    }
}
