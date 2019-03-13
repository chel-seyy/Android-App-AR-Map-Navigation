package com.arjo129.artest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Done by Chelsey
 */
public class WifiActivity extends AppCompatActivity {
    private WifiManager wifi;
    private ListView listView;
    private Button buttonScan;
    private List<ScanResult>results;
    private static int size = 0;
    private boolean gps_enabled = false;
    private boolean network_enabled = false;

    ArrayList<String>arrayList = new ArrayList<>();
    ArrayAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_wifi);

        buttonScan = findViewById(R.id.buttonScan);
        buttonScan.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                adapter.clear();
                adapter.notifyDataSetChanged();
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

    @Override
    protected void onPause() {
        super.onPause();
        gps_enabled = false;
        network_enabled = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        scanWifiNetworks();
    }

    public void scanWifiNetworks(){
        enableLocation();
        if(gps_enabled && network_enabled){
            registerReceiver(wifi_receiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            wifi.startScan();
//            Toast.makeText(this, "Scanning....", Toast.LENGTH_SHORT).show();
        }
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


    public void enableLocation(){
        LocationManager lm = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
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

        if(!gps_enabled && !network_enabled){
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setPositiveButton(getResources().getString(R.string.open_location_settings), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    Intent myIntent = new Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(myIntent);
                    //get gps
                }
            });
            dialog.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    onBackPressed();
                }
            });
            dialog.show();

        }
    }

}
