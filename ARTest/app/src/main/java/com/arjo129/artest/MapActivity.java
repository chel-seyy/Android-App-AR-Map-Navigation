package com.arjo129.artest;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.Toast;

import com.arjo129.artest.arrendering.ARScene;
import com.arjo129.artest.arrendering.DirectionInstruction;
import com.arjo129.artest.device.WifiLocation;
//import com.arjo129.artest.places.Routing;
import com.arjo129.artest.places.BearingUtils;
import com.arjo129.artest.places.Routing;
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
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Polyline;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
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

public class MapActivity extends AppCompatActivity implements LocationEngineListener,
        OnMapReadyCallback, PermissionsListener{
    private MapView mapView;
    private List<Point> boundingBox;
    private GeoJsonSource indoorBuildingSource;
    private List<List<Point>> boundingBoxList;
    private Icon green_icon;
    private List<Marker> routeDrawn;
    private List<LatLng>routePolyline;
    private Polyline polyline;

    private MapboxMap map;
    private View levelButtons;
    private Button[] buttons;
    private Button routeButton;
    private Button buttonZeroLevel, buttonFirstLevel, buttonSecondLevel;

    private LocationLayerPlugin locationLayerPlugin;
    private LocationEngine locationEngine;
    private PermissionsManager permissionsManager;
    private Location originLocation;

    private Routing mapRouting;

    private Marker startMarker, destinationMarker;
    private LatLng startCoord, destinationCoord;
    private int floor = -1; //Keep track of the floor
    private String TAG = "MapActivity1"; // Used for log.d

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_map);
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        setLevelButtons();
        if(savedInstanceState != null){
            floor = savedInstanceState.getInt("floor");
            double start_lat = savedInstanceState.getDouble("start_lat");
            double start_lng = savedInstanceState.getDouble("start_lng");
            double dest_lat = savedInstanceState.getDouble("dest_lat");
            double dest_lng = savedInstanceState.getDouble("dest_lng");
            if(start_lat != 0.0 && start_lng != 0.0){
                startCoord = new LatLng(start_lat, start_lng);
            }
            if(dest_lat != 0.0 && dest_lng != 0.0){
                destinationCoord = new LatLng(dest_lat, dest_lng);
            }

        }

        mapRouting = new Routing(this);

        /*// Mock starting position:
        if(startCoord == null){
            Log.d("MapActivity", "Start coord");
            startCoord = new LatLng(1.295252,103.7737);

        }*/


        Button route_button = findViewById(R.id.start_route_buttton);
        route_button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Log.d("MapActivity", "Clicked route button");
                if(startMarker != null){
                    map.removeMarker(startMarker);
                }

                // Real starting position:

                // Remember to enable the location plugin!!
                locationLayerPlugin.setLocationLayerEnabled(false);
                startCoord = new LatLng(originLocation.getLatitude(), originLocation.getLongitude());

                startMarker = map.addMarker(new MarkerOptions()
                        .position(startCoord)
//                        .icon(green_icon)
                );

                if(checkOutBoundMarkers()){
                    return;
                }

                // drawing route on map
                if(destinationMarker != null){
                    if(routeDrawn!= null && !routeDrawn.isEmpty()){
                        for(Marker marker: routeDrawn){
                            map.removeMarker(marker);
                        }
                    }
                    routeDrawn = new ArrayList<>();
                    List<Node> drawNodes = mapRouting.getRoute(startCoord, destinationCoord);
                    drawRoute(drawNodes);
//                    route_button.setEnabled(false);
                }
                else{
                    Log.d("MapActivity", "no route to plot");
                }

            }
        });

        Button startARButton = (Button)findViewById(R.id.start_AR_buttton);
        startARButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(startMarker != null){
                    map.removeMarker(startMarker);
                }

                // Real starting position:

                // Remember to enable the location plugin!!
                //
