package com.phatvv9.wwarning.util;

import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;

public class GeoUtils {
    public static String getGeoHash(double latitude, double longitude) {
        return GeoFireUtils.getGeoHashForLocation(new GeoLocation(latitude, longitude));
    }
}
