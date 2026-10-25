package com.phatvv9.wwarning.data.remote;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Transaction;
import com.phatvv9.wwarning.model.ReputationLog;
import com.phatvv9.wwarning.util.FirestoreNames;
import com.phatvv9.wwarning.util.ReputationPolicy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Cộng/trừ điểm uy tín (users.reputationScore) và ghi reputation_logs trong cùng một transaction. */
public class ReputationRepository {

    private static final String TAG = "WWarning";

    /** Một lần thay đổi điểm kèm lý do. */
    public static class Change {
        public final int delta;
        public final String reason;

        public Change(int delta, @NonNull String reason) {
            this.delta = delta;
            this.reason = reason;
        }
    }

    public interface DoneCallback {
        void onDone();

        void onError(@NonNull Exception e);
    }

    public interface LogsCallback {
        void onLogs(@NonNull List<ReputationLog> logs);

        void onError(@NonNull Exception e);
    }

    private final FirebaseFirestore db;

    public ReputationRepository() {
        this(FirebaseFirestore.getInstance());
    }

    public ReputationRepository(@NonNull FirebaseFirestore db) {
        this.db = db;
    }

    /** Cộng/trừ điểm một lần: updateReputation(uid, delta) theo kế hoạch. */
    public void updateReputation(@NonNull String targetUid, int delta, @Nullable String reportId,
                                 @NonNull String reason, @Nullable DoneCallback callback) {
        List<Change> changes = new ArrayList<>();
        changes.add(new Change(delta, reason));
        applyChanges(targetUid, reportId, changes, callback);
    }

    /**
     * Áp dụng nhiều thay đổi điểm cho một người trong MỘT transaction:
     * đọc điểm hiện tại, ghi điểm mới (không xuống dưới 0) và mỗi thay đổi một dòng reputation_logs.
     */
    public void applyChanges(@NonNull final String targetUid, @Nullable final String reportId,
                             @NonNull final List<Change> changes, @Nullable final DoneCallback callback) {
        if (changes.isEmpty()) {
            if (callback != null) callback.onDone();
            return;
        }
        final DocumentReference userRef = db.collection(FirestoreNames.USERS).document(targetUid);

        db.runTransaction(new Transaction.Function<Void>() {
            @Nullable
            @Override
            public Void apply(@NonNull Transaction transaction) throws FirebaseFirestoreException {
                // Quy tắc của Firestore transaction: đọc hết trước, ghi sau
                DocumentSnapshot userSnap = transaction.get(userRef);
                if (!userSnap.exists()) {
                    throw new FirebaseFirestoreException(
                            "Người nhận điểm chưa có hồ sơ users/" + targetUid,
                            FirebaseFirestoreException.Code.NOT_FOUND);
                }
                Long current = userSnap.getLong("reputationScore");
                long score = current != null ? current : ReputationPolicy.INITIAL_SCORE;

                int sum = 0;
                for (Change change : changes) {
                    sum += change.delta;
                }
                long updated = Math.max(0, score + sum);
                transaction.update(userRef, "reputationScore", updated);

                for (Change change : changes) {
                    DocumentReference logRef =
                            db.collection(FirestoreNames.REPUTATION_LOGS).document();
                    transaction.set(logRef,
                            new ReputationLog(targetUid, reportId, change.delta, change.reason));
                }
                return null;
            }
        }).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                if (callback != null) callback.onDone();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.w(TAG, "Không cập nhật được điểm uy tín", e);
                if (callback != null) callback.onError(e);
            }
        });
    }

    /**
     * Lấy các dòng lịch sử mới nhất của một người.
     * Chỉ lọc theo userId rồi sắp xếp trong máy, nên không cần tạo composite index trên Firestore.
     */
    public void loadRecentLogs(@NonNull String uid, final int limit, @NonNull final LogsCallback callback) {
        db.collection(FirestoreNames.REPUTATION_LOGS)
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot snapshot) {
                        List<ReputationLog> logs = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            ReputationLog log = doc.toObject(ReputationLog.class);
                            if (log != null) logs.add(log);
                        }
                        // Mới nhất lên đầu; dòng chưa có giờ server (vừa ghi) coi là mới nhất
                        Collections.sort(logs, new Comparator<ReputationLog>() {
                            @Override
                            public int compare(ReputationLog a, ReputationLog b) {
                                Timestamp ta = a.getCreatedAt();
                                Timestamp tb = b.getCreatedAt();
                                if (ta == null && tb == null) return 0;
                                if (ta == null) return -1;
                                if (tb == null) return 1;
                                return tb.compareTo(ta);
                            }
                        });
                        if (logs.size() > limit) {
                            logs = new ArrayList<>(logs.subList(0, limit));
                        }
                        callback.onLogs(logs);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        callback.onError(e);
                    }
                });
    }
}
