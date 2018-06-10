package com.arjo129.artest;

import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.Toast;

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
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.CameraMode;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode;
import com.mapbox.mapboxsdk.style.layers.FillLayer;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.turf.TurfJoins;
import com.mapbox.android.core.location.LocationEngine;

import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

import static com.mapbox.mapboxsdk.style.expressions.Expression.exponential;
import static com.mapbox.mapboxsdk.style.expressions.Expression.interpolate;
import static com.mapbox.mapboxsdk.style.expressions.Expression.stop;
import static com.mapbox.mapboxsdk.style.expressions.Expression.zoom;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillOpacity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineOpacity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth;

public class CollectData extends AppCompatActivity implements LocationEngineListener,
        OnMapReadyCallback, PermissionsListener{
    private MapView mapView;
    private List<Point> boundingBox;
    private GeoJsonSource indoorBuildingSource;
    private List<List<Point>> boundingBoxList;
    private MapboxMap map;
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
            double lng= destinationCoord.getLongitude();
            double lat= destinationCoord.getLatitude();
            //Transform the coordinate space into 3x3m grids...
            int x_coord = (int)Math.round(((lat-1)*110547)/3);
            int y_coord = (int)Math.round(111320*Math.cos(Math.toRadians(lat))*lng/3);
            try {
                //Compose JSON POST request
                JSONObject encapsulation_layer = new JSONObject();
                encapsulation_layer.put("session_id", session_id);
                encapsulation_layer.put("session_secret", session_secret);
                encapsulation_layer.put("api_key", getString(R.string.server_api_key));
                encapsulation_layer.put("location", "com1f"+floor+"|"+x_coord+"|"+y_coord);
                encapsulation_layer.put("x",x_coord);
                encapsulation_layer.put("y",y_coord);
                encapsulation_layer.put("floor",floor);
                encapsulation_layer.put("WIFI",wifi_list);
                AsyncHttpClient client = new AsyncHttpClient();
                StringEntity ent = new StringEntity(encapsulation_layer.toString());
                Log.d(TAG,"Sending request");
                Log.d(TAG,encapsulation_layer.toString());
                client.post(this,getString(R.string.server_url)+"learn_location",ent,"application/json",new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject responseBody) {
                        Log.d(TAG,responseBody.toString());
                        Log.d(TAG,"Recieved response");
                        try {
                            if(responseBody.get("status").equals("failed")){
                                // Log user out as something went wrong
                                Intent loginIntent = new Intent(getApplicationContext(),LoginActivity.class);
                                startActivity(loginIntent);
                            }
                            else {
                                //Continue
                                setSessionSecret((String)responseBody.get("session_secret"));
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
                Log.d(TAG, "Completed wifi scan");
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
            return null;
        });
    }
    @SuppressWarnings({"MissingPermission"})
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

    private void getFeatureCollectionFromJson(int level) throws IOException{
        String filename = "com1floor"+String.valueOf(level)+".geojson";
        try {
            featureCollection = FeatureCollection.fromJson(loadJsonFromAsset(filename));
        } catch (Exception e){
            Log.d("MapActivity", "getFeatureCollectionFromJson: "+e);
        }
    }
    private void initializeNewLevel(int level){
        String filename = "com1floor"+String.valueOf(level)+".geojson";
        indoorBuildingSource.setGeoJson(loadJsonFromAsset(filename));
        map.removeAnnotations();
        featureCollection = null;
        try {
            getFeatureCollectionFromJson(level);
        }catch (Exception e){
            Log.e("MapActivity","onCreate: "+e);
        }
        List<Feature> featureList = featureCollection.features();


        Log.d("Mapactivity","Building features list");
        for(int i=0; i<featureList.size(); i++){
            Feature singleLocation = featureList.get(i);
            if( singleLocation.hasProperty("name")){
                String name = singleLocation.getStringProperty("name");
                Double stringLng = ((Point)singleLocation.geometry()).coordinates().get(0);
                Double stringLat = ((Point)singleLocation.geometry()).coordinates().get(1);
//                Log.d("MapActivity", "feature: " +name);
                LatLng locationLatLng = new LatLng(stringLat, stringLng);

                map.addMarker(new MarkerOptions()
                        .position(locationLatLng)
                        .title(name));
            }
        }
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
            Log.d("LoadJson", "loading....");
            InputStream is = getAssets().open(filename);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            Log.d("LOadJson", filename);
            return new String(buffer, "UTF-8");
        } catch(IOException e){
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        map = mapboxMap;

        map.addOnMapClickListener(new MapboxMap.OnMapClickListener(){

            @Override
            public void onMapClick(@NonNull LatLng point) {
//                String string = String.format(Locale.ENGLISH,"User clicked at: %s", point.toString());
//                Toast.makeText(MainActivity.this, string, Toast.LENGTH_LONG).show();

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
                double lng = mapboxMap.getCameraPosition().target.getLongitude();
                double lat = mapboxMap.getCameraPosition().target.getLatitude();
//                Log.d("CameraMove", "Position: "+String.valueOf(lng)+" , " + String.valueOf(lat));
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


        indoorBuildingSource = new GeoJsonSource("indoor-building", loadJsonFromAsset("com1floor0.geojson"));
        mapboxMap.addSource(indoorBuildingSource);
        loadBuildingLayer();
        try {
            getFeatureCollectionFromJson(0);
        }catch (Exception e){
            Log.e("MapActivity","onCreate: "+e);
        }
        List<Feature> featureList = featureCollection.features();
        Log.d("Mapactivity","Building features list");
        for(int i=0; i<featureList.size(); i++){
            Feature singleLocation = featureList.get(i);
            if( singleLocation.hasProperty("name")){

                String name = singleLocation.getStringProperty("name");
                Double stringLng = ((Point)singleLocation.geometry()).coordinates().get(0);
                Double stringLat = ((Point)singleLocation.geometry()).coordinates().get(1);
//                Log.d("MapActivity", "feature: " +name);
                LatLng locationLatLng = new LatLng(stringLat, stringLng);

                mapboxMap.addMarker(new MarkerOptions()
                        .position(locationLatLng)
                        .title(name));
            }

        }
//        enableLocationPlugin();
    }

    public void enableLocationPlugin(){
        if(PermissionsManager.areLocationPermissionsGranted(this)){
            initializeLocationEngine();
            initializeLocationLayer();
            getLifecycle().addObserver(locationLayerPlugin);
        }
        else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }
    private void initializeLocationEngine(){
        locationEngine = new LocationEngineProvider(this).obtainBestLocationEngineAvailable();
        locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
        locationEngine.activate();

        Location lastlocation = locationEngine.getLastLocation();
        if(lastlocation!=null){
            originLocation = lastlocation;
            setCameraPosition(lastlocation);
        }
        else {
            locationEngine.addLocationEngineListener(this);
        }
    }

    private void initializeLocationLayer(){
        locationLayerPlugin = new LocationLayerPlugin(mapView, map,locationEngine);
        locationLayerPlugin.setLocationLayerEnabled(true);
        locationLayerPlugin.setCameraMode(CameraMode.TRACKING);
        locationLayerPlugin.setRenderMode(RenderMode.NORMAL);
    }

    private void setCameraPosition(Location location){
        Log.d("Cam position", String.valueOf(location.getLatitude())+", "+String.valueOf(location.getLongitude()));
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(location.getLatitude(), location.getLongitude()), 16));
    }

    @Override
    public void onConnected() {
        locationEngine.requestLocationUpdates();
    }

    @Override
    public void onLocationChanged(Location location) {
        if(location!= null){
            originLocation = location;
            setCameraPosition(location);
            locationEngine.removeLocationEngineListener(this);

        }
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        // User denies the first time, this is the 2nd time permission is presented
        // Present toast or dialog to explain why permission is needed
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if(granted) enableLocationPlugin();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}

