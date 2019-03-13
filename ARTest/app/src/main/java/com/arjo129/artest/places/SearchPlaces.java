package com.arjo129.artest.places;


import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.geometry.LatLng;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Done by Chelsey
 */
public class SearchPlaces  extends AppCompatActivity {
    private Set<PlaceSearch> places;
    private Context mContext;

    SearchPlaces(Context context, int level){
        mContext = context;
        places = loadPlaces(level);
    }

    SearchPlaces(Context context) {
        mContext = context;
        places = new LinkedHashSet<>();
        for (int level = 0; level < 3; level++) {
            Set<PlaceSearch> levelPlaces = loadPlaces(level);
            places.addAll(levelPlaces);
        }
    }

    private String loadJsonFromAsset(String filename){
        try{
            InputStream is = mContext.getAssets().open(filename);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            return new String(buffer, "UTF-8");
        } catch(IOException e){
            e.printStackTrace();
            return null;
        }
    }

    public Set<PlaceSearch> loadPlaces(int level){
        FeatureCollection featureCollection;
        try {
            String filename =  "com1floor"+level+".geojson";
            featureCollection = FeatureCollection.fromJson(loadJsonFromAsset(filename));
        }catch (Exception e){
            return null;
        }
        List<Feature> featureList = featureCollection.features();
        Set<PlaceSearch> level_1_places = new LinkedHashSet<>();
        for(int i=0; i<featureList.size(); i++){
            Feature singleLocation = featureList.get(i);
            if( singleLocation.hasProperty("name")){
                String name = singleLocation.getStringProperty("name");
                Double stringLng = ((Point)singleLocation.geometry()).coordinates().get(0);
                Double stringLat = ((Point)singleLocation.geometry()).coordinates().get(1);
                LatLng locationLatLng = new LatLng(stringLat, stringLng);
                locationLatLng.setAltitude((int) level);
                level_1_places.add(new PlaceSearch(name, locationLatLng, level) );
            }
        }
        return level_1_places;
    }

    public Set<PlaceSearch> getPlaces(){
        return places;
    }




}