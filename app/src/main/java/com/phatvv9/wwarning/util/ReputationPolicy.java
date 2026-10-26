package com.phatvv9.wwarning.util;

/**
 * Quy tắc tính điểm uy tín. Đổi số ở đây là cả app đổi theo.
 * Điểm được cộng/trừ cho NGƯỜI ĐĂNG báo cáo, theo bình chọn của người khác.
 */
public final class ReputationPolicy {

    private ReputationPolicy() {}

    /** Điểm khởi tạo khi tạo users/{uid} (khớp UserProfile). */
    public static final int INITIAL_SCORE = 100;

    /** Mỗi lượt "Hữu ích" cộng bấy nhiêu, mỗi lượt "Báo sai" trừ bấy nhiêu. */
    public static final int POINTS_PER_VOTE = 2;

    /** Trừ thêm một lần khi báo cáo bị cộng đồng tự động ẩn. */
    public static final int HIDDEN_PENALTY = -10;

    // Lý do ghi vào reputation_logs.reason (hiển thị thẳng trong Hồ sơ)
    public static final String REASON_UPVOTE = "Báo cáo được cộng đồng xác nhận";
    public static final String REASON_DOWNVOTE = "Báo cáo bị đánh dấu sai";
    public static final String REASON_VOTE_CHANGED = "Cộng đồng đổi bình chọn";
    public static final String REASON_VOTE_CANCELLED = "Cộng đồng hủy bình chọn";
    public static final String REASON_HIDDEN = "Báo cáo bị cộng đồng ẩn";
}
