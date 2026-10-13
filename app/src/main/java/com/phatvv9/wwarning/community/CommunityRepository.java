package com.phatvv9.wwarning.community;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;

import java.util.HashMap;
import java.util.Map;

public class CommunityRepository {
    private final FirebaseFirestore db;

    public CommunityRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Xử lý bình chọn Upvote/Downvote và tự động ẩn bài nếu vi phạm quy tắc: Downvote > Upvote + 5
     */
    public void castVote(String reportId, String userId, int voteType, VoteCallback callback) {
        DocumentReference reportRef = db.collection("weather_reports").document(reportId);
        DocumentReference voteRef = db.collection("report_votes").document(reportId + "_" + userId);

        db.runTransaction((Transaction.Function<Void>) transaction -> {
                    // 1. Kiểm tra bài báo cáo có tồn tại không
                    DocumentSnapshot reportSnapshot = transaction.get(reportRef);
                    if (!reportSnapshot.exists()) {
                        throw new IllegalStateException("Báo cáo không tồn tại!");
                    }

                    // 2. Lấy số lượt vote hiện tại
                    Long upvotes = reportSnapshot.getLong("upvotes");
                    Long downvotes = reportSnapshot.getLong("downvotes");
                    upvotes = (upvotes != null) ? upvotes : 0L;
                    downvotes = (downvotes != null) ? downvotes : 0L;

                    // 3. Kiểm tra user đã vote chưa
                    DocumentSnapshot voteSnapshot = transaction.get(voteRef);
                    if (voteSnapshot.exists()) {
                        Long oldVoteType = voteSnapshot.getLong("voteType");
                        if (oldVoteType != null && oldVoteType == voteType) {
                            throw new IllegalStateException("Bạn đã bình chọn rồi!");
                        }
                        // Nếu đổi từ Upvote sang Downvote hoặc ngược lại
                        if (voteType == 1) {
                            upvotes += 1;
                            downvotes -= 1;
                        } else {
                            downvotes += 1;
                            upvotes -= 1;
                        }
                    } else {
                        // Vote mới
                        if (voteType == 1) {
                            upvotes += 1;
                        } else {
                            downvotes += 1;
                        }
                    }

                    // 4. Kiểm tra quy tắc tự động ẩn bài: Downvote > Upvote + 5
                    String status = reportSnapshot.getString("status");
                    if (downvotes > upvotes + 5) {
                        status = "Hidden";
                    }

                    // 5. Cập nhật dữ liệu vào Firestore
                    transaction.update(reportRef, "upvotes", upvotes, "downvotes", downvotes, "status", status);

                    Map<String, Object> voteData = new HashMap<>();
                    voteData.put("voteId", voteRef.getId());
                    voteData.put("reportId", reportId);
                    voteData.put("userId", userId);
                    voteData.put("voteType", voteType);
                    voteData.put("timestamp", System.currentTimeMillis());
                    transaction.set(voteRef, voteData);

                    return null;
                }).addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    public interface VoteCallback {
        void onSuccess();
        void onError(Exception e);
    }
}