package com.arjo129.artest.places;

import com.mapbox.mapboxsdk.geometry.LatLng;

/**
 * Done by Chelsey
 */
public class PlaceSearch {
    LatLng coordinate;
    String place_name;
    int level;

    PlaceSearch(String name, LatLng coordinate, int level){
        this.place_name = name;
        this.level = level;
        this.coordinate = coordinate;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof PlaceSearch) {
            PlaceSearch anotherPlace = (PlaceSearch) obj;
            if (anotherPlace.place_name.equals(this.place_name)
                    && anotherPlace.level == this.level) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        String placeLevel= place_name + level;
        return placeLevel.hashCode();
    }

    @Override
    public String toString() {
        return "Level: " + level + " - " + place_name;
    }
}
