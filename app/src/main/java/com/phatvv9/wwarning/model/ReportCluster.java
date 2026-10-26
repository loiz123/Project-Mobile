package com.phatvv9.wwarning.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.phatvv9.wwarning.ui.map.MapFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Đại diện cho một cụm gom nhóm các báo cáo trong phạm vi bán kính 500m.
 */
public class ReportCluster {

    public static class CategoryStat {
        private final String categoryId;
        private int reportCount = 0;
        private int totalUpCount = 0;
        private int totalDownCount = 0;
        private Report topReport = null;
        private final List<Report> reports = new ArrayList<>();

        public CategoryStat(String categoryId) {
            this.categoryId = categoryId;
        }

        public void addReport(Report report) {
            reports.add(report);
            reportCount++;
            totalUpCount += report.getUpCount();
            totalDownCount += report.getDownCount();
            if (topReport == null || report.getUpCount() > topReport.getUpCount()) {
                topReport = report;
            }
        }

        public String getCategoryId() {
            return categoryId;
        }

        public int getReportCount() {
            return reportCount;
        }

        public int getTotalUpCount() {
            return totalUpCount;
        }

        public int getTotalDownCount() {
            return totalDownCount;
        }

        public Report getTopReport() {
            return topReport;
        }

        public List<Report> getReports() {
            return reports;
        }

        /** Điểm xếp hạng = số báo cáo (tự báo cáo) + số vote đúng (upvote). */
        public int getScore() {
            return reportCount + totalUpCount;
        }
    }

    private final List<Report> reports = new ArrayList<>();
    private double centerLat;
    private double centerLng;

    public ReportCluster(@NonNull Report initialReport) {
        this.centerLat = initialReport.getLatitude();
        this.centerLng = initialReport.getLongitude();
        addReport(initialReport);
    }

    public void addReport(@NonNull Report report) {
        reports.add(report);
    }

    public void removeReport(@NonNull String reportId) {
        for (int i = 0; i < reports.size(); i++) {
            if (reportId.equals(reports.get(i).getId())) {
                reports.remove(i);
                break;
            }
        }
    }

    public List<Report> getReports() {
        return reports;
    }

    public int getReportCount() {
        return reports.size();
    }

    public double getCenterLat() {
        return centerLat;
    }

    public double getCenterLng() {
        return centerLng;
    }

    /** Vị trí hiển thị marker cụm: trung bình toạ độ các báo cáo trong cụm. */
    public double getDisplayLat() {
        if (reports.isEmpty()) return centerLat;
        double sum = 0;
        for (Report r : reports) sum += r.getLatitude();
        return sum / reports.size();
    }

    public double getDisplayLng() {
        if (reports.isEmpty()) return centerLng;
        double sum = 0;
        for (Report r : reports) sum += r.getLongitude();
        return sum / reports.size();
    }

    /**
     * Nhóm các báo cáo theo categoryId và sắp xếp theo số vote đúng / số báo cáo nhiều nhất.
     */
    public List<CategoryStat> getCategoryStats() {
        Map<String, CategoryStat> map = new HashMap<>();
        for (Report report : reports) {
            String cat = report.getCategoryId() != null ? report.getCategoryId() : "other";
            CategoryStat stat = map.get(cat);
            if (stat == null) {
                stat = new CategoryStat(cat);
                map.put(cat, stat);
            }
            stat.addReport(report);
        }

        List<CategoryStat> list = new ArrayList<>(map.values());
        Collections.sort(list, new Comparator<CategoryStat>() {
            @Override
            public int compare(CategoryStat o1, CategoryStat o2) {
                if (o2.getScore() != o1.getScore()) {
                    return Integer.compare(o2.getScore(), o1.getScore());
                }
                if (o2.getTotalUpCount() != o1.getTotalUpCount()) {
                    return Integer.compare(o2.getTotalUpCount(), o1.getTotalUpCount());
                }
                return Integer.compare(o2.getReportCount(), o1.getReportCount());
            }
        });
        return list;
    }

    @Nullable
    public Report getPrimaryReport() {
        if (reports.isEmpty()) return null;
        List<CategoryStat> stats = getCategoryStats();
        if (!stats.isEmpty() && stats.get(0).getTopReport() != null) {
            return stats.get(0).getTopReport();
        }
        return reports.get(0);
    }

    public boolean hasRiskCategory() {
        for (Report r : reports) {
            if (MapFragment.isRiskCategory(r.getCategoryId())) {
                return true;
            }
        }
        return false;
    }

    public boolean isHighReputation() {
        for (Report r : reports) {
            if (r.getUpCount() >= 3) return true;
        }
        return false;
    }

    public int getTotalUpCount() {
        int sum = 0;
        for (Report r : reports) sum += r.getUpCount();
        return sum;
    }

    public int getTotalDownCount() {
        int sum = 0;
        for (Report r : reports) sum += r.getDownCount();
        return sum;
    }
}
