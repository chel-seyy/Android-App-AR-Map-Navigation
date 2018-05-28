package com.arjo129.artest;

import android.Manifest;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.GridView;
import android.widget.Toast;

import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class PlacesToGo extends AppCompatActivity
        implements EasyPermissions.PermissionCallbacks{

    private static final int LOCATION_PERMISSION = 22;
    private GridView gridView;
    String[] places = {
            "Lecture Theatres",
            "Favourites",
            "Recent"

    };
    int[] images = {
            R.mipmap.ic_location_gps_foreground,
            R.mipmap.ic_happy_star_foreground,
            R.mipmap.ic_recents_foreground

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_places_to_go);

        gridView = (GridView)findViewById(R.id.grid_view);

        GridAdapter gridAdapter = new GridAdapter(this, places, images);
        gridView.setAdapter(gridAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        MenuItem loginItem = menu.findItem(R.id.activity_login);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.activity_login){
            Intent loginIntent = new Intent(this, LoginActivity.class);
            startActivity(loginIntent);
        }
        if(item.getItemId() == R.id.activity_compass){
            Intent compassIntent = new Intent(this, CompassActivity.class);
            startActivity(compassIntent);
        }
        if(item.getItemId() == R.id.activity_wifi){
            wifiScanning();
        }
        return super.onOptionsItemSelected(item);
    }
    @AfterPermissionGranted(LOCATION_PERMISSION)
    private void wifiScanning(){
        String[] perms = {Manifest.permission.ACCESS_FINE_LOCATION};
        if(EasyPermissions.hasPermissions(this, perms)){
            openWifiIntent();

        }
        else EasyPermissions.requestPermissions(this, "We need permissions to do Wifi Scanning",
                LOCATION_PERMISSION, perms);
    }

    public void openWifiIntent(){
        Intent wifiIntent = new Intent(this, WifiActivity.class);
        startActivity(wifiIntent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        EasyPermissions.onRequestPermissionsResult(requestCode,permissions,grantResults,this);

    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        if(EasyPermissions.somePermissionPermanentlyDenied(this, perms)){
            new AppSettingsDialog.Builder(this).build().show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE){
            wifiScanning();
        }
    }
}
