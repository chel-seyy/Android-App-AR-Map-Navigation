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
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.mapbox.mapboxsdk.style.expressions.Expression.exponential;
import static com.mapbox.mapboxsdk.style.expressions.Expression.interpolate;
import static com.mapbox.mapboxsdk.style.expressions.Expression.stop;
import static com.mapbox.mapboxsdk.style.expressions.Expression.zoom;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineOpacity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth;

/**
 * Done by Chelsey
 */
public class Routing extends AppCompatActivity {

    private Double inf = Double.POSITIVE_INFINITY;  // change to long? if dist gets Large...
    private static final Double bearing = 41.3;
    private static final int current_level = 1;
    private static final String TAG = "Routing";
    private Context mContext;
//    private HashMap<String, List<LatLng>> rooms;
    private HashMap<Integer, HashMap<Integer, LatLng>> levelJunctions;
    private HashMap<Integer, LatLng> junctions;
    private HashMap<Integer, List<List<Pair<Integer, Double>>>> floorAdjacencyList;
    private HashMap<Integer, Integer> backtracking;
    private List<Double> distances;
    private HashMap<Integer, HashMap<Integer, Pair<Connector, LatLng>>> connectorLevelsCoords;
    private LatLng start, end;
    private int floor = -1;
    List<List<Point>> boundingList;

