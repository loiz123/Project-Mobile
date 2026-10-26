package com.phatvv9.wwarning.data.remote;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.phatvv9.wwarning.model.Report;
import com.phatvv9.wwarning.util.FirestoreNames;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Kiểm duyệt báo cáo cho màn Admin: nghe danh sách báo cáo bị báo sai nhiều,
 * ẩn / xóa / khôi phục và ghi moderation_logs trong cùng một batch.
 */
public class ModerationRepository {
    private static final String TAG = "ModerationRepository";
    private static final int REPORT_LIMIT = 50;

    public interface ReportsCallback {
        void onReports(@NonNull List<Report> reports);

        void onError(@NonNull Exception e);
    }

    public interface ActionCallback {
        void onSuccess();

        void onError(@NonNull Exception e);
    }

    private final FirebaseFirestore db;
    private ListenerRegistration reportsRegistration;

    public ModerationRepository() {
        this(FirebaseFirestore.getInstance());
    }

    public ModerationRepository(@NonNull FirebaseFirestore db) {
        this.db = db;
    }

    /** Nghe realtime tối đa 50 báo cáo, nhiều downCount nhất lên đầu; không lọc status để admin thấy cả báo cáo đã ẩn. */
    public void listenToReports(@NonNull final ReportsCallback callback) {
        stopListening();
        reportsRegistration = db.collection(FirestoreNames.REPORTS)
                .orderBy("downCount", Query.Direction.DESCENDING)
                .limit(REPORT_LIMIT)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            Log.e(TAG, "Không nghe được danh sách báo cáo", error);
                            callback.onError(error);
                            return;
                        }
                        List<Report> reports = new ArrayList<>();
                        if (value != null) {
                            for (QueryDocumentSnapshot doc : value) {
                                Report report = doc.toObject(Report.class);
                                report.setId(doc.getId());
                                reports.add(report);
                            }
                        }
                        callback.onReports(reports);
                    }
                });
    }

    public void stopListening() {
        if (reportsRegistration != null) {
            reportsRegistration.remove();
            reportsRegistration = null;
        }
    }

    /**
     * action là một trong FirestoreNames.ModerationAction (Hide, Delete, Restore).
     * Đổi reports.status và tạo moderation_logs trong cùng một batch: hoặc cùng thành công, hoặc cùng thất bại.
     * Xóa là xóa mềm (status = Deleted) vì rules không cho xóa document báo cáo.
     */
    public void moderate(@NonNull String adminId, @NonNull String reportId, @NonNull String action,
                         @Nullable final ActionCallback callback) {
        String newStatus = statusFor(action);
        if (newStatus == null) {
            if (callback != null) {
                callback.onError(new IllegalArgumentException("Hành động không hợp lệ: " + action));
            }
            return;
        }

        DocumentReference reportRef = db.collection(FirestoreNames.REPORTS).document(reportId);
        DocumentReference logRef = db.collection(FirestoreNames.MODERATION_LOGS).document();

        Map<String, Object> log = new HashMap<>();
        log.put("adminId", adminId);
        log.put("reportId", reportId);
        log.put("action", action);
        log.put("createdAt", Timestamp.now());

        WriteBatch batch = db.batch();
        batch.update(reportRef, "status", newStatus);
        batch.set(logRef, log);
        batch.commit()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        if (callback != null) callback.onSuccess();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Kiểm duyệt bị từ chối", e);
                        if (callback != null) callback.onError(e);
                    }
                });
    }

    @Nullable
    private static String statusFor(@NonNull String action) {
        if (FirestoreNames.ModerationAction.HIDE.equals(action)) {
            return FirestoreNames.ReportStatus.HIDDEN;
        }
        if (FirestoreNames.ModerationAction.DELETE.equals(action)) {
            return FirestoreNames.ReportStatus.DELETED;
        }
        if (FirestoreNames.ModerationAction.RESTORE.equals(action)) {
            return FirestoreNames.ReportStatus.ACTIVE;
        }
        return null;
    }
}