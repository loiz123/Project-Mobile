package com.phatvv9.wwarning.notification;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.phatvv9.wwarning.MainActivity;
import com.phatvv9.wwarning.R;

/** Quick-report notification. Android/OEM settings control lock-screen visibility. */
public final class QuickReportNotification {
    public static final String CHANNEL_ID = "wwarning_quick_report";
    public static final int NOTIFICATION_ID = 8201;
    public static final String ACTION_REPORT = "com.phatvv9.wwarning.ACTION_QUICK_REPORT";
    public static final String EXTRA_CATEGORY_ID = "categoryId";
    public static final String EXTRA_CATEGORY_NAME = "categoryName";

    private QuickReportNotification() {}

    public static void show(Context context, String statusText) {
        Context app = context.getApplicationContext();
        createChannel(app);
        Intent open = new Intent(app, MainActivity.class);
        PendingIntent openPending = PendingIntent.getActivity(app, 8201, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(app, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("WWarning · Báo cáo nhanh")
                .setContentText(statusText == null ? "Chạm nút để báo cáo tình hình tại vị trí đã ghi nhận." : statusText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(
                        (statusText == null ? "Báo cáo nhanh tại vị trí đã ghi nhận." : statusText)
                                + "\nKhông cần chụp ảnh khi gửi từ thông báo."))
                .setContentIntent(openPending)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setOngoing(false)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW);
        addQuickReportActions(builder, app);

        if (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManager manager = (NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) manager.notify(NOTIFICATION_ID, builder.build());
        }
    }

    public static void dismiss(Context context) {
        NotificationManager manager = (NotificationManager)
                context.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.cancel(NOTIFICATION_ID);
    }

    /** Gắn các thao tác Quick Report vào bất kỳ thông báo nào của WWarning. */
    public static void addQuickReportActions(NotificationCompat.Builder builder, Context context) {
        Context app = context.getApplicationContext();
        builder.addAction(0, "Mưa", reportPending(app, "cat_mua", "Mưa", 1))
                .addAction(0, "Nắng", reportPending(app, "cat_nang", "Nắng", 4))
                .addAction(0, "Ngập", reportPending(app, "cat_ngap", "Ngập lụt", 2))
                .addAction(0, "Bão", reportPending(app, "cat_bao", "Bão", 3))
                .addAction(0, "Sương mù", reportPending(app, "cat_suong", "Sương mù", 5))
                .addAction(0, "Âm u", reportPending(app, "cat_am_u", "Âm u", 6));
    }

    private static PendingIntent reportPending(Context context, String categoryId, String name, int requestCode) {
        Intent intent = new Intent(context, QuickReportActionReceiver.class);
        intent.setAction(ACTION_REPORT);
        intent.putExtra(EXTRA_CATEGORY_ID, categoryId);
        intent.putExtra(EXTRA_CATEGORY_NAME, name);
        return PendingIntent.getBroadcast(context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null && manager.getNotificationChannel(CHANNEL_ID) == null) {
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Báo cáo nhanh", NotificationManager.IMPORTANCE_LOW);
                channel.setDescription("Thông báo cố định có nút gửi báo cáo WWarning");
                channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
                manager.createNotificationChannel(channel);
            }
        }
    }
}
