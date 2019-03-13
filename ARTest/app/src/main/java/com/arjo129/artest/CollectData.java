package com.arjo129.artest;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;

import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.Button;

import com.arjo129.artest.datacollection.UploadConfirmation;
import com.arjo129.artest.datacollection.WifiFingerprint;
import com.arjo129.artest.datacollection.WifiFingerprintList;
import com.arjo129.artest.device.WifiLocation;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.mapbox.android.core.location.LocationEngineListener;
import com.mapbox.android.core.location.LocationEnginePriority;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.CameraMode;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode;
import com.mapbox.mapboxsdk.style.layers.FillLayer;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.turf.TurfJoins;
import com.mapbox.android.core.location.LocationEngine;

import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

import static com.mapbox.mapboxsdk.style.expressions.Expression.exponential;
import static com.mapbox.mapboxsdk.style.expressions.Expression.interpolate;
import static com.mapbox.mapboxsdk.style.expressions.Expression.stop;
import static com.mapbox.mapboxsdk.style.expressions.Expression.zoom;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillOpacity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconSize;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineOpacity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth;

/**
 * Done by Chelsey
 */
public class CollectData extends AppCompatActivity implements
        OnMapReadyCallback, PermissionsListener{
    private MapView mapView;
    private List<Point> boundingBox;
    private GeoJsonSource indoorBuildingSource;
    private List<List<Point>> boundingBoxList;
    private MapboxMap map;
    private Button[] buttons;
    private GeoJsonSource toiletSource, elevatorSource, stairSource;
    private boolean settingUp = true;
    private WifiLocation wifilocation; // Custom class to wrap wifi scans which will be done in multiple placed through this app
    private View levelButtons;
    private FeatureCollection featureCollection;
    private LocationLayerPlugin locationLayerPlugin;
    private LocationEngine locationEngine;
    private PermissionsManager permissionsManager;
    private Location originLocation;
    private Marker destinationMarker;
    private LatLng destinationCoord;
    private int floor = -1; //Keep track of the floor
    private String TAG = "CollectData"; // Used for log.d
    private String session_secret;
    private String session_id;
    //private ArrayList<WifiFingerprint> fingerprints;
    private WifiFingerprintList fingerprints;
    private static final LatLngBounds COM1_BOUNDS = new LatLngBounds.Builder()
            .include(new LatLng(1.2957635, 103.7730444))
            .include(new LatLng(1.2943826, 103.7745324))
            .build();


    void setSessionSecret(String str){
        session_secret = str;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_collect_data);
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        setLevelButtons();
        fingerprints = WifiFingerprintList.getInstance();
        //Extract session_id and session_secret as given by LoginActivity
        Intent current_intent =  getIntent();
        session_id = current_intent.getStringExtra("session_id");
        session_secret  = current_intent.getStringExtra("session_secret");
        //Instantiate the wifilocation class with the context, and a cllback function to
        //make it easy to post data
        wifilocation = new WifiLocation(this,(HashMap<String,Integer> map) -> {
            //Format WIFI list so the server can learn a location
            JSONObject wifi_list = new JSONObject();
            for (Map.Entry<String, Integer> item : map.entrySet()) {
                String key = item.getKey();
                int value = item.getValue();
                try {
                    wifi_list.put(key, value);
                }
                catch(Exception e){
                    Log.d(TAG,e.toString());
                    return null;
                }
                Log.d(TAG, "Got wife bssid: "+ key +" , RSSI:"+ value + "session_secret");
            }
            if(destinationCoord == null){
                return null;
            }
            double lng= destinationCoord.getLongitude();
            double lat= destinationCoord.getLatitude();
            WifiFingerprint wifiFingerprint = new WifiFingerprint(lat,lng,floor,map);
            fingerprints.addFingerprint(wifiFingerprint);
            //Transform the coordinate space into 3x3m grids...
            return null;
        });
    }

    private void setLevelButtons(){
        Button buttonSecondLevel = findViewById(R.id.second_level_button);
        buttonSecondLevel.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                floor = 2;
                initializeNewLevel(floor);
            }
        });
        Button buttonFirstLevel = findViewById(R.id.first_level_button);
        buttonFirstLevel.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                floor = 1;
                initializeNewLevel(floor);
            }
        });
        Button buttonZeroLevel = findViewById(R.id.zero_level_button);
        buttonZeroLevel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                floor = 0;
                initializeNewLevel(floor);
            }
        });
        buttons = new Button[]{buttonZeroLevel, buttonFirstLevel, buttonSecondLevel};
    }

    private void hideLevelButton(){
        AlphaAnimation animation = new AlphaAnimation(1.0f,0.0f);
        animation.setDuration(500); // millisecs
        levelButtons.startAnimation(animation);
        levelButtons.setVisibility(View.GONE);
    }
    private void showLevelButton(){
        AlphaAnimation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(500);
        levelButtons.startAnimation(animation);
        levelButtons.setVisibility(View.VISIBLE);
    }
    private void setColorButton(int level){
        buttons[level].setBackgroundResource(R.color.green);
        for(int i=0; i<3;i++){
            if(i!=level){
                buttons[i].setBackgroundResource(R.color.turquoise);
            }
        }
    }
    private void initializeNewLevel(int level){
        String filename = "com1floor"+String.valueOf(level)+".geojson";
        indoorBuildingSource.setGeoJson(loadJsonFromAsset(filename));
        initializeNewIcons(level);
        setColorButton(level);
    }

    private void initializeNewIcons(int level) {
        List<Feature> toiletsFeatures = filterFeatures(level, "toilet");
        List<Feature> stairsFeatures = filterFeatures(level, "stair");
        List<Feature> elevatorFeatures = filterFeatures(level, "lift");
//        Log.d(TAG, String.valueOf(map.getSources().size()));
        if (settingUp){
            Log.d(TAG, "Adding new sources");
            stairSource = new GeoJsonSource("stair-source", FeatureCollection.fromFeatures(stairsFeatures));
            elevatorSource = new GeoJsonSource("elevator-source", FeatureCollection.fromFeatures(elevatorFeatures));
            toiletSource = new GeoJsonSource("toilet-source", FeatureCollection.fromFeatures(toiletsFeatures));
            map.addSource(stairSource);
            map.addSource(elevatorSource);
            map.addSource(toiletSource);
            settingUp = false;
        } else {
            stairSource.setGeoJson(FeatureCollection.fromFeatures(stairsFeatures));
            elevatorSource.setGeoJson(FeatureCollection.fromFeatures(elevatorFeatures));
            toiletSource.setGeoJson(FeatureCollection.fromFeatures(toiletsFeatures));
        }

    }
    private void initializeIconsLayer(int floor){
        Bitmap stairs = BitmapFactory.decodeResource(
                CollectData.this.getResources(), R.drawable.staircase_marker);
        map.addImage("staircase-image", stairs);

        Bitmap toilet_icon = BitmapFactory.decodeResource(
                CollectData.this.getResources(), R.drawable.toilet_marker);
        map.addImage("toilet-image", toilet_icon);

        Bitmap elevator_icon = BitmapFactory.decodeResource(
                CollectData.this.getResources(), R.drawable.elevator_marker);
        map.addImage("elevator-image", elevator_icon);

        initializeNewIcons(floor);
        addSymbolLayer();
    }
    private void addSymbolLayer(){
        SymbolLayer stairs_layer = new SymbolLayer("stairs.layer.id", "stair-source");
        SymbolLayer toilets_layer = new SymbolLayer("toilets.layer.id", "toilet-source");
        SymbolLayer elevator_layer = new SymbolLayer("elevator.layer.id", "elevator-source");
        map.addLayer(stairs_layer);
        map.addLayer(toilets_layer);
        map.addLayer(elevator_layer);
        stairs_layer.withProperties(PropertyFactory.iconImage("staircase-image"),
                iconSize((float) 0.4));
        toilets_layer.withProperties(PropertyFactory.iconImage("toilet-image"),
                iconSize((float) 0.2));
        elevator_layer.withProperties(PropertyFactory.iconImage("elevator-image"),
                iconSize((float) 0.25));
    }
    private List<Feature> filterFeatures(int level, String key){
        FeatureCollection featureCollection;
        try {
            String filename = "com1floor" + level + ".geojson";
            featureCollection = FeatureCollection.fromJson(loadJsonFromAsset(filename));
        } catch (Exception e) {
            Log.d("MapActivity", "converting failed");
            return null;
        }
        List<Feature> featureList = featureCollection.features();
        List<Feature> featuresRequired = new ArrayList<>();
        for (int i = 0; i < featureList.size(); i++) {
            Feature singleLocation = featureList.get(i);
            if(key.equals("toilet") && singleLocation.hasProperty(key)){
                featuresRequired.add(singleLocation);
            }
            if((key.equals("stair") && singleLocation.hasProperty("connector")) ||
                    (key.equals("lift") && singleLocation.hasProperty("connector"))){
                String title = singleLocation.getStringProperty("connector");
                if(title.toLowerCase().contains(key)){
                    featuresRequired.add(singleLocation);
                }
            }
        }
        return featuresRequired;
    }

    private void loadBuildingLayer(){
        FillLayer indoorBuildingLayer = new FillLayer("indoor-building-fill","indoor-building").withProperties(
                fillColor(Color.parseColor("#eeeeee")), fillOpacity(interpolate(exponential(1f),zoom(),
                        stop(17f, 1f),
                        stop(16.5f, 0.5f),
                        stop(16f,0f))));

        map.addLayer(indoorBuildingLayer);
        Log.d("MainActtttivity", "main layer built");
        LineLayer indoorBuildingLineLayer = new LineLayer("indoor-building-line","indoor-building")
                .withProperties(lineColor(Color.parseColor("#50667f")),
                        lineWidth(0.5f),
                        lineOpacity(interpolate(exponential(1f), zoom(),
                                stop(17f,1f),
                                stop(16.5f, 0.5f),
                                stop(16f,0f))));
        map.addLayer(indoorBuildingLineLayer);
        Log.d("MainActtttivity", "line layer built");
    }


    private String loadJsonFromAsset(String filename){
        try{
//            Log.d("LoadJson", "loading....");
            InputStream is = getAssets().open(filename);
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

    private void settingMapView() {
        map.setMinZoomPreference(18);
        map.setMaxZoomPreference(22);
        map.setLatLngBoundsForCameraTarget(COM1_BOUNDS);
    }

    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        map = mapboxMap;
        settingMapView();
        map.addOnMapClickListener(new MapboxMap.OnMapClickListener(){

            @Override
            public void onMapClick(@NonNull LatLng point) {
//                String string = String.format(Locale.ENGLISH,"User clicked at: %s", point.toString());
//                Toast.makeText(CollectData.this, string, Toast.LENGTH_LONG).show();

                if(destinationMarker != null){
                    mapboxMap.removeMarker(destinationMarker);
                }
                destinationCoord = point;
                destinationMarker = mapboxMap.addMarker(new MarkerOptions()
                        .position(destinationCoord)
                        .setTitle(point.toString())
                );
                //Performing Wifi Scan how should we add an indicator
                wifilocation.scanWifiNetworks();
            }
        });


        levelButtons = findViewById(R.id.floor_level_buttons);
        boundingBox = new ArrayList<>();
        boundingBox.add(Point.fromLngLat(103.775,1.2925)); // 1.295, 103.774
        boundingBox.add(Point.fromLngLat(103.775,1.2969));
        boundingBox.add(Point.fromLngLat(103.773,1.2969));
        boundingBox.add(Point.fromLngLat(103.773,1.2925));
        boundingBoxList = new ArrayList<>();
        boundingBoxList.add(boundingBox);

        mapboxMap.addOnCameraMoveListener(new MapboxMap.OnCameraMoveListener() {
            @Override
            public void onCameraMove() {

                if(mapboxMap.getCameraPosition().zoom > 16){
                    if(TurfJoins.inside(Point.fromLngLat(mapboxMap.getCameraPosition().target.getLongitude(),
                            mapboxMap.getCameraPosition().target.getLatitude()), Polygon.fromLngLats(boundingBoxList))){

                        if(levelButtons.getVisibility()!=View.VISIBLE){
                            showLevelButton();
                        }
                    } else{
                        if(levelButtons.getVisibility() ==View.VISIBLE){
                            Log.d("CameraMove", "Outside the polygon");
                            hideLevelButton();
                        }
                    }
                } else if (levelButtons.getVisibility() == View.VISIBLE){
                    Log.d("CameraMove", "Too far");
                    hideLevelButton();
                }
            }
        });

        floor = 0;
        indoorBuildingSource = new GeoJsonSource("indoor-building", loadJsonFromAsset("com1floor" + floor + ".geojson"));
        mapboxMap.addSource(indoorBuildingSource);
        setColorButton(floor);
        loadBuildingLayer();

        initializeIconsLayer(floor);

        //enableLocationPlugin();
    }

    @Override
    public void onStart() {
        super.onStart();
        if(locationEngine != null){
            locationEngine.requestLocationUpdates();
        }
        if(locationLayerPlugin != null){
            locationLayerPlugin.onStart();
        }
        mapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(locationEngine != null){
            locationEngine.removeLocationUpdates();
        }
        if(locationLayerPlugin != null){
            locationLayerPlugin.onStop();
        }
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        if(locationEngine!= null){
            locationEngine.deactivate();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        // User denies the first time, this is the 2nd time permission is presented
        // Present toast or dialog to explain why permission is needed
    }

    @Override
    public void onPermissionResult(boolean granted) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.collectdata, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        // When the home button is pressed, take the user back to the VisualizerActivity
        if (id == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
        }
        else if(id == R.id.uploadToServer){
            Intent mapIntent = new Intent(this, UploadConfirmation.class);
            startActivity(mapIntent);
        }
        return super.onOptionsItemSelected(item);
    }

}