//                locationLayerPlugin.setLocationLayerEnabled(false);
//                startCoord = new LatLng(originLocation.getLatitude(), originLocation.getLongitude());
                routeDrawn = new ArrayList<>();
                Log.d(TAG,"Generating path");
                List<Node> path = mapRouting.getRoute(startCoord, destinationCoord);
                ArrayList<DirectionInstruction> directionInstructions = new ArrayList<>();
                Node prevNode = null;
                for(Node node: path){
                    if(prevNode != null){
                        float dist = (float) BearingUtils.calculate_distance(node.coordinate,prevNode.coordinate)*1000;
                        float bearing = (float)(prevNode.bearing + 360)%360;
                        DirectionInstruction dirInst = new DirectionInstruction(dist, bearing);
                        directionInstructions.add(dirInst);
                        Log.d(TAG,"Adding instruction "+dist+"m" + ","+bearing);
                    }
                    prevNode = node;
                }
                Intent intent = new Intent(MapActivity.this,ARActivity.class);
                intent.putExtra("Directions",(Serializable)directionInstructions);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        map = mapboxMap;
        addIcons();
        // Adding markers from before
        if(startCoord!= null) {
            startMarker = map.addMarker(new MarkerOptions()
                    .position(startCoord)
            );
        }
        if(destinationCoord != null){
            destinationMarker = map.addMarker(new MarkerOptions()
                    .position(destinationCoord)
            );
        }

        map.addOnMapClickListener(new MapboxMap.OnMapClickListener(){

            @Override
            public void onMapClick(@NonNull LatLng point) {

                if(destinationMarker != null){
                    mapboxMap.removeMarker(destinationMarker);
                }
                Bitmap green_marker = BitmapFactory.decodeResource(
                        MapActivity.this.getResources(), R.drawable.green_marker);

                destinationCoord = point;
                destinationMarker = mapboxMap.addMarker(new MarkerOptions()
                        .position(destinationCoord)
                        .setTitle(point.toString())
                );


                /*if(mapRouting.withinPolygon(point)){
                    Toast.makeText(MapActivity.this, "Inside Polygon", Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(MapActivity.this, "Outside Polygon", Toast.LENGTH_SHORT).show();
                }*/

                // TODO: Launch the polyline to go
            }

        });

        /*
         * Camera bounding box
         */
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

        // TODO: Enable location but not animate camera sometimes
        enableLocationPlugin();



        /*
         * Location entered
         */
        Intent before = getIntent();
        if(before.hasExtra("lat") && before.hasExtra("lng") && before.hasExtra("place_name") && before.hasExtra("level")){
            double lat = before.getDoubleExtra("lat", 0);
            double lng = before.getDoubleExtra("lng", 0);
            String place_name = before.getStringExtra("place_name");
            int level = before.getIntExtra("level",0);

            // Load map layout for that level
            indoorBuildingSource = new GeoJsonSource("indoor-building", loadJsonFromAsset("com1floor"+level+".geojson"));
            mapboxMap.addSource(indoorBuildingSource);
            loadBuildingLayer();
            setColorButton(level);


            // Place destination marker
            if(destinationMarker != null){
                mapboxMap.removeMarker(destinationMarker);
            }
            destinationCoord = new LatLng(lat,lng);
            destinationMarker = mapboxMap.addMarker(new MarkerOptions()
                    .position(destinationCoord)
                    .setTitle(place_name)
            );

            return;
        } else{
            if(floor != -1){    // SavedinstanceState
                indoorBuildingSource = new GeoJsonSource("indoor-building", loadJsonFromAsset("com1floor"+floor+".geojson"));
                setColorButton(floor);
            }
            else{
                indoorBuildingSource = new GeoJsonSource("indoor-building", loadJsonFromAsset("com1floor1.geojson"));
                setColorButton(1);
            }
            mapboxMap.addSource(indoorBuildingSource);
            loadBuildingLayer();
        }
    }

    private boolean checkOutBoundMarkers(){
        // To check for out of bound markers
        if(destinationCoord != null && !mapRouting.withinPolygon(destinationCoord)){
            Toast.makeText(MapActivity.this, "Out of COM1!", Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

    private void drawRoute(List<Node> waypoints){
        if(waypoints == null || waypoints.size() <= 0)return;
//        map.removeMarker(startMarker);
//        map.removeMarker(destinationMarker);
        if(routePolyline != null && !routePolyline.isEmpty()){
            map.removePolyline(polyline);
        }
        routePolyline = new ArrayList<>();


        for(int i=0; i<waypoints.size();i++){
            routePolyline.add(waypoints.get(i).coordinate);
//            Marker marker = map.addMarker(new MarkerOptions()
//                    .position(waypoints.get(i).coordinate)
//                    .setTitle(String.valueOf(i)+" || "+waypoints.get(i).bearing)
//            );
//            routeDrawn.add(marker);
        }

        PolylineOptions polylineOptions = new PolylineOptions()
                .addAll(routePolyline)
                .color(Color.GRAY)
                .width(2f);
        polyline = map.addPolyline(polylineOptions);
    }

    /*
     * Buttons
     *
     */

    private void setLevelButtons(){
        buttonZeroLevel = findViewById(R.id.zero_level_button);
        buttonFirstLevel = findViewById(R.id.first_level_button);
        buttonSecondLevel = findViewById(R.id.second_level_button);
        buttonZeroLevel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                floor = 0;
                initializeNewLevel(floor);
                setColorButton(floor);
            }
        });
        buttonFirstLevel.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                floor = 1;
                initializeNewLevel(floor);
                setColorButton(floor);
            }
        });
        buttonSecondLevel.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                floor = 2;
                initializeNewLevel(floor);
                setColorButton(floor);
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


    /*
     * Floor layout initialization
     */
    private void initializeNewLevel(int level){
        String filename = "com1floor"+String.valueOf(level)+".geojson";
        indoorBuildingSource.setGeoJson(loadJsonFromAsset(filename));
//        map.removeAnnotations();
//        featureCollection = null;
//        try {
//            getFeatureCollectionFromJson(level);
//        }catch (Exception e){
//            Log.e("MapActivity","onCreate: "+e);
//        }
//        List<Feature> featureList = featureCollection.features();
//
//
//        Log.d("Mapactivity","Building features list");
//        for(int i=0; i<featureList.size(); i++){
//            Feature singleLocation = featureList.get(i);
//            if( singleLocation.hasProperty("name")){
//                String name = singleLocation.getStringProperty("name");
//                Double stringLng = ((Point)singleLocation.geometry()).coordinates().get(0);
//                Double stringLat = ((Point)singleLocation.geometry()).coordinates().get(1);
////                Log.d("MapActivity", "feature: " +name);
//                LatLng locationLatLng = new LatLng(stringLat, stringLng);
//
//                map.addMarker(new MarkerOptions()
//                        .position(locationLatLng)
//                        .title(name));
//            }
//        }
    }

    private void loadBuildingLayer(){
        FillLayer indoorBuildingLayer = new FillLayer("indoor-building-fill","indoor-building").withProperties(
                fillColor(Color.parseColor("#eeeeee")), fillOpacity(interpolate(exponential(1f),zoom(),
                        stop(17f, 1f),
                        stop(16.5f, 0.5f),
                        stop(16f,0f))));

        map.addLayer(indoorBuildingLayer);

        LineLayer indoorBuildingLineLayer = new LineLayer("indoor-building-line","indoor-building")
                .withProperties(lineColor(Color.parseColor("#50667f")),
                        lineWidth(0.5f),
                        lineOpacity(interpolate(exponential(1f), zoom(),
                                stop(17f,1f),
                                stop(16.5f, 0.5f),
                                stop(16f,0f))));
        map.addLayer(indoorBuildingLineLayer);
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

    private void convertFeatures() {
        int level = 1;
        FeatureCollection featureCollection;
        try {
            String filename = "com1floor" + level + ".geojson";
            featureCollection = FeatureCollection.fromJson(loadJsonFromAsset(filename));
        } catch (Exception e) {
            Log.d("MapActivity", "converting failed");
            return;
        }
        List<Feature> featureList = featureCollection.features();
        List<Feature> toilets = new ArrayList<>();
        List<Feature> stairs = new ArrayList<>();
        List<Feature> lifts = new ArrayList<>();

        for (int i = 0; i < featureList.size(); i++) {
            Feature singleLocation = featureList.get(i);
            Double stringLng, stringLat;
            LatLng locationLatLng;
            if(singleLocation.hasProperty("connector")){//door coordinates (can have > 1)
                String type = singleLocation.getStringProperty("connector");
                if(type.contains("stairs")){
                    stairs.add(singleLocation);
                }
                else{
                    lifts.add(singleLocation);
                }
            }
            else if(singleLocation.hasProperty("toilet")){
                toilets.add(singleLocation);
            }
        }
        map.addSource(new GeoJsonSource("toilet-source", FeatureCollection.fromFeatures(toilets)));
        map.addSource(new GeoJsonSource("stair-source", FeatureCollection.fromFeatures(stairs)));
        map.addSource(new GeoJsonSource("elevator-source", FeatureCollection.fromFeatures(lifts)));
    }

    /*
     * Adding Symbol Layers
     */
    private void addIcons(){

        convertFeatures();

        Bitmap stairs = BitmapFactory.decodeResource(
                MapActivity.this.getResources(), R.drawable.staircase_marker);
        map.addImage("staircase-image", stairs);

        Bitmap toilet_icon = BitmapFactory.decodeResource(
                MapActivity.this.getResources(), R.drawable.toilet_marker);
        map.addImage("toilet-image", toilet_icon);

        Bitmap elevator_icon = BitmapFactory.decodeResource(
                MapActivity.this.getResources(), R.drawable.elevator_marker);
        map.addImage("elevator-image", elevator_icon);

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


    /*
     * Location Engine & Plugin
     *
     */
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
        locationEngine = new DBLocationEngine(this);
        locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
        locationEngine.activate();
        Location lastlocation = locationEngine.getLastLocation();
        if(lastlocation!=null){
            originLocation = lastlocation;
//            setCameraPosition(lastlocation);
        }
        locationEngine.addLocationEngineListener(this);
    }

    private void initializeLocationLayer(){
        locationLayerPlugin = new LocationLayerPlugin(mapView, map,locationEngine);
        locationLayerPlugin.setLocationLayerEnabled(true);
        locationLayerPlugin.setCameraMode(CameraMode.TRACKING);
        locationLayerPlugin.setRenderMode(RenderMode.COMPASS);
        getLifecycle().addObserver(locationLayerPlugin);
        Log.d(TAG, "intialized location layer");
    }

    private void setCameraPosition(Location location){
        Log.d("Cam position", String.valueOf(location.getLatitude())+", "+String.valueOf(location.getLongitude()));
        panningTo(location.getLatitude(), location.getLongitude());
    }

    private void panningTo(double lat, double lng){
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(lat, lng), 16));
    }


    @Override
    public void onConnected() {
        locationEngine.requestLocationUpdates();
    }

    @Override
    public void onLocationChanged(Location location) {
        if(location!= null){
            originLocation = location;
            //setCameraPosition(location);
            int floor = (int)location.getAltitude();
            Log.d(TAG,"got :"+ location.getAltitude()+ "cast to" + floor);
            initializeNewLevel(floor);
            //locationEngine.removeLocationEngineListener(this);
        }
    }


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

        outState.putInt("floor", floor);
        if(startCoord!= null){
            outState.putDouble("start_lat", startCoord.getLatitude());
            outState.putDouble("start_lng", startCoord.getLongitude());
        }
        if(destinationCoord!= null){
            outState.putDouble("dest_lat", destinationCoord.getLatitude());
            outState.putDouble("dest_lng", destinationCoord.getLongitude());
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
