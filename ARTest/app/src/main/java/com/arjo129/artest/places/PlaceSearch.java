package com.arjo129.artest.places;

import com.mapbox.mapboxsdk.geometry.LatLng;

public class PlaceSearch {
    LatLng coordinate;
    String place_name;
    int level;

    PlaceSearch(String name, LatLng coordinate, int level){
        this.place_name = name;
        this.level = level;
        this.coordinate = coordinate;
    }
}
