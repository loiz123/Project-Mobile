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
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Transaction;
import com.phatvv9.wwarning.model.ReportVote;
import com.phatvv9.wwarning.util.ReputationPolicy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VoteRepository {
    private static final int DEFAULT_DOWNVOTE_THRESHOLD = 5;
    private final FirebaseFirestore db;
    private final ReputationRepository reputationRepository;

    /** Kết quả của transaction, dùng để tính điểm uy tín cho người đăng báo cáo. */
    private static class Outcome {
        String authorId;
        int voteDelta;
        String voteReason;
        boolean newlyHidden;
    }

    public VoteRepository() {
        this(FirebaseFirestore.getInstance());
    }

    public VoteRepository(@NonNull FirebaseFirestore db) {
        this.db = db;
        this.reputationRepository = new ReputationRepository(db);
    }

    public void castVote(String reportId, String userId, int voteValue, @Nullable final VoteCallback callback) {
        String voteDocId = reportId + "_" + userId;
        DocumentReference voteRef = db.collection("votes").document(voteDocId);
        DocumentReference reportRef = db.collection("reports").document(reportId);
        final Outcome outcome = new Outcome();
        final String voterId = userId;

        db.runTransaction(new Transaction.Function<Void>() {
            @Nullable
            @Override
            public Void apply(@NonNull Transaction transaction) throws FirebaseFirestoreException {
                // Transaction có thể chạy lại, nên đặt lại kết quả mỗi lần
                outcome.voteDelta = 0;
                outcome.voteReason = null;
                outcome.newlyHidden = false;

                DocumentSnapshot voteSnap = transaction.get(voteRef);
                DocumentSnapshot reportSnap = transaction.get(reportRef);
                outcome.authorId = reportSnap.getString("userId");
                String previousStatus = reportSnap.getString("status");

                long currentUpCount = reportSnap.contains("upCount") && reportSnap.getLong("upCount") != null ? reportSnap.getLong("upCount") : 0;
                long currentDownCount = reportSnap.contains("downCount") && reportSnap.getLong("downCount") != null ? reportSnap.getLong("downCount") : 0;

                if (voteSnap.exists()) {
                    Long oldValLong = voteSnap.getLong("voteValue");
                    int oldVal = oldValLong != null ? oldValLong.intValue() : 0;

                    if (oldVal == voteValue) {
                        // Người dùng bấm lại cùng 1 nút -> Hủy vote
                        transaction.delete(voteRef);
                        outcome.voteDelta = -oldVal * ReputationPolicy.POINTS_PER_VOTE;
                        outcome.voteReason = ReputationPolicy.REASON_VOTE_CANCELLED;
                        if (voteValue == 1) currentUpCount = Math.max(0, currentUpCount - 1);
                        else if (voteValue == -1) currentDownCount = Math.max(0, currentDownCount - 1);
                    } else {
                        // Đổi chiều vote
                        Map<String, Object> voteData = new HashMap<>();
                        voteData.put("voteValue", voteValue);
                        voteData.put("createdAt", Timestamp.now());
                        transaction.set(voteRef, voteData, SetOptions.merge());
                        outcome.voteDelta = (voteValue - oldVal) * ReputationPolicy.POINTS_PER_VOTE;
                        outcome.voteReason = ReputationPolicy.REASON_VOTE_CHANGED;

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
                    outcome.voteDelta = voteValue * ReputationPolicy.POINTS_PER_VOTE;
                    outcome.voteReason = voteValue == 1
                            ? ReputationPolicy.REASON_UPVOTE
                            : ReputationPolicy.REASON_DOWNVOTE;
                    if (voteValue == 1) currentUpCount += 1;
                    else if (voteValue == -1) currentDownCount += 1;
                }

                Map<String, Object> updateReport = new HashMap<>();
                updateReport.put("upCount", currentUpCount);
                updateReport.put("downCount", currentDownCount);

                // Tự động ẩn bài viết nếu downvote > upvote + ngưỡng (mặc định 5)
                if (currentDownCount > currentUpCount + DEFAULT_DOWNVOTE_THRESHOLD) {
                    updateReport.put("status", "Hidden");
                    outcome.newlyHidden = !"Hidden".equals(previousStatus);
                }

                transaction.update(reportRef, updateReport);
                return null;
            }
        }).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                applyReputation(outcome, reportId, voterId, callback);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (callback != null) callback.onError(e.getMessage());
            }
        });
    }

    /**
     * Sau khi vote ghi xong: cộng/trừ điểm uy tín cho người đăng báo cáo (không tính khi tự vote).
     * Lỗi cập nhật điểm chỉ ghi log, vì vote đã được ghi thành công.
     */
    private void applyReputation(@NonNull Outcome outcome, String reportId, String voterId,
                                 @Nullable final VoteCallback callback) {
        List<ReputationRepository.Change> changes = new ArrayList<>();
        boolean selfVote = outcome.authorId == null || outcome.authorId.equals(voterId);
        if (!selfVote) {
            if (outcome.voteDelta != 0 && outcome.voteReason != null) {
                changes.add(new ReputationRepository.Change(outcome.voteDelta, outcome.voteReason));
            }
            if (outcome.newlyHidden) {
                changes.add(new ReputationRepository.Change(
                        ReputationPolicy.HIDDEN_PENALTY, ReputationPolicy.REASON_HIDDEN));
            }
        }
        if (changes.isEmpty()) {
            if (callback != null) callback.onSuccess();
            return;
        }
        reputationRepository.applyChanges(outcome.authorId, reportId, changes,
                new ReputationRepository.DoneCallback() {
                    @Override
                    public void onDone() {
                        if (callback != null) callback.onSuccess();
                    }

                    @Override
                    public void onError(@NonNull Exception e) {
                        Log.w("WWarning", "Vote ghi được nhưng chưa cập nhật điểm uy tín", e);
                        if (callback != null) callback.onSuccess();
                    }
                });
    }

    public interface VoteCallback {
        void onSuccess();
        void onError(String error);
    }
}
