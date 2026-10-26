package com.phatvv9.wwarning.worker;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.phatvv9.wwarning.util.FirestoreNames;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class WeatherReminderWorker extends Worker {
    public static final String KEY_REMINDER_ID = "reminderId";
    public static final String KEY_REPORT_ID = "reportId";
    public static final String KEY_USER_ID = "userId";
    public static final String KEY_MINUTES = "minutes";
    public static final int NOTIFICATION_ID = 1001;

    private static final String TAG = "WeatherReminderWorker";
    private static final String CHANNEL_ID = "weather_reminder_channel";

    public WeatherReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        String reminderId = getInputData().getString(KEY_REMINDER_ID);
        String reportId = getInputData().getString(KEY_REPORT_ID);
        String userId = getInputData().getString(KEY_USER_ID);
        int minutes = getInputData().getInt(KEY_MINUTES, ReminderScheduler.DEFAULT_REMINDER_MINUTES);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference reminderRef = reminderId != null
                ? db.collection(FirestoreNames.REMINDERS).document(reminderId)
                : null;

        // Chỉ nhắc khi bộ đếm còn Pending (chưa bị hủy hoặc đã nhắc rồi)
        if (reminderRef != null && isNotPending(reminderRef)) {
            return Result.success();
        }

        if (userId == null && FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        String message = "Đã " + minutes + " phút trôi qua! "
                + "Tình hình thời tiết tại khu vực của bạn hiện thế nào?";
        String notificationDocId = writeNotificationLog(db, userId, reportId, message);
        showNotification(context, notificationDocId, message);

        if (reminderRef != null) {
            reminderRef.update("status", FirestoreNames.ReminderStatus.TRIGGERED);
            SharedPreferences prefs = context.getSharedPreferences(
                    ReminderScheduler.PREFS, Context.MODE_PRIVATE);
            if (reminderId.equals(prefs.getString(ReminderScheduler.KEY_ACTIVE_REMINDER_ID, null))) {
                prefs.edit().remove(ReminderScheduler.KEY_ACTIVE_REMINDER_ID).apply();
            }
        }
        return Result.success();
    }

    // Đọc trạng thái có giới hạn thời gian; không đọc được thì coi như còn Pending
    private boolean isNotPending(DocumentReference ref) {
        try {
            DocumentSnapshot snap = Tasks.await(ref.get(), 10, TimeUnit.SECONDS);
            return snap.exists()
                    && !FirestoreNames.ReminderStatus.PENDING.equals(snap.getString("status"));
        } catch (ExecutionException e) {
            Log.w(TAG, "Không đọc được reminders", e);
        } catch (TimeoutException e) {
            Log.w(TAG, "Đọc reminders quá thời gian", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return false;
    }

    // Ghi dòng log thông báo; không chờ máy chủ để không treo khi mất mạng
    private String writeNotificationLog(FirebaseFirestore db, String userId, String reportId, String message) {
        DocumentReference ref = db.collection(FirestoreNames.NOTIFICATIONS).document();
        if (userId == null) return ref.getId();

        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("reportId", reportId);
        data.put("message", message);
        data.put("isInteracted", false);
        data.put("createdAt", Timestamp.now());
        ref.set(data);
        return ref.getId();
    }

    private void showNotification(Context context, String notificationDocId, String message) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Chưa có quyền POST_NOTIFICATIONS, bỏ qua thông báo");
            return;
        }

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Weather Reminders", NotificationManager.IMPORTANCE_HIGH);
        manager.createNotificationChannel(channel);

        Intent open = new Intent(context, ReminderOpenActivity.class);
        open.putExtra(ReminderOpenActivity.EXTRA_NOTIFICATION_DOC_ID, notificationDocId);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context, 0, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Cập nhật thời tiết")
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(contentIntent)
                .setAutoCancel(true);

        manager.notify(NOTIFICATION_ID, builder.build());
    }
}