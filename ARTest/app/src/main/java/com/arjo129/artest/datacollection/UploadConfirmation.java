package com.arjo129.artest.datacollection;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.arjo129.artest.R;

import java.util.ArrayList;

public class UploadConfirmation extends AppCompatActivity {
    private static final String TAG = "UploadConfirmation";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_confirmation);
        Intent intent = getIntent();
        ArrayList<WifiFingerprint> wifiFingerprints = (ArrayList<WifiFingerprint>) intent.getSerializableExtra("fingerprints");
        ArrayList<String> creation_times = new ArrayList<>();
        for(WifiFingerprint fingerprint : wifiFingerprints){
            creation_times.add("FingerPrint collected on:"+fingerprint.getDate());
        }
        ListView lv = (ListView)findViewById(R.id.upload_list);
        ArrayAdapter<String> listAdapter = new ArrayAdapter<String>(this,R.layout.upload_listview,creation_times);
        lv.setAdapter(listAdapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Log.d(TAG,"removing item "+i);
                creation_times.remove(i);
                wifiFingerprints.remove(i);
                listAdapter.notifyDataSetChanged();
            }
        });
    }

}
