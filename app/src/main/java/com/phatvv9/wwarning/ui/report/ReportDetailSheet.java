package com.phatvv9.wwarning.ui.report;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.phatvv9.wwarning.R;
import com.phatvv9.wwarning.data.remote.UserRepository;
import com.phatvv9.wwarning.data.remote.VoteRepository;
import com.phatvv9.wwarning.model.Report;
import com.phatvv9.wwarning.model.UserProfile;
import com.phatvv9.wwarning.ui.map.MapFragment;

public class ReportDetailSheet extends BottomSheetDialogFragment {
    private static final String ARG_REPORT_ID = "reportId";
    private static final String ARG_CATEGORY = "categoryId";
    private static final String ARG_SEVERITY = "severityId";
    private static final String ARG_CREATED = "createdAtMillis";
    private static final String ARG_UP = "upCount";
    private static final String ARG_DOWN = "downCount";
    private static final String ARG_AUTHOR = "authorId";

    private String reportId;
    private String categoryId;
    private String severityId;
    private String authorId;
    private long createdAtMillis;
    private int upCount;
    private int downCount;

    private TextView tvCategory;
    private TextView tvTime;
    private TextView tvSeverity;
    private TextView tvAuthor;
    private TextView tvUpvoteCount;
    private TextView tvDownvoteCount;
    private Button btnUpvote;
    private Button btnDownvote;
    private Button btnDeleteMyReport;

    private final VoteRepository voteRepository = new VoteRepository();

