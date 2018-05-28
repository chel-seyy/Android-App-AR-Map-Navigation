package com.arjo129.artest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class WifiActivity extends AppCompatActivity {
    private WifiManager wifi;
    private ListView listView;
    private Button buttonScan;
    private List<ScanResult>results;
    private static int size = 0;

    ArrayList<String>arrayList = new ArrayList<>();
    ArrayAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi);

        buttonScan = findViewById(R.id.buttonScan);
        buttonScan.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                scanWifiNetworks();
            }
        });

        listView = findViewById(R.id.list);
        wifi = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if(!wifi.isWifiEnabled()){
            String message = "Wifi is disabled. Enabling it now~";
            Toast.makeText(this,message,Toast.LENGTH_LONG).show();
            wifi.setWifiEnabled(true);
        }
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, arrayList);
        listView.setAdapter(adapter);
        scanWifiNetworks();
    }

//    @Override
//    protected void onPause() {
//        super.onPause();
//        unregisterReceiver(wifi_receiver);
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//        unregisterReceiver(wifi_receiver);
//    }

    public void scanWifiNetworks(){
        arrayList.clear();
        registerReceiver(wifi_receiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        wifi.startScan();
        Toast.makeText(this, "Scanning....", Toast.LENGTH_SHORT).show();
    }

    BroadcastReceiver wifi_receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("WifiScanner", "onReceive");
            results = wifi.getScanResults();
            unregisterReceiver(this);
            Log.d("WifiScanner", "size: " +results.size());
            for(int i=0; i< results.size(); i++){
                ScanResult scanResult = results.get(i);
                arrayList.add(i+ ". "+scanResult.SSID + " - " + scanResult.capabilities);
                adapter.notifyDataSetChanged();
            }
        }
    };
}
