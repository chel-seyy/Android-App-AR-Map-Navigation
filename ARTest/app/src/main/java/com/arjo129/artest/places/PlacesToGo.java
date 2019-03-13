package com.arjo129.artest.places;

import android.Manifest;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;

import com.arjo129.artest.CompassActivity;
import com.arjo129.artest.LoginActivity;
import com.arjo129.artest.R;
import com.arjo129.artest.WifiActivity;
import com.arjo129.artest.indoorLocation.WifiService;

import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

/**
 * Done by Chelsey
 */
public class PlacesToGo extends AppCompatActivity
        implements EasyPermissions.PermissionCallbacks{

    private static final int LOCATION_PERMISSION = 22;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_places_to_go);
        Button button = findViewById(R.id.main_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                wifiScanning();
            }
        });
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
        return super.onOptionsItemSelected(item);
    }

    @AfterPermissionGranted(LOCATION_PERMISSION)
    private void wifiScanning(){
        String[] perms = {Manifest.permission.ACCESS_FINE_LOCATION};
        if(EasyPermissions.hasPermissions(this, perms)){
//            openWifiIntent();
            startService(new Intent(this, WifiService.class));
            Intent favsAct = new Intent(PlacesToGo.this,PlacesLectureTheatres.class);
            PlacesToGo.this.startActivity(favsAct);

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
