package com.phatvv9.wwarning.notification;

/** Local notification safeguards; does not replace server-side rate limits. */
public final class QuickReportSendGuard {
    public static final long COOLDOWN_MS = 30_000L;
    public static final long MAX_LOCATION_AGE_MS = 5 * 60_000L;
    private boolean sending;

    public static boolean isLocationFresh(long measuredAt, long now) {
        return measuredAt > 0 && measuredAt <= now && now - measuredAt <= MAX_LOCATION_AGE_MS;
    }

    public synchronized boolean tryStart(long now, long lastSuccess) {
        if (sending) return false;
        if (lastSuccess > 0 && (now < lastSuccess || now - lastSuccess < COOLDOWN_MS)) {
            return false;
        }
        sending = true;
        return true;
    }

    public synchronized boolean isSending() { return sending; }

    public synchronized void finish() { sending = false; }
}