    public Routing(Context context){
        mContext = context;
        int floors = 3;
        levelJunctions = new HashMap<>();
        connectorLevelsCoords = new HashMap<>();
        floorAdjacencyList = new HashMap<>();
        for(int i=0; i < floors; i++){
            filterFeatures(i);
            addToAdjacencyList(i);
        }
        boundingList = new ArrayList<>();
        List<Point> perimeter = Arrays.asList(Point.fromLngLat(103.773678, 1.295476),
                Point.fromLngLat(103.7740141, 1.2950704),
                Point.fromLngLat(103.7743153, 1.294700),
                Point.fromLngLat(103.7741678, 1.29457401),
                Point.fromLngLat(103.7740424, 1.29448216),
                Point.fromLngLat(103.7739611, 1.29458082),
                Point.fromLngLat(103.7739135, 1.29454267),
                Point.fromLngLat(103.7737461,1.294742746),
                Point.fromLngLat(103.7735135, 1.29502694),
                Point.fromLngLat(103.7736895, 1.29518788),
                Point.fromLngLat(103.7735521, 1.29534816),

                Point.fromLngLat(103.773678, 1.295476)); // TODO
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
    public List<Node> getRouteWithinLevel(LatLng startingPoint, LatLng endingPoint){
        start = startingPoint;
        end = endingPoint;
        floor = (int)startingPoint.getAltitude();
        junctions = levelJunctions.get(floor);
        backtracking = new HashMap<>();
        List<List<Pair<Integer, Double>>> adjacencyList = floorAdjacencyList.get(floor);
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
                break;
            }
//            Log.d("Routing", "Vertex: "+nextJunction);
            for(Pair<Integer,Double> neighborDist: adjacencyList.get(nextJunction)){
                Integer neighbor = neighborDist.first;
                Double toDistance = neighborDist.second;
                // if unvisited
                if(unvisitedJunctions.contains(neighbor) &&
                        toDistance + distances.get(nextJunction) < distances.get(neighbor)){

                    distances.set(neighbor, toDistance + distances.get(nextJunction));
                    // going from NextJunction to neighbor
                    backtracking.put(neighbor, nextJunction);
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
            if(potentialNJ != null){         // no possible route found
                nextJunction = potentialNJ;
            } else{
                Log.d("Routing", "returning null");
                return null;
            }
        }


        List<Node> route = new ArrayList<>(); // start, junctions/waypoints, end

        // Backtracking the waypoints, starting from EndPoint
        route.add(0, new Node(endingPoint, 0.0));

        // IF nearest junction involves backtracking:
        Integer current = nearestJunction(endingPoint);
        Integer parentNode = backtracking.get(current);

        // No waypoint needed

        if(getStartDist(nearestJunction(startingPoint)) > getStartToEndDist() ||
                getEndDist(nearestJunction(endingPoint)) > getStartToEndDist()){
            Log.d("Routing", "No waypoint - start to end: "+ getStartToEndDist());
            route.add(0, new Node(startingPoint, getStartToEndBearing()));
            return route;
        }
        // Omit the last waypoint -> from 2nd last waypoint to endPoint
        else if(parentNode != null && getDistance(parentNode, current) > getEndDist(parentNode)) {
            Log.d("Routing", "Omitting the last waypoint");
            current = parentNode;
            parentNode = backtracking.get(current);
        }

        route.add(0, new Node(junctions.get(current), getEndBearing(current)));

        while(true){
            if(parentNode == null || (backtracking.get(parentNode) == null && getStartDist(current) < getDistance(parentNode, current))){
                route.add(0, new Node(startingPoint, getStartBearing(current)));
                Log.d("Routing", "add starting");
                return route;
            }
            else{
                route.add(0, new Node(junctions.get(parentNode), getBearing(parentNode, current)));
            }
            current = parentNode;
            parentNode = backtracking.get(current);
        }
    }

    /*
        Only connectors about to be entered are marked with directions
        Leaving connectors are not marked.
     */
    public HashMap<Integer, List<Node>> getRoute(LatLng start, LatLng end) {
        HashMap<Integer, List<Node>> route = new HashMap<>();
        int startFloor = (int)start.getAltitude();
        int endFloor = (int)end.getAltitude();
        boolean goingUp = endFloor - startFloor > 0;

        if (startFloor == endFloor) {
            route.put(startFloor, getRouteWithinLevel(start, end));
        } else if (Math.abs(endFloor - startFloor) == 1 ) {
            int connector = nearestConnector(start, endFloor);
            Pair<Connector, LatLng> fromConnector = connectorLevelsCoords.get(startFloor).get(connector);
            Pair<Connector, LatLng> toConnector = connectorLevelsCoords.get(endFloor).get(connector);

            List<Node> startRoute = getRouteWithinLevel(start, fromConnector.second);
            Log.d("MapActivity1Routing", "connector: " + fromConnector.first.toString());
            startRoute.get(startRoute.size() - 1).setConnector(fromConnector.first, goingUp);
            Log.d("MapActivity1Routing", startRoute.get(startRoute.size() - 1).toString());

            List<Node> endRoute = getRouteWithinLevel(toConnector.second, end);
//            endRoute.get(0).setConnector(toConnector.first, goingUp);
            route.put(startFloor, startRoute);
            route.put(endFloor, endRoute);
        } else {        // From Basement to Level 2
            int interFloor = 1;
            int connector_1 = nearestConnector(start, interFloor);
            Pair<Connector, LatLng>  basementConnector = connectorLevelsCoords.get(startFloor).get(connector_1);
            List<Node> startRoute = getRouteWithinLevel(start, basementConnector.second);
            startRoute.get(startRoute.size() - 1).setConnector(basementConnector.first, goingUp);

            Pair<Connector, LatLng> firstConnector1 = connectorLevelsCoords.get(interFloor).get(connector_1);
            int connector_2 = nearestConnector(firstConnector1.second, endFloor);
            Pair<Connector, LatLng> firstConnector2 = connectorLevelsCoords.get(interFloor).get(connector_2);
            List<Node> interRoute = getRouteWithinLevel(firstConnector1.second, firstConnector2.second);
//            interRoute.get(0).setConnector(firstConnector1.first, goingUp);
            interRoute.get(interRoute.size() - 1).setConnector(firstConnector2.first, goingUp);

            Pair<Connector, LatLng> secondConnector = connectorLevelsCoords.get(endFloor).get(connector_2);
            List<Node> endRoute = getRouteWithinLevel(secondConnector.second, end);
//            endRoute.get(0).setConnector(secondConnector.first, goingUp);

            route.put(startFloor, startRoute);
            route.put(interFloor, interRoute);
            route.put(endFloor, endRoute);
        }
        return route;
    }

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

    private Integer nearestConnector(LatLng start, int endFloor) {
        int startFloor = (int) start.getAltitude();
//        int endFloor = (int) end.getAltitude();
        Set<Integer> fromConnectors = (connectorLevelsCoords.get(startFloor)).keySet();
        Set<Integer> toConnectors = (connectorLevelsCoords.get(endFloor)).keySet();
        List<Integer> connectors = fromConnectors.stream()
                                    .filter(x -> toConnectors.contains(x))
                                    .collect(Collectors.toList());

        if (connectors.isEmpty()) {
            return null;
        }

        double shortestDistance = 1000;
        Integer nearestConnector = null;
        for(Integer type: connectors){
            LatLng connector = connectorLevelsCoords.get(startFloor).get(type).second;
            if (calculate_distance(connector, start) < shortestDistance) {
                shortestDistance = calculate_distance(connector, start);
                nearestConnector = type;
            }
        }
        return nearestConnector;
    }

    private void addToAdjacencyList(int level) {
//        Log.d("Routing", "adding to list-> level: "+level);
        List<List<Pair<Integer, Double>>> adjacencyList = new ArrayList<>();
        try{
            DataInputStream textFileStream = new DataInputStream(mContext.getAssets().open(String.format("floor"+level+".txt")));
            Scanner scanner = new Scanner(textFileStream);
            int i=0;
            while(scanner.hasNextLine()){
                String line = scanner.nextLine();
                String[] vertices = line.split(" ");
                List<Pair<Integer,Double>> neighbors = new ArrayList<>();
                for(String s: vertices){
                    Integer vertex = Integer.valueOf(s);
//                    Log.d("ROuting", "vertex: "+i+" -> "+vertex);

                    neighbors.add(Pair.create(vertex, calculate_distance(levelJunctions.get(level).get(i),
                            levelJunctions.get(level).get(vertex))));
                }
                adjacencyList.add(neighbors);
                i++;
            }
            scanner.close();
            floorAdjacencyList.put(level, adjacencyList);
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private void filterFeatures(int level){
        HashMap<Integer, LatLng> junctions = new HashMap<>();
        HashMap<Integer, Pair<Connector, LatLng>> connectors = new HashMap<>();
        FeatureCollection featureCollection;
        try {
            String filename = "com1floor" + level + ".geojson";
            featureCollection = FeatureCollection.fromJson(loadJsonFromAsset(filename));
        } catch (Exception e) {
            Log.d("MapActivity", "converting failed");
            return;
        }
        List<Feature> featureList = featureCollection.features();

        for (int i = 0; i < featureList.size(); i++) {
            Feature singleLocation = featureList.get(i);
//            if(key.equals("toilet") && singleLocation.hasProperty(key)){
//                featuresRequired.add(singleLocation);
//            }
//            if((key.equals("stair") && singleLocation.hasProperty("connector")) ||
//                    (key.equals("lift") && singleLocation.hasProperty("connector"))){
//                String title = singleLocation.getStringProperty("connector");
//                if(title.toLowerCase().contains(key)){
//                    featuresRequired.add(singleLocation);
//                }
//            }
            if (singleLocation.hasProperty("connector") && singleLocation.hasProperty("type")) {
                Double stringLng = ((Point)singleLocation.geometry()).coordinates().get(0);
                Double stringLat = ((Point)singleLocation.geometry()).coordinates().get(1);
                LatLng locationLatLng = new LatLng(stringLat, stringLng);
                locationLatLng.setAltitude(level);
                Integer type = singleLocation.getNumberProperty("type").intValue();
                if (singleLocation.getStringProperty("connector").toLowerCase().contains("stairs")) {
                    connectors.put(type, Pair.create(Connector.Stairs, locationLatLng));
                } else {
                    connectors.put(type, Pair.create(Connector.Lift, locationLatLng));
                    assert singleLocation.getStringProperty("connector").contains("lift") : "Not a lift actually";
                }
//                Log.d("MapActivity1Routing", "Lvl " + level + " Creating connector: " + connectors.get(type).first);
            }
            if(singleLocation.hasProperty("junction")){
                Double stringLng = ((Point)singleLocation.geometry()).coordinates().get(0);
                Double stringLat = ((Point)singleLocation.geometry()).coordinates().get(1);
                LatLng locationLatLng = new LatLng(stringLat, stringLng);
                locationLatLng.setAltitude(level);
                Integer vertex = singleLocation.getNumberProperty("junction").intValue();
                junctions.put(vertex, locationLatLng);
            }
        }
        levelJunctions.put(level, junctions);
        connectorLevelsCoords.put(level, connectors);
        return;
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
