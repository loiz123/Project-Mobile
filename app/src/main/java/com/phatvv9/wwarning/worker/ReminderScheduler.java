package com.phatvv9.wwarning.worker;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.phatvv9.wwarning.util.FirestoreNames;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Lập lịch nhắc cập nhật thời tiết sau khi gửi báo cáo thành công.
 * Mỗi người dùng chỉ có một bộ đếm: báo cáo mới thì hủy bộ đếm cũ.
 */
public final class ReminderScheduler {
    public static final String PREFS = "reminder_prefs";
    public static final String KEY_ACTIVE_REMINDER_ID = "active_reminder_id";
    public static final int DEFAULT_REMINDER_MINUTES = 15;

    private static final String TAG = "ReminderScheduler";
    private static final int REQUEST_POST_NOTIFICATIONS = 2001;

    private ReminderScheduler() {}

    public static String workName(@NonNull String uid) {
        return "reminder_" + uid;
    }

    /** Gọi sau khi báo cáo được gửi thành công. */
    public static void schedule(@NonNull Context context, @NonNull final String reportId) {
        final Context appContext = context.getApplicationContext();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        final String uid = user.getUid();

        FirebaseFirestore.getInstance()
                .collection(FirestoreNames.APP_SETTINGS)
                .document(FirestoreNames.SettingKey.REMINDER_MINUTES)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        int minutes = DEFAULT_REMINDER_MINUTES;
                        if (task.isSuccessful() && task.getResult() != null) {
                            minutes = readMinutes(task.getResult());
                        }
                        enqueue(appContext, uid, reportId, minutes);
                    }
                });
    }

    private static void enqueue(Context context, String uid, String reportId, int minutes) {
        // Báo cáo mới -> hủy bộ đếm cũ trước khi tạo bộ mới
        cancelActive(context);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference ref = db.collection(FirestoreNames.REMINDERS).document();
        long now = System.currentTimeMillis();

        Map<String, Object> data = new HashMap<>();
        data.put("userId", uid);
        data.put("reportId", reportId);
        data.put("status", FirestoreNames.ReminderStatus.PENDING);
        data.put("triggerAt", new Timestamp(new Date(now + minutes * 60L * 1000L)));
        data.put("createdAt", new Timestamp(new Date(now)));
        ref.set(data).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.w(TAG, "Không ghi được reminders", e);
            }
        });

        Data input = new Data.Builder()
                .putString(WeatherReminderWorker.KEY_REMINDER_ID, ref.getId())
                .putString(WeatherReminderWorker.KEY_REPORT_ID, reportId)
                .putString(WeatherReminderWorker.KEY_USER_ID, uid)
                .putInt(WeatherReminderWorker.KEY_MINUTES, minutes)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(WeatherReminderWorker.class)
                .setInitialDelay(minutes, TimeUnit.MINUTES)
                .setInputData(input)
                .build();

        WorkManager.getInstance(context)
                .enqueueUniqueWork(workName(uid), ExistingWorkPolicy.REPLACE, request);

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY_ACTIVE_REMINDER_ID, ref.getId()).apply();
    }

    /** Phục vụ demo/chấm điểm nhanh: bắn notification sau số giây chỉ định (ví dụ 5 giây). */
    public static void scheduleTestReminder(@NonNull Context context, int seconds) {
        final Context appContext = context.getApplicationContext();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String uid = user != null ? user.getUid() : "demo_user";

        Data input = new Data.Builder()
                .putString(WeatherReminderWorker.KEY_USER_ID, uid)
                .putInt(WeatherReminderWorker.KEY_MINUTES, 1)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(WeatherReminderWorker.class)
                .setInitialDelay(seconds, TimeUnit.SECONDS)
                .setInputData(input)
                .build();

        WorkManager.getInstance(appContext)
                .enqueueUniqueWork("test_reminder_" + uid, ExistingWorkPolicy.REPLACE, request);
    }

    /** Hủy bộ đếm đang chạy (nếu có): hủy WorkRequest, đặt reminders.status = Cancelled, xóa thông báo. */
    public static void cancelActive(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            WorkManager.getInstance(appContext).cancelUniqueWork(workName(user.getUid()));
        }

        SharedPreferences prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String reminderId = prefs.getString(KEY_ACTIVE_REMINDER_ID, null);
        if (reminderId != null) {
            FirebaseFirestore.getInstance()
                    .collection(FirestoreNames.REMINDERS)
                    .document(reminderId)
                    .update("status", FirestoreNames.ReminderStatus.CANCELLED);
            prefs.edit().remove(KEY_ACTIVE_REMINDER_ID).apply();
        }

        NotificationManagerCompat.from(appContext).cancel(WeatherReminderWorker.NOTIFICATION_ID);
    }

    /** Android 13 trở lên cần xin quyền hiện thông báo; gọi một lần khi gửi báo cáo. */
    public static void requestNotificationPermissionIfNeeded(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return;
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Activity activity = findActivity(context);
        if (activity == null) return;
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_POST_NOTIFICATIONS);
    }

    private static Activity findActivity(Context context) {
        Context current = context;
        while (current instanceof ContextWrapper) {
            if (current instanceof Activity) return (Activity) current;
            current = ((ContextWrapper) current).getBaseContext();
        }
        return null;
    }

    // app_settings/reminder_minutes: value dạng chuỗi; thiếu hoặc sai thì dùng mặc định 15
    private static int readMinutes(@NonNull DocumentSnapshot snap) {
        Object raw = snap.exists() ? snap.get("value") : null;
        if (raw == null) return DEFAULT_REMINDER_MINUTES;
        try {
            int minutes = Integer.parseInt(String.valueOf(raw).trim());
            return minutes > 0 ? minutes : DEFAULT_REMINDER_MINUTES;
        } catch (NumberFormatException e) {
            return DEFAULT_REMINDER_MINUTES;
        }
    }
}