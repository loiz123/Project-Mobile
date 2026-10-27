package com.phatvv9.wwarning;

import static org.junit.Assert.*;

import com.firebase.geofire.GeoQueryBounds;
import com.phatvv9.wwarning.model.Report;
import com.phatvv9.wwarning.model.ReportCluster;
import com.phatvv9.wwarning.ui.map.MapFragment;
import com.phatvv9.wwarning.util.GeoUtils;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class GeoClusterBoundaryTest {
    private Report at(String id, double longitude, int votes) {
        Report r = new Report("user", "cat_mua", null, 0, longitude,
                GeoUtils.getGeoHash(0, longitude), null);
        r.setId(id);
        r.setUpCount(votes);
        return r;
    }

    @Test public void geohash_matchesPublishedReferenceCoordinate() {
        assertTrue(GeoUtils.getGeoHash(42.6, -5.6).startsWith("ezs42"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void geohash_rejectsLatitudeOutsideEarth() {
        GeoUtils.getGeoHash(91, 0);
    }

    @Test public void tenKm_distanceSeparatesPointsOnEitherSide() {
        assertTrue(GeoUtils.getDistanceBetween(0, 0, 0, 0.089) < 10000);
        assertTrue(GeoUtils.getDistanceBetween(0, 0, 0, 0.091) > 10000);
    }

    // Bounds are only a candidate query; repository must still filter by distance.
    @Test public void tenKm_queryBoundsContainNearbyGeohashes() {
        List<GeoQueryBounds> bounds = GeoUtils.getGeoHashQueryBounds(0, 0, 10000);
        for (double longitude : new double[]{-0.089, 0, 0.089}) {
            String hash = GeoUtils.getGeoHash(0, longitude);
            boolean covered = false;
            for (GeoQueryBounds bound : bounds) {
                covered |= hash.compareTo(bound.startHash) >= 0
                        && hash.compareTo(bound.endHash) <= 0;
            }
            assertTrue("Missing longitude " + longitude, covered);
        }
    }

    @Test public void clustering_uses500MetersOnEitherSide() {
        assertEquals(500, MapFragment.CLUSTER_RADIUS_METERS, 0);
        assertEquals(1, MapFragment.clusterReports(
                Arrays.asList(at("a", 0, 3), at("b", 0.0044, 0)), 500).size());
        assertEquals(2, MapFragment.clusterReports(
                Arrays.asList(at("a", 0, 3), at("b", 0.0046, 0)), 500).size());
    }

    @Test public void clustering_includesExactComputedBoundary() {
        Report a = at("a", 0, 3), b = at("b", 0.0045, 0);
        double distance = GeoUtils.getDistanceBetween(0, 0, 0, 0.0045);
        assertEquals(1, MapFragment.clusterReports(Arrays.asList(a, b), distance).size());
        assertEquals(2, MapFragment.clusterReports(Arrays.asList(a, b), distance - 0.01).size());
    }

    @Test public void clustering_prioritizesVotesWithoutMutatingInputOrder() {
        Report low = at("low", 0, 0), high = at("high", 0.001, 4);
        List<Report> input = Arrays.asList(low, high);
        ReportCluster cluster = MapFragment.clusterReports(input, 500).get(0);
        assertEquals(high.getLongitude(), cluster.getCenterLng(), 0);
        assertSame(low, input.get(0));
        assertSame(high, cluster.getPrimaryReport());
        assertEquals(0.0005, cluster.getDisplayLng(), 0.0000001);
    }
}
