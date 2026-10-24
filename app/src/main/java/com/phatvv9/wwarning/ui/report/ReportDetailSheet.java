package com.phatvv9.wwarning.ui.report;

import android.content.DialogInterface;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
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
import com.phatvv9.wwarning.model.ReportCluster;
import com.phatvv9.wwarning.model.UserProfile;
import com.phatvv9.wwarning.ui.map.MapFragment;

import java.util.List;

public class ReportDetailSheet extends BottomSheetDialogFragment {
    private static final String ARG_REPORT_ID = "reportId";
    private static final String ARG_CATEGORY = "categoryId";
    private static final String ARG_SEVERITY = "severityId";
    private static final String ARG_CREATED = "createdAtMillis";
    private static final String ARG_UP = "upCount";
    private static final String ARG_DOWN = "downCount";
    private static final String ARG_AUTHOR = "authorId";

    private static ReportCluster currentClusterInstance;

    private String reportId;
    private String categoryId;
    private String severityId;
    private String authorId;
    private long createdAtMillis;
    private int upCount;
    private int downCount;

    private LinearLayout llClusterSection;
    private TextView tvClusterTitle;
    private TextView tvClusterSubtitle;
    private LinearLayout llClusterCategories;

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

    public static ReportDetailSheet newInstance(@NonNull ReportCluster cluster) {
        currentClusterInstance = cluster;
        Report primary = cluster.getPrimaryReport();
        if (primary == null && !cluster.getReports().isEmpty()) {
            primary = cluster.getReports().get(0);
        }
        if (primary != null) {
            return createSheet(primary);
        }
        return new ReportDetailSheet();
    }

    public static ReportDetailSheet newInstance(@NonNull Report report) {
        currentClusterInstance = null;
        return createSheet(report);
    }

    private static ReportDetailSheet createSheet(@NonNull Report report) {
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

        llClusterSection = view.findViewById(R.id.llClusterSection);
        tvClusterTitle = view.findViewById(R.id.tvClusterTitle);
        tvClusterSubtitle = view.findViewById(R.id.tvClusterSubtitle);
        llClusterCategories = view.findViewById(R.id.llClusterCategories);

        tvCategory = view.findViewById(R.id.tvReportCategory);
        tvTime = view.findViewById(R.id.tvReportTime);
        tvSeverity = view.findViewById(R.id.tvReportSeverity);
        tvAuthor = view.findViewById(R.id.tvReportAuthor);
        tvUpvoteCount = view.findViewById(R.id.tvUpvoteCount);
        tvDownvoteCount = view.findViewById(R.id.tvDownvoteCount);
        btnUpvote = view.findViewById(R.id.btnUpvote);
        btnDownvote = view.findViewById(R.id.btnDownvote);
        btnDeleteMyReport = view.findViewById(R.id.btnDeleteMyReport);

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

        // Thiết lập hiển thị khối Cluster nếu có từ 2 báo cáo trở lên
        if (currentClusterInstance != null && currentClusterInstance.getReportCount() > 1) {
            if (llClusterSection != null) {
                llClusterSection.setVisibility(View.VISIBLE);
                if (tvClusterTitle != null) {
                    tvClusterTitle.setText("📍 Tổng hợp khu vực 500m (" + currentClusterInstance.getReportCount() + " báo cáo)");
                }
                populateClusterCategories();
            }
        } else {
            if (llClusterSection != null) {
                llClusterSection.setVisibility(View.GONE);
            }
        }

        bindCurrentReportViews();
    }

    private void bindCurrentReportViews() {
        if (tvCategory == null) return;

        tvCategory.setText(MapFragment.getCategoryIconAndName(categoryId));
        tvSeverity.setText("Mức độ: Đang tải...");
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
        updateDeleteButton();
    }

