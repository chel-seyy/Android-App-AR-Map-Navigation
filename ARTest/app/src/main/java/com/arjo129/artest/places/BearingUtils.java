package com.arjo129.artest.places;

import com.mapbox.mapboxsdk.geometry.LatLng;

/**
 * Done by Chelsey
 */
public class BearingUtils {

    /*
     * Calculates the bearing from point1 to point2 in clockwise direction
     */
    public static Double calculate_bearing(LatLng point1, LatLng point2){
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
    public static double calculate_distance(LatLng current, LatLng destination){
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

    private static double degreeToRadians(double degrees){
        return degrees*Math.PI/180;
    }

}
