package com.phatvv9.wwarning.util;

import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQueryBounds;

import java.util.List;

public class GeoUtils {

    public static String getGeoHash(double latitude, double longitude) {
        return GeoFireUtils.getGeoHashForLocation(new GeoLocation(latitude, longitude));
    }

    public static List<GeoQueryBounds> getGeoHashQueryBounds(double latitude, double longitude, double radiusInMeters) {
        return GeoFireUtils.getGeoHashQueryBounds(new GeoLocation(latitude, longitude), radiusInMeters);
    }

    public static double getDistanceBetween(double lat1, double lng1, double lat2, double lng2) {
        return GeoFireUtils.getDistanceBetween(new GeoLocation(lat1, lng1), new GeoLocation(lat2, lng2));
    }
}
