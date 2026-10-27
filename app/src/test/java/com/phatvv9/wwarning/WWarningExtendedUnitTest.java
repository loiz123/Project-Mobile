package com.phatvv9.wwarning;

import static org.junit.Assert.*;
import com.phatvv9.wwarning.util.GeoUtils;
import com.phatvv9.wwarning.util.ReputationPolicy;
import com.phatvv9.wwarning.data.local.ReportMapper;
import com.phatvv9.wwarning.data.local.ReportEntity;
import com.phatvv9.wwarning.model.Report;
import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;

public class WWarningExtendedUnitTest {
    @Test public void geo_samePointDistanceZero() { assertEquals(0, GeoUtils.getDistanceBetween(10.77,106.7,10.77,106.7),0.01); }
    @Test public void geo_distanceIsSymmetric() { double a=GeoUtils.getDistanceBetween(10.77,106.7,10.78,106.71); double b=GeoUtils.getDistanceBetween(10.78,106.71,10.77,106.7); assertEquals(a,b,0.01); }
    @Test public void geo_nearPointInside500Meters() { assertTrue(GeoUtils.getDistanceBetween(10.7769,106.7009,10.7771,106.7011)<500); }
    @Test public void geo_farPointOutside10Km() { assertTrue(GeoUtils.getDistanceBetween(10.7769,106.7009,10.95,106.9)>10000); }
    @Test public void geo_geohashIsDeterministic() { assertEquals(GeoUtils.getGeoHash(10.77,106.7),GeoUtils.getGeoHash(10.77,106.7)); }
    @Test public void geo_queryBoundsNotEmpty() { assertFalse(GeoUtils.getGeoHashQueryBounds(10.77,106.7,10000).isEmpty()); }
    @Test public void reputation_exactPolicyValues() { assertEquals(100,ReputationPolicy.INITIAL_SCORE); assertEquals(2,ReputationPolicy.POINTS_PER_VOTE); assertEquals(-10,ReputationPolicy.HIDDEN_PENALTY); }
    @Test public void mapper_nullAndEmptyLists() { assertNull(ReportMapper.toEntity(null)); assertNull(ReportMapper.toDomain(null)); assertTrue(ReportMapper.toEntityList(null).isEmpty()); assertTrue(ReportMapper.toDomainList(null).isEmpty()); }
    @Test public void mapper_roundTripPreservesFields() { Report r=new Report("user1","cat_ngap","sev_high",10.77,106.7,"abc",null); r.setId("r1"); r.setUpCount(3); r.setDownCount(1); ReportEntity e=ReportMapper.toEntity(r); Report back=ReportMapper.toDomain(e); assertEquals("r1",back.getId()); assertEquals("user1",back.getUserId()); assertEquals("cat_ngap",back.getCategoryId()); assertEquals(10.77,back.getLatitude(),0.000001); assertEquals(106.7,back.getLongitude(),0.000001); assertEquals(3,back.getUpCount()); assertEquals(1,back.getDownCount()); }
    @Test public void mapper_listSkipsNull() { Report r=new Report("u","cat_mua",null,10.7,106.7,"g",null); r.setId("x"); assertEquals(1,ReportMapper.toEntityList(Arrays.asList(null,r)).size()); assertEquals(0,ReportMapper.toDomainList(Collections.<ReportEntity>emptyList()).size()); }
}
