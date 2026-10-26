package com.phatvv9.wwarning.data.local;

import com.google.firebase.Timestamp;
import com.phatvv9.wwarning.model.Report;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ReportMapper {

    public static ReportEntity toEntity(Report report) {
        if (report == null) return null;

        String id = report.getId() != null ? report.getId() : "";
        long createdAtMs = report.getCreatedAt() != null ? report.getCreatedAt().toDate().getTime() : System.currentTimeMillis();
        long expiresAtMs = report.getExpiresAt() != null ? report.getExpiresAt().toDate().getTime() : System.currentTimeMillis() + 3600000L;

        return new ReportEntity(
                id,
                report.getUserId(),
                report.getCategoryId(),
                report.getSeverityId(),
                report.getLatitude(),
                report.getLongitude(),
                report.getGeohash(),
                report.getStatus(),
                report.getUpCount(),
                report.getDownCount(),
                createdAtMs,
                expiresAtMs
        );
    }

    public static Report toDomain(ReportEntity entity) {
        if (entity == null) return null;

        Report report = new Report();
        report.setId(entity.getId());
        report.setUserId(entity.getUserId());
        report.setCategoryId(entity.getCategoryId());
        report.setSeverityId(entity.getSeverityId());
        report.setLatitude(entity.getLatitude());
        report.setLongitude(entity.getLongitude());
        report.setGeohash(entity.getGeohash());
        report.setStatus(entity.getStatus());
        report.setUpCount(entity.getUpCount());
        report.setDownCount(entity.getDownCount());
        report.setCreatedAt(new Timestamp(new Date(entity.getCreatedAt())));
        report.setExpiresAt(new Timestamp(new Date(entity.getExpiresAt())));

        return report;
    }

    public static List<ReportEntity> toEntityList(List<Report> reports) {
        List<ReportEntity> entities = new ArrayList<>();
        if (reports == null) return entities;
        for (Report report : reports) {
            ReportEntity entity = toEntity(report);
            if (entity != null) {
                entities.add(entity);
            }
        }
        return entities;
    }

    public static List<Report> toDomainList(List<ReportEntity> entities) {
        List<Report> reports = new ArrayList<>();
        if (entities == null) return reports;
        for (ReportEntity entity : entities) {
            Report report = toDomain(entity);
            if (report != null) {
                reports.add(report);
            }
        }
        return reports;
    }
}
