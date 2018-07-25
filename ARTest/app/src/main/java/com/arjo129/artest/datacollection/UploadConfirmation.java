package com.arjo129.artest.datacollection;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.arjo129.artest.LoginActivity;
import com.arjo129.artest.R;

import java.util.ArrayList;

public class UploadConfirmation extends AppCompatActivity {
    private static final String TAG = "UploadConfirmation";
    ArrayAdapter<String> listAdapter;
    ArrayList<String> creation_times;
    private WifiFingerprintList wifiList;
    private boolean delete_item = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_confirmation);
        ActionBar actionBar = this.getSupportActionBar();
        // Set the action bar back button to look like an up button
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        wifiList = WifiFingerprintList.getInstance();
        creation_times = new ArrayList<>();
        for(WifiFingerprint fingerprint : wifiList.wifiFingerprints){
            creation_times.add("FingerPrint collected on:"+fingerprint.getDate());
        }
        ListView lv = findViewById(R.id.upload_list);
        listAdapter = new ArrayAdapter<>(this,R.layout.upload_listview,creation_times);
        lv.setAdapter(listAdapter);
        lv.setOnItemClickListener((adapterView, view, i, l) -> {
            Log.d(TAG,"removing item "+i);
            if(delete_item) {
                creation_times.remove(i);
                wifiList.removeFingerprint(i);
                listAdapter.notifyDataSetChanged();
            }
        });
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.confirmupload, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        // When the home button is pressed, take the user back to the VisualizerActivity
        if (id == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
        } else if (id == R.id.upload_readings) {
            //NavUtils.navigateUpFromSameTask(this);
            wifiList.upload(this, (ServerResponse response) -> {
                if(response == ServerResponse.SERVER_RESPONSE_OK){
                    wifiList.wifiFingerprints.clear();
                    creation_times.clear();
                    listAdapter.notifyDataSetChanged();
                }
                else {
                    Intent login = new Intent(this, LoginActivity.class);
                    login.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(login);
                }
                return null;
            });
        }
        if(id == R.id.delete_item){
            delete_item = !delete_item;
        }
        return super.onOptionsItemSelected(item);
    }

}
