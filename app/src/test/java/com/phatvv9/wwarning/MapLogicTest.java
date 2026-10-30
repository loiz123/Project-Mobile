package com.phatvv9.wwarning;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.phatvv9.wwarning.model.Report;
import com.phatvv9.wwarning.model.ReportCluster;
import com.phatvv9.wwarning.ui.map.MapFragment;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/** Kiểm thử logic thuần (không cần thiết bị): phân loại rủi ro, gom cụm 500m, uy tín. */
public class MapLogicTest {

    private static Report report(String id, String category, double lat, double lng, int up) {
        Report r = new Report("u_" + id, category, null, lat, lng, "g", null);
        r.setId(id);
        r.setUpCount(up);
        return r;
    }

    // ---- Risk filter ----

    @Test
    public void riskCategories_areFloodAndStorm() {
        assertTrue(MapFragment.isRiskCategory("cat_ngap"));
        assertTrue(MapFragment.isRiskCategory("cat_bao"));
        assertTrue(MapFragment.isRiskCategory("Flood"));
    }

    @Test
    public void normalCategories_areNotRisk() {
        assertFalse(MapFragment.isRiskCategory("cat_nang"));
        assertFalse(MapFragment.isRiskCategory("cat_mua"));
        assertFalse(MapFragment.isRiskCategory("cat_suong"));
        assertFalse(MapFragment.isRiskCategory("cat_am_u"));
        assertFalse(MapFragment.isRiskCategory(null));
    }

    @Test
    public void displayName_mapsKnownAndFallsBack() {
        assertEquals("Ngập lụt", MapFragment.getCategoryDisplayName("cat_ngap"));
        assertEquals("Mưa", MapFragment.getCategoryDisplayName("Rain"));
        assertEquals("Âm u", MapFragment.getCategoryDisplayName("cat_am_u"));
        assertEquals("Cảnh báo thời tiết", MapFragment.getCategoryDisplayName(null));
        assertEquals("abc", MapFragment.getCategoryDisplayName("abc"));
    }

    // ---- Gom cụm ----

    @Test
    public void cluster_emptyInput_returnsEmpty() {
        assertTrue(MapFragment.clusterReports(new ArrayList<Report>(), 500).isEmpty());
    }

    @Test
    public void cluster_nearReportsMerge_farReportsSeparate() {
        List<Report> list = new ArrayList<>();
        list.add(report("a", "cat_mua", 10.7769, 106.7009, 0));
        list.add(report("b", "cat_ngap", 10.7771, 106.7011, 0)); // ~30m
        list.add(report("c", "cat_nang", 10.8500, 106.8000, 0)); // vài km

        List<ReportCluster> clusters = MapFragment.clusterReports(list, 500);

        assertEquals(2, clusters.size());
        int total = 0;
        for (ReportCluster c : clusters) total += c.getReportCount();
        assertEquals(3, total);
    }

    @Test
    public void cluster_withRiskReport_flagsRisk() {
        List<Report> list = new ArrayList<>();
        list.add(report("a", "cat_mua", 10.7769, 106.7009, 0));
        list.add(report("b", "cat_ngap", 10.7770, 106.7010, 0));

        ReportCluster cluster = MapFragment.clusterReports(list, 500).get(0);
        assertTrue(cluster.hasRiskCategory());
    }

    // ---- Uy tín ----

    @Test
    public void highReputation_atThreeUpvotes() {
        ReportCluster low = new ReportCluster(report("a", "cat_mua", 10.77, 106.70, 2));
        ReportCluster high = new ReportCluster(report("b", "cat_mua", 10.77, 106.70, 3));
        assertFalse(low.isHighReputation());
        assertTrue(high.isHighReputation());
    }

    @Test
    public void categoryStats_sortedByScoreDescending() {
        ReportCluster cluster = new ReportCluster(report("a", "cat_mua", 10.77, 106.70, 0));
        cluster.addReport(report("b", "cat_ngap", 10.77, 106.70, 5));

        // ngập: 1 báo cáo + 5 up = 6 điểm > mưa: 1 điểm
        assertEquals("cat_ngap", cluster.getCategoryStats().get(0).getCategoryId());
    }
}
