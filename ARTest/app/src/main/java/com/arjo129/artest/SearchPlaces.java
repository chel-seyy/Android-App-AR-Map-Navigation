package com.arjo129.artest;


import android.content.Context;
import android.location.Location;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.arjo129.artest.CollectData;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SearchPlaces  extends AppCompatActivity {
    private HashMap<Integer, List<PlaceSearch>> places;
    private Context mContext;

    SearchPlaces(Context context){
        mContext = context;
        places = new HashMap<Integer, List<PlaceSearch>>();
        loadPlaces();
    }

    private String loadJsonFromAsset(String filename){
        try{
            Log.d("SearchPlaces","Load Json");
            InputStream is = mContext.getAssets().open(filename);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            Log.d("LoadJson", "loading done");
            return new String(buffer, "UTF-8");
        } catch(IOException e){
            e.printStackTrace();
            return null;
        }
    }

    public void loadPlaces(){
        FeatureCollection featureCollection;
        try {
            featureCollection = FeatureCollection.fromJson(loadJsonFromAsset("com1floor1.geojson"));
        }catch (Exception e){
            Log.e("MapActivity","onCreate: "+e);
            return;
        }
        List<Feature> featureList = featureCollection.features();
        int level = 1;
        List<PlaceSearch> level_1_places = new ArrayList<>();
        for(int i=0; i<featureList.size(); i++){
            Feature singleLocation = featureList.get(i);
            if( singleLocation.hasProperty("name")){
                String name = singleLocation.getStringProperty("name");
                Double stringLng = ((Point)singleLocation.geometry()).coordinates().get(0);
                Double stringLat = ((Point)singleLocation.geometry()).coordinates().get(1);
                //                Log.d("MapActivity", "feature: " +name);
                LatLng locationLatLng = new LatLng(stringLat, stringLng);
                Log.d("SearchPlaces", locationLatLng.toString() + " at "+name);
                level_1_places.add(new PlaceSearch(name, locationLatLng, level) );
            }
        }
        places.put(level, level_1_places);
    }

    public List<PlaceSearch> getPlaces(int level){
        return places.get(level);
    }




}