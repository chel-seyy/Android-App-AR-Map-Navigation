package com.arjo129.artest;

import com.mapbox.mapboxsdk.geometry.LatLng;

public class Node {
    LatLng coordinate;
    Double bearing;     // Difference in bearing to the next node
//    boolean startPoint;
//    boolean endPoint;

    public Node(LatLng coordinate, Double bearing){
//        this.startPoint = false;
//        this.endPoint = false;
        this.coordinate = coordinate;
        this.bearing = bearing;
    }
//    public Node(LatLng coordinate, boolean startPoint, boolean endPoint){
//        this.coordinate = coordinate;
//        this.bearing = 0.0;
//        this.startPoint = startPoint;
//        this.endPoint = endPoint;
//    }
}
