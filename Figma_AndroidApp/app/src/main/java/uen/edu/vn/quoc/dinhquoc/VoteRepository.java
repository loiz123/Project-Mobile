package uen.edu.vn.quoc.dinhquoc;

import android.util.Log;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

public class VoteRepository {

    private static final String TAG = "VoteRepository";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Thực hiện đánh giá Upvote (+1) hoặc Downvote (-1) cho một báo cáo thời tiết
     *
     * @param reportId  ID của document báo cáo thời tiết trong collection "weather_reports"
     * @param voteValue Giá trị vote: 1 là Upvote, -1 là Downvote
     */
    public void castVote(String reportId, int voteValue) {
        if (reportId == null || reportId.isEmpty()) {
            Log.e(TAG, "reportId không hợp lệ");
            return;
        }

        DocumentReference reportRef = db.collection("weather_reports").document(reportId);

        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(reportRef);

            if (!snapshot.exists()) {
                Log.e(TAG, "Báo cáo không tồn tại!");
                return null;
            }

            // Lấy dữ liệu lượt vote hiện tại
            Long currentUpvotes = snapshot.getLong("upvotes");
            Long currentDownvotes = snapshot.getLong("downvotes");
            String authorId = snapshot.getString("user_id");

            long upvotes = (currentUpvotes != null) ? currentUpvotes : 0;
            long downvotes = (currentDownvotes != null) ? currentDownvotes : 0;

            // Cập nhật số lượng theo nút bấm
            if (voteValue == 1) {
                upvotes++;
            } else if (voteValue == -1) {
                downvotes++;
            }

            // Cập nhật giá trị vote mới vào Document
            transaction.update(reportRef, "upvotes", upvotes);
            transaction.update(reportRef, "downvotes", downvotes);

            // Quy tắc kiểm duyệt: Nếu Downvote > Upvote + 5 thì tự động ẩn tin
            if (downvotes > (upvotes + 5)) {
                transaction.update(reportRef, "status", "Hidden");

                // Trừ 10 điểm uy tín của tác giả tạo báo cáo giả mạo này
                if (authorId != null && !authorId.isEmpty()) {
                    DocumentReference authorRef = db.collection("users").document(authorId);
                    transaction.update(authorRef, "reputation_score", FieldValue.increment(-10));
                }
            }

            return null;
        }).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Đã gửi đánh giá thành công cho reportId: " + reportId);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Lỗi khi gửi đánh giá: ", e);
        });
    }
}