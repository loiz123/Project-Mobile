package com.phatvv9.wwarning.data.remote;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Transaction;
import com.phatvv9.wwarning.model.ReportVote;

import java.util.HashMap;
import java.util.Map;

public class VoteRepository {
    private static final int DEFAULT_DOWNVOTE_THRESHOLD = 5;
    private final FirebaseFirestore db;

    public VoteRepository() {
        this(FirebaseFirestore.getInstance());
    }

    public VoteRepository(@NonNull FirebaseFirestore db) {
        this.db = db;
    }

    public void castVote(String reportId, String userId, int voteValue, @Nullable final VoteCallback callback) {
        String voteDocId = reportId + "_" + userId;
        DocumentReference voteRef = db.collection("votes").document(voteDocId);
        DocumentReference reportRef = db.collection("reports").document(reportId);

        db.runTransaction(new Transaction.Function<Void>() {
            @Nullable
            @Override
            public Void apply(@NonNull Transaction transaction) throws FirebaseFirestoreException {
                DocumentSnapshot voteSnap = transaction.get(voteRef);
                DocumentSnapshot reportSnap = transaction.get(reportRef);

                long currentUpCount = reportSnap.contains("upCount") && reportSnap.getLong("upCount") != null ? reportSnap.getLong("upCount") : 0;
                long currentDownCount = reportSnap.contains("downCount") && reportSnap.getLong("downCount") != null ? reportSnap.getLong("downCount") : 0;

                if (voteSnap.exists()) {
                    Long oldValLong = voteSnap.getLong("voteValue");
                    int oldVal = oldValLong != null ? oldValLong.intValue() : 0;

                    if (oldVal == voteValue) {
                        // Người dùng bấm lại cùng 1 nút -> Hủy vote
                        transaction.delete(voteRef);
                        if (voteValue == 1) currentUpCount = Math.max(0, currentUpCount - 1);
                        else if (voteValue == -1) currentDownCount = Math.max(0, currentDownCount - 1);
                    } else {
                        // Đổi chiều vote
                        Map<String, Object> voteData = new HashMap<>();
                        voteData.put("voteValue", voteValue);
                        voteData.put("createdAt", Timestamp.now());
                        transaction.set(voteRef, voteData, SetOptions.merge());

                        if (voteValue == 1) {
                            currentUpCount += 1;
                            currentDownCount = Math.max(0, currentDownCount - 1);
                        } else {
                            currentDownCount += 1;
                            currentUpCount = Math.max(0, currentUpCount - 1);
                        }
                    }
                } else {
                    // Tạo vote mới
                    ReportVote newVote = new ReportVote(voteDocId, reportId, userId, voteValue, Timestamp.now());
                    transaction.set(voteRef, newVote);
                    if (voteValue == 1) currentUpCount += 1;
                    else if (voteValue == -1) currentDownCount += 1;
                }

                Map<String, Object> updateReport = new HashMap<>();
                updateReport.put("upCount", currentUpCount);
                updateReport.put("downCount", currentDownCount);

                // Tự động ẩn bài viết nếu downvote > upvote + ngưỡng (mặc định 5)
                if (currentDownCount > currentUpCount + DEFAULT_DOWNVOTE_THRESHOLD) {
                    updateReport.put("status", "Hidden");
                }

                transaction.update(reportRef, updateReport);
                return null;
            }
        }).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                if (callback != null) callback.onSuccess();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (callback != null) callback.onError(e.getMessage());
            }
        });
    }

    public interface VoteCallback {
        void onSuccess();
        void onError(String error);
    }
}
