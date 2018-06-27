package com.arjo129.artest.places;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Pair;

import com.arjo129.artest.Node;
import com.mapbox.geojson.BoundingBox;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.turf.TurfJoins;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.mapbox.mapboxsdk.style.expressions.Expression.exponential;
import static com.mapbox.mapboxsdk.style.expressions.Expression.interpolate;
import static com.mapbox.mapboxsdk.style.expressions.Expression.stop;
import static com.mapbox.mapboxsdk.style.expressions.Expression.zoom;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineOpacity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth;

public class Routing extends AppCompatActivity {

    private Double inf = Double.POSITIVE_INFINITY;  // change to long? if dist gets Large...
    private static final Double bearing = 41.3;
    private List<List<Integer>> adjacentVertices;
    private static final int current_level = 1;
    private Context mContext;
    private HashMap<String, List<LatLng>> rooms;
    private HashMap<Integer, LatLng> junctions;

    private List<List<Pair<Integer, Double>>>adjacencyList;
    private HashMap<Integer, Integer> backtracking;
    private List<Double> distances;
    private LatLng start, end;
    List<List<Point>> boundingList;

    public Routing(Context context){
        mContext = context;
        rooms = new HashMap<>();
        junctions = new HashMap<>();
        convertFeatures();
        addToAdjacencyList();
        boundingList = new ArrayList<>();
        List<Point> perimeter = Arrays.asList(Point.fromLngLat(103.773678, 1.295476),
                Point.fromLngLat(103.77431, 1.2946974),
                Point.fromLngLat(103.774209, 1.29450826),
                Point.fromLngLat(103.773961, 1.29458369),
                Point.fromLngLat(103.7791351, 1.2945415),
                Point.fromLngLat(103.7735196, 1.2950287),
                Point.fromLngLat(103.773687,1.2951863),
                Point.fromLngLat(103.7735626, 1.29534509),

                Point.fromLngLat(103.773678, 1.295476));
        boundingList.add(perimeter);
    }

//    public List<PlaceSearch> loadPlaces(int level){
//        FeatureCollection featureCollection;
//        try {
//            String filename =  "com1floor"+level+".geojson";
//            featureCollection = FeatureCollection.fromJson(loadJsonFromAsset(filename));
//        }catch (Exception e){
//            return null;
//        }
//        List<Feature> featureList = featureCollection.features();
//        List<PlaceSearch> level_1_places = new ArrayList<>();
//        for(int i=0; i<featureList.size(); i++){
//            Feature singleLocation = featureList.get(i);
//            if( singleLocation.hasProperty("name")){
//                String name = singleLocation.getStringProperty("name");
//                Double stringLng = ((Point)singleLocation.geometry()).coordinates().get(0);
//                Double stringLat = ((Point)singleLocation.geometry()).coordinates().get(1);
//                //                Log.d("MapActivity", "feature: " +name);
//                LatLng locationLatLng = new LatLng(stringLat, stringLng);
//                Log.d("SearchPlaces", locationLatLng.toString() + " at "+name);
//                level_1_places.add(new PlaceSearch(name, locationLatLng, level) );
//            }
//        }
//        return level_1_places;
//    }