    public static ReportDetailSheet newInstance(Report report) {
        Bundle args = new Bundle();
        args.putString(ARG_REPORT_ID, report.getId());
        args.putString(ARG_CATEGORY, report.getCategoryId());
        args.putString(ARG_SEVERITY, report.getSeverityId());
        args.putString(ARG_AUTHOR, report.getUserId());
        Timestamp created = report.getCreatedAt();
        args.putLong(ARG_CREATED, created != null ? created.toDate().getTime() : -1L);
        args.putInt(ARG_UP, report.getUpCount());
        args.putInt(ARG_DOWN, report.getDownCount());

        ReportDetailSheet sheet = new ReportDetailSheet();
        sheet.setArguments(args);
        return sheet;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            reportId = args.getString(ARG_REPORT_ID);
            categoryId = args.getString(ARG_CATEGORY);
            severityId = args.getString(ARG_SEVERITY);
            authorId = args.getString(ARG_AUTHOR);
            createdAtMillis = args.getLong(ARG_CREATED, -1L);
            upCount = args.getInt(ARG_UP, 0);
            downCount = args.getInt(ARG_DOWN, 0);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_report_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvCategory = view.findViewById(R.id.tvReportCategory);
        tvTime = view.findViewById(R.id.tvReportTime);
        tvSeverity = view.findViewById(R.id.tvReportSeverity);
        tvAuthor = view.findViewById(R.id.tvReportAuthor);
        tvUpvoteCount = view.findViewById(R.id.tvUpvoteCount);
        tvDownvoteCount = view.findViewById(R.id.tvDownvoteCount);
        btnUpvote = view.findViewById(R.id.btnUpvote);
        btnDownvote = view.findViewById(R.id.btnDownvote);
        btnDeleteMyReport = view.findViewById(R.id.btnDeleteMyReport);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        boolean isMyReport = currentUser != null && authorId != null && authorId.equals(currentUser.getUid());
        UserProfile currentProfile = UserRepository.getInstance().getCurrentProfile().getValue();
        boolean isAdmin = com.phatvv9.wwarning.util.AdminSessionManager.isAdmin(requireContext())
                || (currentProfile != null && currentProfile.isAdmin());

        if (isMyReport || isAdmin) {
            btnDeleteMyReport.setVisibility(View.VISIBLE);
            btnDeleteMyReport.setText(isMyReport ? "🗑️ Xóa báo cáo của tôi" : "🗑️ Xóa báo cáo (Quyền Admin)");
            btnDeleteMyReport.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    confirmDeleteReport();
                }
            });
        } else {
            btnDeleteMyReport.setVisibility(View.GONE);
        }

        tvCategory.setText(MapFragment.getCategoryDisplayName(categoryId));
        tvSeverity.setText("Mức độ: Không rõ");
        loadSeverityName();
        if (createdAtMillis > 0) {
            CharSequence relative = DateUtils.getRelativeTimeSpanString(
                    createdAtMillis, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
            tvTime.setText("Thời gian: " + relative);
        } else {
            tvTime.setText("Thời gian: Vừa xong");
        }

        showCounts();
        loadAuthor();

        btnUpvote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vote(1);
            }
        });
        btnDownvote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vote(-1);
            }
        });
    }

    // severityId là id document trong collection "severities"; đọc field "name" để hiện tên
    private void loadSeverityName() {
        if (severityId == null || severityId.isEmpty()) return;
        FirebaseFirestore.getInstance().collection("severities").document(severityId).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot snap) {
                        if (!isAdded() || getView() == null) return;
                        String name = snap.getString("name");
                        if (name != null && !name.isEmpty()) {
                            tvSeverity.setText("Mức độ: " + name);
                        }
                    }
                });
    }

    // Hiện tên và điểm uy tín của người đăng báo cáo (users/{authorId})
    private void loadAuthor() {
        if (tvAuthor == null || authorId == null || authorId.isEmpty()) return;
        UserRepository.getInstance().fetchProfile(authorId, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(@NonNull UserProfile profile) {
                if (!isAdded() || tvAuthor == null) return;
                tvAuthor.setText("Người đăng: " + profile.getDisplayName()
                        + " · Uy tín: " + profile.getReputationScore());
            }

            @Override
            public void onError(@NonNull Exception e) {
                // Báo cáo cũ có thể chưa có hồ sơ người đăng; giữ nguyên dòng mặc định
            }
        });
    }

    private void showCounts() {
        tvUpvoteCount.setText("Hữu ích: " + upCount);
        tvDownvoteCount.setText("Báo sai: " + downCount);
    }

    private void setButtonsEnabled(boolean enabled) {
        btnUpvote.setEnabled(enabled);
        btnDownvote.setEnabled(enabled);
    }

    private void vote(int voteValue) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Bạn cần đăng nhập để bình chọn", Toast.LENGTH_SHORT).show();
            return;
        }
        if (UserRepository.getInstance().isBanned()) {
            Toast.makeText(requireContext(), "Tài khoản của bạn đã bị khóa, không thể bình chọn", Toast.LENGTH_SHORT).show();
            return;
        }
        if (reportId == null) return;

        setButtonsEnabled(false);
        voteRepository.castVote(reportId, user.getUid(), voteValue, new VoteRepository.VoteCallback() {
            @Override
            public void onSuccess() {
                refreshCounts();
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                setButtonsEnabled(true);
                Toast.makeText(requireContext(), "Không bình chọn được: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // VoteRepository chỉ ghi Firestore, nên đọc lại báo cáo để hiện số vote mới
    private void refreshCounts() {
        FirebaseFirestore.getInstance().collection("reports").document(reportId).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot snap) {
                        if (!isAdded()) return;
                        Long up = snap.getLong("upCount");
                        Long down = snap.getLong("downCount");
                        upCount = up != null ? up.intValue() : 0;
                        downCount = down != null ? down.intValue() : 0;
                        showCounts();
                        loadAuthor();
                        setButtonsEnabled(true);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if (!isAdded()) return;
                        setButtonsEnabled(true);
                    }
                });
    }

    private void confirmDeleteReport() {
        if (!isAdded() || reportId == null) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("Xóa báo cáo")
                .setMessage("Bạn có chắc chắn muốn xóa cảnh báo này khỏi bản đồ?")
                .setPositiveButton("Xóa", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteReport();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteReport() {
        if (reportId == null) return;
        setButtonsEnabled(false);
        if (btnDeleteMyReport != null) btnDeleteMyReport.setEnabled(false);

        FirebaseFirestore.getInstance().collection("reports").document(reportId)
                .update("status", "Deleted")
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), "Đã xóa báo cáo thành công", Toast.LENGTH_SHORT).show();
                            dismiss();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if (isAdded()) {
                            setButtonsEnabled(true);
                            if (btnDeleteMyReport != null) btnDeleteMyReport.setEnabled(true);
                            Toast.makeText(requireContext(), "Lỗi khi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}