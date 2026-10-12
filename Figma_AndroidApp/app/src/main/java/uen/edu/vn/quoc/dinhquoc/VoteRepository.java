package uen.edu.vn.quoc.dinhquoc;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class VoteRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Xử lý Upvote/Downvote và tự động ẩn báo cáo nếu bị Downvote nhiều
     * @param reportId ID của bài báo cáo thời tiết trong Firestore
     * @param voteValue 1 cho Upvote, -1 cho Downvote
     */
    public void castVote(String reportId, int voteValue) {
        DocumentReference reportRef = db.collection("weather_reports").document(reportId);

        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(reportRef);

            Long currentUpvotes = snapshot.getLong("upvotes");
            Long currentDownvotes = snapshot.getLong("downvotes");

            long upvotes = (currentUpvotes != null) ? currentUpvotes : 0;
            long downvotes = (currentDownvotes != null) ? currentDownvotes : 0;

            if (voteValue == 1) {
                upvotes++;
            } else if (voteValue == -1) {
                downvotes++;
            }

            // Cập nhật số lượng vote
            transaction.update(reportRef, "upvotes", upvotes);
            transaction.update(reportRef, "downvotes", downvotes);

            // Quy tắc kiểm duyệt: Nếu Downvote > Upvote + 5 thì tự động ẩn tin giả
            if (downvotes > (upvotes + 5)) {
                transaction.update(reportRef, "status", "Hidden");
            }

            return null;
        });
    }
}