    /*
     * Plots the shortest road from startingPoint to EndingPoint,
     * with junctions (waypoints) in between.
     * Uses Dijkstra algorithm.
     */
    public List<Node> getRoute(LatLng startingPoint, LatLng endingPoint){
        start = startingPoint;
        end = endingPoint;
        backtracking = new HashMap<>();
        distances = IntStream.range(0, adjacencyList.size())
                .boxed().map(x -> inf).collect(Collectors.toList());

        List<Integer> unvisitedJunctions = IntStream.range(0, adjacencyList.size())
                                            .boxed().collect(Collectors.toList());

        Integer nextJunction = nearestJunction(startingPoint);
        distances.set(nextJunction, calculate_distance(startingPoint, junctions.get(nextJunction)));
//        Log.d("Routing", "nearestj to start:"+nextJunction);
//        Log.d("Routing", "nearestj to end:"+nearestJunction(endingPoint));
        boolean destinationReached = false;
        while(!destinationReached){
            unvisitedJunctions.remove(nextJunction);
            if(!unvisitedJunctions.contains(nearestJunction(endingPoint))){
                destinationReached = true;
            }
            for(Pair<Integer,Double> neighborDist: adjacencyList.get(nextJunction)){
                Integer neighbor = neighborDist.first;
                Double toDistance = neighborDist.second;
                // if unvisited
                if(unvisitedJunctions.contains(neighbor) &&
                        toDistance + distances.get(nextJunction) < distances.get(neighbor)){

                    distances.set(neighbor, toDistance + distances.get(nextJunction));
                    // going from NextJunction to neighbor
                    backtracking.put(neighbor, nextJunction);
//                    Log.d("Routingbacktrack", "nei:"+neighbor+" nextj:"+nextJunction);

                }
            }
            // Select unvisited node with the least distance
            Integer potentialNJ = null;
            double minDist = inf;
            for(Integer v: unvisitedJunctions){
                if (distances.get(v) < minDist){
                    minDist = distances.get(v);
                    potentialNJ = v;
                }
            }
            if(potentialNJ!= null){         // no possible route found
                nextJunction = potentialNJ;
            } else{
                Log.d("Routing", "returning null");
                return null;
            }
        }

        /////
        Log.d("Routing", "End: somepoint");
        Integer curret = nearestJunction(endingPoint);
        while(curret!= null){
            Log.d("Routing", "-"+curret);
            curret = backtracking.get(curret);
        }
        /////
        List<Node> route = new ArrayList<>(); // start, junctions/waypoints, end

        // Backtracking the waypoints, starting from EndPoint
        route.add(0, new Node(endingPoint, 0.0));

        // IF nearest junction involves backtracking:
        Integer current = nearestJunction(endingPoint);
        Integer parentNode = backtracking.get(current);

//        // NO waypoints - from startpoint to endpoint
        if(getStartDist(current) > getStartToEndDist() ){
            Log.d("Routing", "No waypoints- start to end: "+ getStartToEndDist());
            route.add(0, new Node(startingPoint, getStartToEndBearing()));
            return route;
        }
        // Omit the last waypoint due to backtracking -  from 2nd last waypoint to endPoint
        else if(parentNode != null && getDistance(parentNode, current) > getEndDist(parentNode)) {
            current = parentNode;
            parentNode = backtracking.get(current);
        }

        route.add(0, new Node(junctions.get(current), getEndBearing(current)));
        Log.d("Routing", "add +"+current);
        while(true){
            Log.d("Routing", current+" || "+parentNode);
            if(parentNode == null || (backtracking.get(parentNode) == null && getStartDist(current) < getDistance(parentNode, current))){
                route.add(0, new Node(startingPoint, getStartBearing(current)));
//                Log.d("Routing", "add starting");
                return route;
            }
            else{
                route.add(0, new Node(junctions.get(parentNode), getBearing(parentNode, current)));
//                Log.d("Routing", "add +"+parentNode);
            }
            current = parentNode;
            parentNode = backtracking.get(current);
        }
    }


    /*
     * Finds the nearest junction to the point
     */
    private Integer nearestJunction(LatLng startingPoint){
        double shortestDist = 1000;
        Integer nearest = null;
        for(Integer junction: junctions.keySet()){

            if(calculate_distance(startingPoint, junctions.get(junction)) < shortestDist){
                shortestDist = calculate_distance(startingPoint, junctions.get(junction));
                nearest = junction;
            }
        }
        return nearest;
    }