    private void populateClusterCategories() {
        if (llClusterCategories == null || currentClusterInstance == null) return;
        llClusterCategories.removeAllViews();
        float density = getResources().getDisplayMetrics().density;
        int pad12 = (int) (12 * density);
        int pad8 = (int) (8 * density);

        List<ReportCluster.CategoryStat> stats = currentClusterInstance.getCategoryStats();
        for (final ReportCluster.CategoryStat stat : stats) {
            LinearLayout item = new LinearLayout(requireContext());
            item.setOrientation(LinearLayout.VERTICAL);
            item.setPadding(pad12, pad8, pad12, pad8);

            GradientDrawable gd = new GradientDrawable();
            boolean isCurrentCat = stat.getCategoryId() != null && stat.getCategoryId().equals(categoryId);
            gd.setColor(isCurrentCat ? 0xFFEDE7F6 : 0xFFF5F6F9);
            gd.setCornerRadius(10 * density);
            gd.setStroke((int) (1 * density), isCurrentCat ? 0xFF7E57C2 : 0xFFE0E0E0);
            item.setBackground(gd);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = (int) (6 * density);
            item.setLayoutParams(lp);

            // Dòng 1: [Icon + Tên] ---------- [Số lượng báo cáo]
            LinearLayout topRow = new LinearLayout(requireContext());
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView tvName = new TextView(requireContext());
            tvName.setText(MapFragment.getCategoryIconAndName(stat.getCategoryId()));
            tvName.setTextSize(15);
            tvName.setTypeface(null, Typeface.BOLD);
            tvName.setTextColor(0xFF212121);
            LinearLayout.LayoutParams lpName = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            topRow.addView(tvName, lpName);

            TextView tvCount = new TextView(requireContext());
            tvCount.setText(stat.getReportCount() + " báo cáo");
            tvCount.setTextSize(13);
            tvCount.setTextColor(0xFF757575);
            topRow.addView(tvCount);

            item.addView(topRow);

            // Dòng 2: Chi tiết số lượng vote: 👍 X hữu ích · 👎 Y báo sai
            LinearLayout bottomRow = new LinearLayout(requireContext());
            bottomRow.setOrientation(LinearLayout.HORIZONTAL);
            bottomRow.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams lpBottom = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lpBottom.topMargin = (int) (4 * density);
            bottomRow.setLayoutParams(lpBottom);

            TextView tvVotes = new TextView(requireContext());
            String voteText = "👍 " + stat.getTotalUpCount() + " Hữu ích   ·   👎 " + stat.getTotalDownCount() + " Báo sai";
            if (stat.getTotalUpCount() >= 3) {
                voteText += "   ⭐ (Xác thực cao)";
            }
            tvVotes.setText(voteText);
            tvVotes.setTextSize(13);
            tvVotes.setTextColor(stat.getTotalUpCount() >= 3 ? 0xFF2E7D32 : 0xFF616161);
            bottomRow.addView(tvVotes);

            item.addView(bottomRow);

            // Chạm vào dòng category này để xem chi tiết và vote cho báo cáo của loại đó
            item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (stat.getTopReport() != null) {
                        selectReport(stat.getTopReport());
                    }
                }
            });

            llClusterCategories.addView(item);
        }
    }

    private void selectReport(@NonNull Report report) {
        this.reportId = report.getId();
        this.categoryId = report.getCategoryId();
        this.severityId = report.getSeverityId();
        this.authorId = report.getUserId();
        Timestamp created = report.getCreatedAt();
        this.createdAtMillis = created != null ? created.toDate().getTime() : -1L;
        this.upCount = report.getUpCount();
        this.downCount = report.getDownCount();

        bindCurrentReportViews();
        populateClusterCategories();
    }

    private void updateDeleteButton() {
        if (btnDeleteMyReport == null) return;
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String myUid = currentUser != null ? currentUser.getUid() : null;

        boolean isMyReport = myUid != null && authorId != null && authorId.equals(myUid);
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
    }

    // severityId là id document trong collection "severities"; đọc field "name" để hiện tên
    private void loadSeverityName() {
        if (severityId == null || severityId.isEmpty()) {
            if (tvSeverity != null) tvSeverity.setText("Mức độ: Vừa phải");
            return;
        }
        FirebaseFirestore.getInstance().collection("severities").document(severityId).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot snap) {
                        if (!isAdded() || getView() == null || tvSeverity == null) return;
                        String name = snap.getString("name");
                        if (name != null && !name.isEmpty()) {
                            tvSeverity.setText("Mức độ: " + name);
                        } else {
                            tvSeverity.setText("Mức độ: Vừa phải");
                        }
                    }
                });
    }

    // Hiện tên và điểm uy tín của người đăng báo cáo (users/{authorId})
    private void loadAuthor() {
        if (tvAuthor == null) return;
        if (authorId == null || authorId.isEmpty()) {
            tvAuthor.setText("Người đăng: Khách ẩn danh");
            return;
        }
        UserRepository.getInstance().fetchProfile(authorId, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(@NonNull UserProfile profile) {
                if (!isAdded() || tvAuthor == null) return;
                tvAuthor.setText("Người đăng: " + profile.getDisplayName()
                        + " · Uy tín: " + profile.getReputationScore());
            }

            @Override
            public void onError(@NonNull Exception e) {
                if (!isAdded() || tvAuthor == null) return;
                tvAuthor.setText("Người đăng: Khách ẩn danh");
            }
        });
    }

    private void showCounts() {
        if (tvUpvoteCount != null) tvUpvoteCount.setText("Hữu ích: " + upCount);
        if (tvDownvoteCount != null) tvDownvoteCount.setText("Báo sai: " + downCount);
    }

    private void setButtonsEnabled(boolean enabled) {
        if (btnUpvote != null) btnUpvote.setEnabled(enabled);
        if (btnDownvote != null) btnDownvote.setEnabled(enabled);
    }

    private void vote(final int voteValue) {
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
        if (reportId == null) return;
        FirebaseFirestore.getInstance().collection("reports").document(reportId).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot snap) {
                        if (!isAdded()) return;
                        Long up = snap.getLong("upCount");
                        Long down = snap.getLong("downCount");
                        upCount = up != null ? up.intValue() : 0;
                        downCount = down != null ? down.intValue() : 0;

                        // Cập nhật lại số liệu vào model trong cluster
                        if (currentClusterInstance != null) {
                            for (Report r : currentClusterInstance.getReports()) {
                                if (reportId.equals(r.getId())) {
                                    r.setUpCount(upCount);
                                    r.setDownCount(downCount);
                                    break;
                                }
                            }
                            populateClusterCategories();
                        }

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