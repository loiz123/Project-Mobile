package com.phatvv9.wwarning.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.phatvv9.wwarning.data.remote.ReportRepository;
import com.phatvv9.wwarning.worker.ReminderScheduler;

/** Sends a report directly from the notification; deliberately does not request/capture an image. */
public class QuickReportActionReceiver extends BroadcastReceiver {
    private static final String PREFS = "wwarning_quick_report_location";
    public static final String KEY_LAT = "lat";
    public static final String KEY_LNG = "lng";
    public static final String KEY_HAS_LOCATION = "has_location";
    public static final String KEY_LOCATION_TIME = "location_time";
    private static final String KEY_LAST_SUCCESS = "last_report_success";
    private static final QuickReportSendGuard SEND_GUARD = new QuickReportSendGuard();

    @Override public void onReceive(Context context, Intent intent) {
        if (intent == null || !QuickReportNotification.ACTION_REPORT.equals(intent.getAction())) return;
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(context, "Vui lòng đăng nhập WWarning trước khi gửi báo cáo.", Toast.LENGTH_LONG).show();
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(KEY_HAS_LOCATION, false)) {
            Toast.makeText(context, "Chưa có vị trí. Mở WWarning một lần để ghi nhận GPS rồi thử lại.", Toast.LENGTH_LONG).show();
            return;
        }
        long now = System.currentTimeMillis();
        if (!QuickReportSendGuard.isLocationFresh(prefs.getLong(KEY_LOCATION_TIME, 0L), now)) {
            String message = "Vị trí GPS đã cũ. Mở WWarning để cập nhật vị trí trước khi gửi.";
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            QuickReportNotification.show(context, message);
            return;
        }
        if (!SEND_GUARD.tryStart(now, prefs.getLong(KEY_LAST_SUCCESS, 0L))) {
            Toast.makeText(context, SEND_GUARD.isSending()
                    ? "Báo cáo đang chờ gửi. Vui lòng không bấm lại."
                    : "Bạn vừa gửi báo cáo. Vui lòng đợi 30 giây rồi thử lại.",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        double lat = Double.longBitsToDouble(prefs.getLong(KEY_LAT, 0L));
        double lng = Double.longBitsToDouble(prefs.getLong(KEY_LNG, 0L));
        String categoryId = intent.getStringExtra(QuickReportNotification.EXTRA_CATEGORY_ID);
        String categoryName = intent.getStringExtra(QuickReportNotification.EXTRA_CATEGORY_NAME);
        if (categoryId == null) categoryId = "cat_mua";
        final String finalName = categoryName == null ? "thời tiết" : categoryName;
        Toast.makeText(context, "Đang gửi báo cáo " + finalName + "…", Toast.LENGTH_SHORT).show();
        try {
            ReportRepository.getInstance(context).createReport(categoryId, lat, lng, new ReportRepository.ReportCallback() {
                @Override public void onSuccess(@NonNull String reportId) {
                    prefs.edit().putLong(KEY_LAST_SUCCESS, System.currentTimeMillis()).apply();
                    SEND_GUARD.finish();
                    // Gửi từ notification cũng phải có chu kỳ nhắc 15 phút như Quick Report trên map.
                    ReminderScheduler.schedule(context.getApplicationContext(), reportId);
                    QuickReportNotification.dismiss(context);
                    Toast.makeText(context, "Đã gửi báo cáo " + finalName + " thành công.",
                            Toast.LENGTH_SHORT).show();
                }
                @Override public void onError(@NonNull Exception e) {
                    SEND_GUARD.finish();
                    QuickReportNotification.show(context, "Gửi báo cáo thất bại. Kiểm tra mạng/đăng nhập rồi thử lại.");
                }
            });
        } catch (RuntimeException e) {
            SEND_GUARD.finish();
            QuickReportNotification.show(context, "Không thể gửi báo cáo. Vui lòng mở WWarning rồi thử lại.");
        }
    }
}