    private void addToAdjacencyList(){
        adjacencyList = new ArrayList<>();
        try{
            DataInputStream textFileStream = new DataInputStream(mContext.getAssets().open(String.format("neighborsList.txt")));
            Scanner scanner = new Scanner(textFileStream);
            int i=0;
            while(scanner.hasNextLine()){
                String line = scanner.nextLine();
                String[] vertices = line.split(" ");
                List<Pair<Integer,Double>> neighbors = new ArrayList<>();
                for(String s: vertices){
                    Integer vertex = Integer.valueOf(s);
                    Log.d("ROuting", "vertex: "+i+" -> "+vertex);
                    neighbors.add(Pair.create(vertex, calculate_distance(junctions.get(i), junctions.get(vertex))));
                }
                adjacencyList.add(neighbors);
                i++;
            }
            scanner.close();

        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private void convertFeatures(){
        int level = 1;
        FeatureCollection featureCollection;
        try {
            String filename =  "com1floor"+level+".geojson";
            featureCollection = FeatureCollection.fromJson(loadJsonFromAsset(filename));
        }catch (Exception e){
            Log.d("RoutingActivity", "converting rooms failed");
            return;
        }
        List<Feature> featureList = featureCollection.features();

        for(int i=0; i<featureList.size(); i++) {
            Feature singleLocation = featureList.get(i);
            Double stringLng, stringLat;
            LatLng locationLatLng;

            if(singleLocation.hasProperty("name")){//door coordinates (can have > 1)
                stringLng = ((Point)singleLocation.geometry()).coordinates().get(0);
                stringLat = ((Point)singleLocation.geometry()).coordinates().get(1);
                locationLatLng = new LatLng(stringLat, stringLng);
                String title = singleLocation.getStringProperty("name");
                if(rooms.get(title) == null){
                    rooms.put(title, new ArrayList<>());
                }
                else{
                    rooms.get(title).add(locationLatLng);
                }
            }
            else if(singleLocation.hasProperty("junction")){
                stringLng = ((Point)singleLocation.geometry()).coordinates().get(0);
                stringLat = ((Point)singleLocation.geometry()).coordinates().get(1);
                locationLatLng = new LatLng(stringLat, stringLng);
                Integer vertex = singleLocation.getNumberProperty("junction").intValue();
                junctions.put(vertex, locationLatLng);
            }
        }
    }

    public Boolean withinPolygon(LatLng point){    //bbox:geojson
        if(TurfJoins.inside(Point.fromLngLat(point.getLongitude(),
                point.getLatitude()), Polygon.fromLngLats(boundingList))){
            return true;
        }
        else{
            return false;
        }
    }


    private void addLine(){

//        mapboxMap.addSource(indoorBuildingSource);
        loadPolyline();
    }

    private void loadPolyline(){
        LineLayer indoorBuildingLineLayer = new LineLayer("wayfinding","indoor-building")
                .withProperties(lineColor(Color.parseColor("#50667f")),
                        lineWidth(0.5f),
                        lineOpacity(interpolate(exponential(1f), zoom(),
                                stop(17f,1f),
                                stop(16.5f, 0.5f),
                                stop(16f,0f))));
//        map.addLayer(indoorBuildingLineLayer);
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


    /*
     * Calculates the bearing from point1 to point2 in clockwise direction
     */
    private Double calculate_bearing(LatLng point1, LatLng point2){
        // In radians
        Double a1 = Math.toRadians(point1.getLatitude());
        Double a2 = Math.toRadians(point2.getLatitude());
        Double b1 = Math.toRadians(point1.getLongitude());
        Double b2 = Math.toRadians(point2.getLongitude());

        Double y = Math.sin(b2-b1) * Math.cos(a2);
        Double x = Math.cos(a1)* Math.sin(a2) -
                Math.sin(a1) * Math.cos(a2) * Math.cos(b2-b1);
        return Math.toDegrees(Math.atan2(y,x));
    }

    /*
     * Calculate distance between 2 coordinates, in kilometres
     */
    private double calculate_distance(LatLng current, LatLng destination){
        int earthRadiusKm = 6371;
        double dLat = degreeToRadians(destination.getLatitude()-current.getLatitude());
        double dLng = degreeToRadians(destination.getLongitude()-current.getLongitude());

        double lat1 = degreeToRadians(current.getLatitude());
        double lat2 = degreeToRadians(destination.getLatitude());

        double a = Math.sin(dLat/2)*Math.sin(dLat/2)+
                Math.sin(dLng/2) * Math.sin(dLng/2) *Math.cos(lat1)*Math.cos(lat2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        return earthRadiusKm*c;
    }

    private double degreeToRadians(double degrees){
        return degrees*Math.PI/180;
    }

    private double getBearing(Integer parent, Integer child){
        return calculate_bearing(junctions.get(parent), junctions.get(child));
    }
    private double getStartBearing(Integer point){
        return calculate_bearing(start, junctions.get(point));
    }
    private double getEndBearing(Integer point){
        return calculate_bearing(junctions.get(point), end);
    }
    private double getStartToEndBearing(){
        return calculate_bearing(start, end);
    }
    private double getDistance(Integer parent, Integer child){
        return calculate_distance(junctions.get(parent), junctions.get(child));
    }
    private double getStartDist(Integer point){
        return calculate_distance(start, junctions.get(point));
    }
    private double getEndDist(Integer point){
        return calculate_distance(junctions.get(point), end);
    }
    private double getStartToEndDist(){
        return calculate_distance(start, end);
    }
}
