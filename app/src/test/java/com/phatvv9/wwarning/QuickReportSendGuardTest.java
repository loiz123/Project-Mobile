package com.phatvv9.wwarning;

import static org.junit.Assert.*;
import com.phatvv9.wwarning.notification.QuickReportSendGuard;
import org.junit.Test;

public class QuickReportSendGuardTest {
    @Test public void locationAtFiveMinutes_isFresh_beyondIsStale() {
        long now = 1_000_000;
        assertTrue(QuickReportSendGuard.isLocationFresh(now - 300_000, now));
        assertFalse(QuickReportSendGuard.isLocationFresh(now - 300_001, now));
    }
    @Test public void unknownOrFutureLocation_isRejected() {
        assertFalse(QuickReportSendGuard.isLocationFresh(0, 1_000_000));
        assertFalse(QuickReportSendGuard.isLocationFresh(1_000_001, 1_000_000));
    }
    @Test public void repeatedTaps_areBlockedUntilCallback() {
        QuickReportSendGuard guard = new QuickReportSendGuard();
        assertTrue(guard.tryStart(1_000_000, 0));
        assertFalse(guard.tryStart(2_000_000, 0));
        guard.finish();
        assertTrue(guard.tryStart(2_000_000, 0));
    }
    @Test public void successCooldown_excludesThirtySecondsBoundary() {
        QuickReportSendGuard guard = new QuickReportSendGuard();
        assertFalse(guard.tryStart(1_029_999, 1_000_000));
        assertTrue(guard.tryStart(1_030_000, 1_000_000));
    }
    @Test public void persistedSuccess_blocksNewGuardAfterProcessRestart() {
        assertFalse(new QuickReportSendGuard().tryStart(1_010_000, 1_000_000));
    }
    @Test public void failedSend_canRetryImmediately() {
        QuickReportSendGuard guard = new QuickReportSendGuard();
        assertTrue(guard.tryStart(1_000_000, 0));
        guard.finish();
        assertFalse(guard.isSending());
        assertTrue(guard.tryStart(1_000_001, 0));
    }
    @Test public void clockMovedBack_doesNotBypassCooldown() {
        assertFalse(new QuickReportSendGuard().tryStart(999_999, 1_000_000));
    }
}
