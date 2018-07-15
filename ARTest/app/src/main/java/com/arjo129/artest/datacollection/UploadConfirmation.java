package com.arjo129.artest.datacollection;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.arjo129.artest.LoginActivity;
import com.arjo129.artest.MapActivity;
import com.arjo129.artest.R;

import java.util.ArrayList;

public class UploadConfirmation extends AppCompatActivity {
    private static final String TAG = "UploadConfirmation";
    ArrayAdapter<String> listAdapter;
    ArrayList<String> creation_times;
    private WifiFingerprintList wifiList;
    private boolean deletitem = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_confirmation);
        wifiList = WifiFingerprintList.getInstance();
        creation_times = new ArrayList<>();
        for(WifiFingerprint fingerprint : wifiList.wifiFingerprints){
            creation_times.add("FingerPrint collected on:"+fingerprint.getDate());
        }
        ListView lv = (ListView)findViewById(R.id.upload_list);
        listAdapter = new ArrayAdapter<String>(this,R.layout.upload_listview,creation_times);
        lv.setAdapter(listAdapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Log.d(TAG,"removing item "+i);
                if(deletitem) {
                    creation_times.remove(i);
                    wifiList.removeFingerprint(i);
                    listAdapter.notifyDataSetChanged();
                }
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
        if (id == R.id.upload_readings) {
            //NavUtils.navigateUpFromSameTask(this);
            //TODO Upload thw wifilist
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
            deletitem = !deletitem;
        }
        return super.onOptionsItemSelected(item);
    }

}
