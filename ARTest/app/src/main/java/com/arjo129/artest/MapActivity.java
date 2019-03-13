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
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.arjo129.artest.arrendering.ARScene;
import com.arjo129.artest.arrendering.DirectionInstruction;
import com.arjo129.artest.device.WifiLocation;
import com.arjo129.artest.places.BearingUtils;
import com.arjo129.artest.places.Connector;
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
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerOptions;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.CameraMode;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode;
import com.mapbox.mapboxsdk.style.layers.FillExtrusionLayer;
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
import static com.mapbox.mapboxsdk.style.expressions.Expression.get;
import static com.mapbox.mapboxsdk.style.expressions.Expression.interpolate;
import static com.mapbox.mapboxsdk.style.expressions.Expression.stop;
import static com.mapbox.mapboxsdk.style.expressions.Expression.zoom;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillExtrusionBase;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillExtrusionColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillExtrusionHeight;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillExtrusionOpacity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillOpacity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconSize;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineOpacity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth;

/**
 * Done by Chelsey
 */
public class MapActivity extends AppCompatActivity implements LocationEngineListener,
        OnMapReadyCallback, PermissionsListener {

    private MapView mapView;
    private MapboxMap map;
    private List<Point> boundingBox;
    private static final LatLngBounds COM1_BOUNDS = new LatLngBounds.Builder()
            .include(new LatLng(1.2957635, 103.7730444))
            .include(new LatLng(1.2943826, 103.7745324))
            .build();

    private GeoJsonSource indoorBuildingSource, toiletSource, elevatorSource, stairSource;

    private List<List<Point>> boundingBoxList;
    private Icon green_icon;
    private HashMap<Integer, List<LatLng>> routePolylines;
    private HashMap<Integer, Polyline> polylines;
    private int startRouteFloor, destRouteFloor;
    private Marker startMarker, destinationMarker;
    private LatLng startCoord, destinationCoord;

    private View levelButtons;
    private Button[] buttons;
    private Button buttonZeroLevel, buttonFirstLevel, buttonSecondLevel;

    private LocationLayerPlugin locationLayerPlugin;
    private LocationEngine locationEngine;
    private PermissionsManager permissionsManager;
    private Location originLocation;
    private Routing mapRouting;
    private boolean settingUp = true;

    private android.support.v7.widget.Toolbar toolbar;
    private TextView destination_text, start_text;

    private int floor = -1; //Keep track of the floor
    private String TAG = "MapActivity1"; // Used for log.d

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_map);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);

        destination_text = findViewById(R.id.textDestination);
        start_text = findViewById(R.id.textStart);

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


        Button route_button = findViewById(R.id.start_route_button);
        route_button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
//                getSupportActionBar().hide();
                Log.d("MapActivity", "Clicked route button");
                if (startCoord == null) {
                    startCoord = locationLayerDisabledAndReturnLocation();
                    startRouteFloor = (int) startCoord.getAltitude();
                }
                locationLayerPlugin.setLocationLayerEnabled(false);


//                Log.d(TAG, "StartFloor: " + startRouteFloor + " DestFloor: " + destRouteFloor + " currentLevel: " + floor);

