package com.phatvv9.wwarning;

import static org.junit.Assert.*;
import com.google.firebase.Timestamp;
import com.phatvv9.wwarning.data.local.ReportEntity;
import com.phatvv9.wwarning.data.local.ReportMapper;
import com.phatvv9.wwarning.model.Report;
import com.phatvv9.wwarning.model.UserProfile;
import com.phatvv9.wwarning.util.ReputationPolicy;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.junit.Test;

public class ReportMapperContractTest {
    @Test public void newProfiles_matchInitialReputationPolicy() {
        assertEquals(ReputationPolicy.INITIAL_SCORE,
                new UserProfile("Guest", "Guest").getReputationScore());
        assertEquals(ReputationPolicy.INITIAL_SCORE,
                new UserProfile("Member", "Google").getReputationScore());
        assertEquals(2, ReputationPolicy.POINTS_PER_VOTE);
        assertEquals(-10, ReputationPolicy.HIDDEN_PENALTY);
    }

    @Test public void roundTrip_preservesEveryPersistedFieldAndMillisecondDates() {
        Report source = new Report("author", "cat_bao", "high", -12.5, 179.9,
                "hash", new Timestamp(new Date(1800000123456L)));
        source.setId("report");
        source.setStatus("Hidden");
        source.setUpCount(7);
        source.setDownCount(4);
        source.setCreatedAt(new Timestamp(new Date(1700000123456L)));
        Report result = ReportMapper.toDomain(ReportMapper.toEntity(source));
        assertEquals(source.getId(), result.getId());
        assertEquals(source.getUserId(), result.getUserId());
        assertEquals(source.getCategoryId(), result.getCategoryId());
        assertEquals(source.getSeverityId(), result.getSeverityId());
        assertEquals(source.getGeohash(), result.getGeohash());
        assertEquals(source.getStatus(), result.getStatus());
        assertEquals(source.getLatitude(), result.getLatitude(), 0);
        assertEquals(source.getLongitude(), result.getLongitude(), 0);
        assertEquals(source.getUpCount(), result.getUpCount());
        assertEquals(source.getDownCount(), result.getDownCount());
        assertEquals(source.getCreatedAt(), result.getCreatedAt());
        assertEquals(source.getExpiresAt(), result.getExpiresAt());
    }

    @Test public void missingDates_useBoundedClockDefaultsAndEmptyId() {
        long before = System.currentTimeMillis();
        ReportEntity entity = ReportMapper.toEntity(new Report());
        long after = System.currentTimeMillis();
        assertEquals("", entity.getId());
        assertTrue(entity.getCreatedAt() >= before && entity.getCreatedAt() <= after);
        assertTrue(entity.getExpiresAt() >= before + 3600000L);
        assertTrue(entity.getExpiresAt() <= after + 3600000L);
        assertNull(entity.getSeverityId());
    }

    @Test public void domainList_skipsNullAndPreservesOrder() {
        ReportEntity first = ReportMapper.toEntity(new Report());
        first.setId("first");
        ReportEntity second = ReportMapper.toEntity(new Report());
        second.setId("second");
        List<Report> result = ReportMapper.toDomainList(Arrays.asList(first, null, second));
        assertEquals(2, result.size());
        assertEquals("first", result.get(0).getId());
        assertEquals("second", result.get(1).getId());
    }
}
