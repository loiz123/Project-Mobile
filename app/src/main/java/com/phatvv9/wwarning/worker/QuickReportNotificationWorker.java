package com.phatvv9.wwarning.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.phatvv9.wwarning.notification.QuickReportNotification;
import com.phatvv9.wwarning.util.FirestoreNames;

import java.util.concurrent.TimeUnit;

/** Periodically refreshes only the signed-in user's currently visible reports. */
public class QuickReportNotificationWorker extends Worker {
    public QuickReportNotificationWorker(@NonNull Context context, @NonNull WorkerParameters params) { super(context, params); }

    @NonNull @Override public Result doWork() {
        String userId = FirebaseAuth.getInstance().getCurrentUser() == null
                ? null
                : FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (userId == null) {
            QuickReportNotification.dismiss(getApplicationContext());
            return Result.success();
        }
        try {
            QuerySnapshot snapshot = Tasks.await(FirebaseFirestore.getInstance()
                    .collection(FirestoreNames.REPORTS)
                    .whereEqualTo("userId", userId)
                    .get(), 20, TimeUnit.SECONDS);
            int count = countVisibleReports(snapshot);
            if (count == 0) {
                QuickReportNotification.dismiss(getApplicationContext());
                return Result.success();
            }
            QuickReportNotification.show(getApplicationContext(),
                    "Bạn đang có " + count + " báo cáo đang hiển thị. Chạm nút bên dưới để gửi Quick Report.");
            return Result.success();
        } catch (Exception e) {
            QuickReportNotification.dismiss(getApplicationContext());
            return Result.retry();
        }
    }

    private int countVisibleReports(QuerySnapshot snapshot) {
        if (snapshot == null) return 0;
        Timestamp now = Timestamp.now();
        int count = 0;
        for (DocumentSnapshot report : snapshot.getDocuments()) {
            Timestamp expiresAt = report.getTimestamp("expiresAt");
            if (FirestoreNames.ReportStatus.ACTIVE.equals(report.getString("status"))
                    && expiresAt != null
                    && expiresAt.compareTo(now) > 0) {
                count++;
            }
        }
        return count;
    }
}