//                if(checkOutBoundMarkers()){
//                    return;
//                }
                if (destinationCoord != null) {
                    HashMap<Integer, List<Node>> drawNodes = mapRouting.getRoute(startCoord, destinationCoord);
                    Log.d(TAG, startCoord.toString());
                    Log.d(TAG, destinationCoord.toString());
                    buildRoute(drawNodes);
                } else {
                    Toast.makeText(MapActivity.this, "Set a destination first." , Toast.LENGTH_LONG).show();
                    Log.d("MapActivity", "no route to plot");
                }
            }
        });

        Button startARButton = (Button)findViewById(R.id.start_AR_button);
        startARButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                startCoord = locationLayerDisabledAndReturnLocation();
                startRouteFloor = (int) startCoord.getAltitude();
                Log.d(TAG,"Generating path");
                if (destinationCoord == null) {
                    Toast.makeText(MapActivity.this, "Set a destination first." , Toast.LENGTH_LONG).show();
                    return;
                }

                HashMap<Integer, List<Node>> drawNodes = mapRouting.getRoute(startCoord, destinationCoord);
                List<Node> nodesList = new ArrayList<>();
                nodesList.addAll(drawNodes.get(startRouteFloor));
                if (Math.abs(startRouteFloor - destRouteFloor) > 1) {
                    nodesList.addAll(drawNodes.get(1));
                }
                if(startRouteFloor != destRouteFloor) nodesList.addAll(drawNodes.get(destRouteFloor));
                Log.d(TAG, "nodesList length " + nodesList.size());
                buildRoute(drawNodes);
                ArrayList<DirectionInstruction> directionInstructions = new ArrayList<>();
                Node prevNode = null;
                float prev_dir = -1;
                for(Node node: nodesList){
                    if(prevNode != null){
                        float dist = (float) BearingUtils.calculate_distance(node.coordinate,prevNode.coordinate)*1000;
                        float bearing = (float)(prevNode.bearing + 360)%360;

                        if(prev_dir >= 0 && Math.abs(bearing - prev_dir) < 45 && !node.isConnector){
                            directionInstructions.get(directionInstructions.size()-1).distance+=dist;
                            directionInstructions.get(directionInstructions.size()-1).direction=bearing;
                            prev_dir = bearing;
                        }
                        else {
                            if(!node.isConnector) {
                                DirectionInstruction dirInst = new DirectionInstruction(dist, bearing);
                                directionInstructions.add(dirInst);
                                prev_dir = bearing;
                            } else {
                                DirectionInstruction dirInst = new DirectionInstruction(dist,bearing);
                                dirInst.isConnector = true;
                                dirInst.connector_type = node.connector;
                                dirInst.goingUp = node.directionUp;
                                directionInstructions.add(dirInst);
                                prev_dir = -1;
                            }
                        }

                        Log.d(TAG,"Adding instruction "+dist+"m" + ","+bearing+" connector: "+node.connector);
                    }
                    prevNode = node;
                }
                Intent intent = new Intent(MapActivity.this, ARActivity.class);
                intent.putExtra("Directions",(Serializable)directionInstructions);
                startActivity(intent);
            }
        });

        Button locationButton = findViewById(R.id.get_location_button);
        locationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Find Location again, and animate camera
                if(locationLayerPlugin != null){
                    getSupportActionBar().show();
                    locationLayerPlugin.setLocationLayerEnabled(true);
                    ((DBLocationEngine)locationEngine).enableLocation();
                    locationEngine.requestLocationUpdates();
                    setCameraPosition(originLocation);
                    startCoord = new LatLng(originLocation.getLatitude(), originLocation.getLongitude());
                    startCoord.setAltitude(originLocation.getAltitude());
                    startRouteFloor = (int) startCoord.getAltitude();
                }
                else{
                    Toast.makeText(MapActivity.this, "Null location", Toast.LENGTH_SHORT).show();
                }
            }
        });

        Button switchLocationsButton = findViewById(R.id.switch_locations_button);
        switchLocationsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (destination_text != null && !destination_text.toString().isEmpty()
                        && startCoord != null && destinationCoord != null) {
                    int tempFloor = startRouteFloor;
                    startRouteFloor = destRouteFloor;
                    destRouteFloor = tempFloor;

                    LatLng tempLatlng = startCoord;
                    startCoord = destinationCoord;
                    destinationCoord = tempLatlng;

                    Marker tempMarker = startMarker;
                    startMarker = destinationMarker;
                    destinationMarker = tempMarker;
                    refreshLevel();

                    String tempText = start_text.getText().toString();
                    start_text.setText(destination_text.getText());
                    destination_text.setText(tempText);

                    Log.d("SwitchLoc", startCoord.toString());
                    Log.d("SwitchLoc", destinationCoord.toString());
                }
            }
        });
    }

    public LatLng locationLayerDisabledAndReturnLocation() {
        locationLayerPlugin.setLocationLayerEnabled(false);
        LatLng location = new LatLng(originLocation.getLatitude(), originLocation.getLongitude());
        Log.d(TAG, "Returning origin location: " + originLocation.getAltitude());
        location.setAltitude(originLocation.getAltitude());
        return location;
    }

    private void settingMapView() {
        green_icon = IconFactory.getInstance(MapActivity.this).fromResource(R.drawable.green_marker);
//        purple_icon = IconFactory.getInstance(MapActivity.this).fromResource(R.drawable.purple_marker);
        map.setMinZoomPreference(18);
        map.setMaxZoomPreference(22);
        map.setLatLngBoundsForCameraTarget(COM1_BOUNDS);
    }

    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        map = mapboxMap;
        settingMapView();
        /*map.addOnMapClickListener(new MapboxMap.OnMapClickListener(){

            @Override
            public void onMapClick(@NonNull LatLng point) {

//                startCoord = locationLayerDisabledAndReturnLocation();

//                destinationCoord = point;
//                Log.d(TAG, "Dest coord level: " + floor);
//                destinationCoord.setAltitude(floor);
//                destRouteFloor = floor;

                //////////////////////////////
//                refreshMarkers();
            }
        });*/

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

        enableLocationPlugin();


        /*
         * Location entered
         */
        Intent before = getIntent();
        if(before.hasExtra("lat") && before.hasExtra("lng") && before.hasExtra("place_name") && before.hasExtra("level")){
            double lat = before.getDoubleExtra("lat", 0);
            double lng = before.getDoubleExtra("lng", 0);
            String place_name = before.getStringExtra("place_name");
            destination_text.setText(place_name);
            floor = before.getIntExtra("level",0);

            // Load map layout for that level
            indoorBuildingSource = new GeoJsonSource("indoor-building", loadJsonFromAsset("com1floor"+floor+".geojson"));
            mapboxMap.addSource(indoorBuildingSource);
            loadBuildingLayer();
            setColorButton(floor);

            destinationCoord = new LatLng(lat,lng);
            destinationCoord.setAltitude(floor);
            destRouteFloor = floor;
        } else{
            if(floor == -1){    // SavedinstanceState
                floor = 1;
            }
            indoorBuildingSource = new GeoJsonSource("indoor-building", loadJsonFromAsset("com1floor"+floor+".geojson"));
            setColorButton(floor);
            mapboxMap.addSource(indoorBuildingSource);
            loadBuildingLayer();
        }
        refreshLevel();
        initializeIconsLayer(floor);

    }

    private boolean checkOutBoundMarkers(){
        // To check for out of bound markers
        if(destinationCoord != null && !mapRouting.withinPolygon(destinationCoord)){
            Toast.makeText(MapActivity.this, "Out of COM1!", Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }
    private void buildRoute(HashMap<Integer, List<Node>> waypoints){
        if(waypoints == null || waypoints.size() <= 0)return;
        removeAllRoutes();
        routePolylines = new HashMap<>();
        polylines = new HashMap<>();
        for (Integer level: waypoints.keySet()) {
            List<LatLng> routePolyline = new ArrayList<>();
            for (Node node: waypoints.get(level)){
                routePolyline.add(node.coordinate);
            }
            routePolylines.put(level, routePolyline);
        }
        refreshLevel();
    }

    private void drawRoute(int level){
        List<LatLng> routePolyline = routePolylines.get(level);
        PolylineOptions polylineOptions = new PolylineOptions()
                .addAll(routePolyline)
                .color(Color.GRAY)
                .width(2f);
        Polyline polyline = map.addPolyline(polylineOptions);
        polylines.put(level, polyline);
    }
    private void removeRoute(int level){
        if(map.getPolylines().size() > 0 && polylines.get(level) != null){
            map.removePolyline(polylines.get(level));
        }
    }

    private void removeAllRoutes() {
        if (polylines == null || polylines.isEmpty()) {
            return;
        }
        for (Integer level: polylines.keySet()){
            removeRoute(level);
        }
    }


    private void setLevelButtons(){
        buttonZeroLevel = findViewById(R.id.zero_level_button);
        buttonFirstLevel = findViewById(R.id.first_level_button);
        buttonSecondLevel = findViewById(R.id.second_level_button);
        buttonZeroLevel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                floor = 0;
                initializeNewLevel(floor);
                refreshLevel();
            }
        });
        buttonFirstLevel.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                floor = 1;
                initializeNewLevel(floor);
                refreshLevel();
            }
        });
        buttonSecondLevel.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                floor = 2;
                initializeNewLevel(floor);
                refreshLevel();
            }
        });
        buttons = new Button[]{buttonZeroLevel, buttonFirstLevel, buttonSecondLevel};
    }
    private void refreshLevel() {
        refreshMarkers();
        for (Polyline line: map.getPolylines()) {
            map.removePolyline(line);
        }
        if (routePolylines != null){
            for (Integer level: routePolylines.keySet()){
                if (floor == level) {
                    drawRoute(level);
                }
            }
        }
    }

    private void refreshMarkers() {
        for (Marker marker: map.getMarkers()) {
            map.removeMarker(marker);
        }
        ///////////////////////////////////////////
        Log.d(TAG, "Refreshing markers at lvl " + floor);
        if (startCoord != null){
            if (floor == startRouteFloor) {
                startMarker = map.addMarker(new MarkerOptions()
                        .position(startCoord)
                        .setTitle("Start point")
                );
//                Log.d(TAG, "Added start marker");
            }
        }

        if (destinationCoord != null){
            if (floor == destRouteFloor) {
                destinationMarker = map.addMarker(new MarkerOptions()
                        .position(destinationCoord)
                        .setTitle(destinationCoord.toString())
                        .setIcon(green_icon)
                );
//                Log.d(TAG, "Added dest marker");
            }
        }
        ////////////////////////
        Log.d(TAG, "markers: " + map.getMarkers().size());
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
        String filename = "com1floor" + String.valueOf(level) + ".geojson";
        indoorBuildingSource.setGeoJson(loadJsonFromAsset(filename));
        initializeNewIcons(level);
        setColorButton(level);
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

//        FillExtrusionLayer roomExtrusionLayer = new FillExtrusionLayer("room-extrusion", "indoor-building");
//        roomExtrusionLayer.setProperties(fillExtrusionColor(get("color")),
//                fillExtrusionHeight(get("height")),
//                fillExtrusionBase(get("base_height")),
//                fillExtrusionOpacity(0.5f));
//        map.addLayer(roomExtrusionLayer);
    }
    private String loadJsonFromAsset(String filename){
        try{
            InputStream is = getAssets().open(filename);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
//            Log.d("LOadJson", filename);
            return new String(buffer, "UTF-8");
        } catch(IOException e){
            e.printStackTrace();
            return null;
        }
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
                MapActivity.this.getResources(), R.drawable.staircase_marker);
        map.addImage("staircase-image", stairs);

        Bitmap toilet_icon = BitmapFactory.decodeResource(
                MapActivity.this.getResources(), R.drawable.toilet_marker);
        map.addImage("toilet-image", toilet_icon);

        Bitmap elevator_icon = BitmapFactory.decodeResource(
                MapActivity.this.getResources(), R.drawable.elevator_marker);
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



    public void enableLocationPlugin(){
        Log.d(TAG, "Enabled Location Plugin");
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
        LocationLayerOptions options = LocationLayerOptions.builder(this)
                .maxZoom(22)
                .build();
        locationLayerPlugin = new LocationLayerPlugin(mapView, map, locationEngine, options);
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
                new LatLng(lat, lng), 18.5));
    }


    @Override
    public void onConnected() {
        Log.d(TAG, "onConnected");
        locationEngine.requestLocationUpdates();
    }

    @Override
    public void onLocationChanged(Location location) {
        //Log.d(TAG, "onLocationChanged");
        if(location!= null){
            originLocation = location;
            // Buggy Line: floor != altitude
//            int floor = (int)location.getAltitude();
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
        if(locationEngine!= null){
            locationEngine.deactivate();
        }
        mapView.onDestroy();
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
