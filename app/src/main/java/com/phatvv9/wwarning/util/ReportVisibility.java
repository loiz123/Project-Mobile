package com.phatvv9.wwarning.util;

import com.phatvv9.wwarning.model.Report;

/** Shared lifetime rules for the map, offline cache and administration UI. */
public final class ReportVisibility {
    public enum State { ACTIVE, HIDDEN, DELETED, EXPIRED }

    private ReportVisibility() {}

    public static boolean hasValidExpiry(Report report, long now) {
        return report.getExpiresAt() != null
                && report.getExpiresAt().toDate().getTime() > now;
    }

    public static boolean isVisible(Report report, long now) {
        return FirestoreNames.ReportStatus.ACTIVE.equals(report.getStatus())
                && hasValidExpiry(report, now);
    }

    public static State stateOf(Report report, long now) {
        if (FirestoreNames.ReportStatus.DELETED.equals(report.getStatus())) return State.DELETED;
        if (FirestoreNames.ReportStatus.HIDDEN.equals(report.getStatus())) return State.HIDDEN;
        return isVisible(report, now) ? State.ACTIVE : State.EXPIRED;
    }

    /** -1 means there is no future expiry to watch. Recheck at least once a minute. */
    public static long nextRefreshDelayMillis(Iterable<Report> reports, long now) {
        long delay = Long.MAX_VALUE;
        for (Report report : reports) {
            if (hasValidExpiry(report, now)) {
                delay = Math.min(delay, report.getExpiresAt().toDate().getTime() - now);
            }
        }
        return delay == Long.MAX_VALUE ? -1 : Math.min(delay, 60000L);
    }
}
