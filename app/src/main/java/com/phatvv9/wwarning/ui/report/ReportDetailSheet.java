package com.phatvv9.wwarning.ui.report;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.phatvv9.wwarning.R;
import com.phatvv9.wwarning.data.remote.AdminRepository;
import com.phatvv9.wwarning.data.remote.ReportImageRepository;
import com.phatvv9.wwarning.data.remote.ReportRepository;
import com.phatvv9.wwarning.data.remote.UserRepository;
import com.phatvv9.wwarning.data.remote.VoteRepository;
import com.phatvv9.wwarning.model.Report;
import com.phatvv9.wwarning.model.ReportCluster;
import com.phatvv9.wwarning.model.UserProfile;
import com.phatvv9.wwarning.ui.map.MapFragment;

import java.util.ArrayList;
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
    private MaterialButton btnDeleteAllMyInCluster;
    private LinearLayout llClusterCategories;

    private TextView tvCategory;
    private TextView tvTime;
    private TextView tvSeverity;
    private TextView tvAuthor;
    private TextView tvUpvoteCount;
    private TextView tvDownvoteCount;
    private TextView tvVoteHint;
    private Button btnUpvote;
    private Button btnDownvote;
    private Button btnDeleteMyReport;
    private Button btnBanAuthor;
    /** Hồ sơ người đăng của báo cáo đang xem (null nếu chưa tải xong / ẩn danh). */
    private UserProfile authorProfile;

    private final VoteRepository voteRepository = new VoteRepository();
    private final AdminRepository adminRepository = new AdminRepository();

    // Ảnh của báo cáo (collection report_images, mã hóa AES-GCM)
    private final ReportImageRepository imageRepository = new ReportImageRepository();
    private ImageView ivReportImage;
    private Button btnAddImage;
    private boolean hasImage;
    private ActivityResultLauncher<Void> cameraLauncher;
    private int currentUserVote;
    private boolean voteStateLoaded;
    private boolean voteInProgress;

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
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicturePreview(),
                new ActivityResultCallback<Bitmap>() {
                    @Override
                    public void onActivityResult(Bitmap bitmap) {
                        if (bitmap == null || !isAdded()) return;
                        try {
                            Uri uri = com.phatvv9.wwarning.util.CameraImageUtils.savePreview(requireContext(), bitmap);
                            uploadPickedImage(uri);
                        } catch (Exception e) {
                            Toast.makeText(requireContext(), "Không lưu được ảnh chụp: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
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

    /**
     * Landscape has much less vertical space. Expand the sheet and give its
     * ScrollView a fixed viewport so the vote controls remain reachable.
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                if (getResources().getConfiguration().orientation
                        != Configuration.ORIENTATION_LANDSCAPE) {
                    return;
                }

                final FrameLayout bottomSheet = dialog.findViewById(
                        com.google.android.material.R.id.design_bottom_sheet);
                if (bottomSheet == null) return;

                bottomSheet.post(new Runnable() {
                    @Override
                    public void run() {
                        ViewGroup.LayoutParams params = bottomSheet.getLayoutParams();
                        if (params != null) {
                            int screenHeight = getResources().getDisplayMetrics().heightPixels;
                            params.height = (int) (screenHeight * 0.90f);
                            bottomSheet.setLayoutParams(params);
                        }

                        BottomSheetBehavior<FrameLayout> behavior =
                                BottomSheetBehavior.from(bottomSheet);
                        behavior.setSkipCollapsed(true);
                        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    }
                });
            }
        });
        return dialog;
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
        btnDeleteAllMyInCluster = view.findViewById(R.id.btnDeleteAllMyInCluster);
        llClusterCategories = view.findViewById(R.id.llClusterCategories);

        tvCategory = view.findViewById(R.id.tvReportCategory);
        tvTime = view.findViewById(R.id.tvReportTime);
        tvSeverity = view.findViewById(R.id.tvReportSeverity);
        tvAuthor = view.findViewById(R.id.tvReportAuthor);
        tvUpvoteCount = view.findViewById(R.id.tvUpvoteCount);
        tvDownvoteCount = view.findViewById(R.id.tvDownvoteCount);
        tvVoteHint = view.findViewById(R.id.tvVoteHint);
        btnUpvote = view.findViewById(R.id.btnUpvote);
        btnDownvote = view.findViewById(R.id.btnDownvote);
        btnDeleteMyReport = view.findViewById(R.id.btnDeleteMyReport);
        btnBanAuthor = view.findViewById(R.id.btnBanAuthor);
        ivReportImage = view.findViewById(R.id.ivReportImage);
        btnAddImage = view.findViewById(R.id.btnAddImage);
        btnAddImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraLauncher.launch(null);
            }
        });

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
                    tvClusterTitle.setText("🏆 Bảng xếp hạng khu vực 500m (" + currentClusterInstance.getReportCount() + " báo cáo)");
                }
                if (tvClusterSubtitle != null) {
                    tvClusterSubtitle.setText("Điểm = số báo cáo + số vote đúng. Chạm một dòng để xem chi tiết và bình chọn.");
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
        loadCurrentUserVote();
        loadAuthor();
        updateDeleteButton();
        loadReportImage();
    }

    private void populateClusterCategories() {
        if (llClusterCategories == null || currentClusterInstance == null) return;
        llClusterCategories.removeAllViews();
        float density = getResources().getDisplayMetrics().density;
        int pad12 = (int) (12 * density);
        int pad8 = (int) (8 * density);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        final String myUid = currentUser != null ? currentUser.getUid() : null;
        UserProfile currentProfile = UserRepository.getInstance().getCurrentProfile().getValue();
        final boolean isAdmin = currentProfile != null && currentProfile.isAdmin();

        final List<Report> myReportsInCluster = new ArrayList<>();
        for (Report r : currentClusterInstance.getReports()) {
            if (isAdmin || (myUid != null && myUid.equals(r.getUserId()))) {
                myReportsInCluster.add(r);
            }
        }

        if (btnDeleteAllMyInCluster != null) {
            if (!myReportsInCluster.isEmpty()) {
                btnDeleteAllMyInCluster.setVisibility(View.VISIBLE);
                btnDeleteAllMyInCluster.setText(isAdmin
                        ? "Xóa tất cả báo cáo trong khu vực này (" + myReportsInCluster.size() + ")"
                        : "Xóa tất cả báo cáo của tôi trong khu vực này (" + myReportsInCluster.size() + ")");
                btnDeleteAllMyInCluster.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        confirmDeleteAllMyInCluster(myReportsInCluster);
                    }
                });
            } else {
                btnDeleteAllMyInCluster.setVisibility(View.GONE);
            }
        }

        List<ReportCluster.CategoryStat> stats = currentClusterInstance.getCategoryStats();
        for (final ReportCluster.CategoryStat stat : stats) {
            final int rank = stats.indexOf(stat) + 1;
            LinearLayout item = new LinearLayout(requireContext());
            item.setOrientation(LinearLayout.VERTICAL);
            item.setPadding(pad12, pad8, pad12, pad8);

            GradientDrawable gd = new GradientDrawable();
            boolean isCurrentCat = stat.getCategoryId() != null && stat.getCategoryId().equals(categoryId);
            boolean isTop = rank == 1;
            gd.setColor(isCurrentCat ? 0xFFEDE7F6 : (isTop ? 0xFFFFF8E1 : 0xFFF5F6F9));
            gd.setCornerRadius(10 * density);
            gd.setStroke((int) (1 * density),
                    isCurrentCat ? 0xFF7E57C2 : (isTop ? 0xFFFFB300 : 0xFFE0E0E0));
            item.setBackground(gd);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = (int) (6 * density);
            item.setLayoutParams(lp);

            // Dòng 1: [Icon + Tên] ---------- [Số lượng báo cáo] [Nút Xóa nếu là của mình/Admin]
            LinearLayout topRow = new LinearLayout(requireContext());
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView tvName = new TextView(requireContext());
            tvName.setText(rankLabel(rank) + "  " + MapFragment.getCategoryIconAndName(stat.getCategoryId()));
            tvName.setTextSize(15);
            tvName.setTypeface(null, Typeface.BOLD);
            tvName.setTextColor(0xFF212121);
            LinearLayout.LayoutParams lpName = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            topRow.addView(tvName, lpName);

            TextView tvCount = new TextView(requireContext());
            tvCount.setText(stat.getScore() + " điểm");
            tvCount.setTextSize(13);
            tvCount.setTextColor(0xFF757575);
            topRow.addView(tvCount);

            Report myRepInStat = null;
            for (Report r : stat.getReports()) {
                if (isAdmin || (myUid != null && myUid.equals(r.getUserId()))) {
                    myRepInStat = r;
                    break;
                }
            }
            if (myRepInStat != null) {
                final Report toDel = myRepInStat;
                TextView btnQuickDel = new TextView(requireContext());
                btnQuickDel.setText("Xóa");
                btnQuickDel.setTextSize(13);
                btnQuickDel.setTextColor(0xFFD32F2F);
                btnQuickDel.setTypeface(null, Typeface.BOLD);
                btnQuickDel.setPadding((int) (12 * density), (int) (4 * density), (int) (4 * density), (int) (4 * density));
                btnQuickDel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        confirmDeleteSpecific(toDel);
                    }
                });
                topRow.addView(btnQuickDel);
            }

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
            String voteText = "📝 " + stat.getReportCount() + " báo cáo   ·   👍 " + stat.getTotalUpCount() + " vote đúng   ·   👎 " + stat.getTotalDownCount() + " báo sai";
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

    private static String rankLabel(int rank) {
        switch (rank) {
            case 1: return "🥇";
            case 2: return "🥈";
            case 3: return "🥉";
            default: return rank + ".";
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

    /** Tải ảnh của báo cáo đang xem (gọi lại mỗi khi đổi báo cáo trong cụm). */
    private void loadReportImage() {
        if (ivReportImage == null || reportId == null) return;
        final String requested = reportId;
        hasImage = false;
        ivReportImage.setVisibility(View.GONE);
        updateAddImageButton();
        imageRepository.loadImage(requested, new ReportImageRepository.Callback<Bitmap>() {
            @Override
            public void onResult(@Nullable Bitmap bitmap) {
                if (!isAdded() || !requested.equals(reportId)) return;   // đã chuyển sang báo cáo khác
                hasImage = bitmap != null;
                if (bitmap != null) {
                    ivReportImage.setImageBitmap(bitmap);
                    ivReportImage.setVisibility(View.VISIBLE);
                }
                updateAddImageButton();
            }

            @Override
            public void onError(@NonNull String message) {
                if (isAdded()) updateAddImageButton();
            }
        });
    }

    /** Nút "Thêm ảnh" chỉ hiện cho chủ báo cáo khi báo cáo chưa có ảnh. */
    private void updateAddImageButton() {
        if (btnAddImage == null) return;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        boolean isMine = user != null && authorId != null && authorId.equals(user.getUid());
        btnAddImage.setVisibility(isMine && !hasImage ? View.VISIBLE : View.GONE);
    }

    private void uploadPickedImage(@NonNull Uri uri) {
        if (reportId == null) return;
        btnAddImage.setEnabled(false);
        imageRepository.uploadImage(requireContext(), reportId, uri,
                new ReportImageRepository.Callback<Void>() {
                    @Override
                    public void onResult(@Nullable Void unused) {
                        if (!isAdded()) return;
                        btnAddImage.setEnabled(true);
                        Toast.makeText(requireContext(), "Đã thêm ảnh", Toast.LENGTH_SHORT).show();
                        loadReportImage();
                    }

                    @Override
                    public void onError(@NonNull String message) {
                        if (!isAdded()) return;
                        btnAddImage.setEnabled(true);
                        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void updateDeleteButton() {
        if (btnDeleteMyReport == null) return;
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String myUid = currentUser != null ? currentUser.getUid() : null;

        boolean isMyReport = myUid != null && authorId != null && authorId.equals(myUid);
        UserProfile currentProfile = UserRepository.getInstance().getCurrentProfile().getValue();
        boolean isAdmin = currentProfile != null && currentProfile.isAdmin();

        if (isMyReport || isAdmin) {
            btnDeleteMyReport.setVisibility(View.VISIBLE);
            btnDeleteMyReport.setText(isMyReport ? "Xóa báo cáo của tôi" : "Xóa báo cáo (Quyền Admin)");
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
        authorProfile = null;
        updateBanButton();

        if (authorId == null || authorId.isEmpty()) {
            tvAuthor.setText("Người đăng: Khách ẩn danh");
            return;
        }
        final String requestedAuthorId = authorId;
        UserRepository.getInstance().fetchProfile(authorId, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(@NonNull UserProfile profile) {
                if (!isAdded() || tvAuthor == null) return;
                // Người dùng có thể đã chạm sang báo cáo khác trong lúc đang tải
                if (!requestedAuthorId.equals(authorId)) return;
                authorProfile = profile;
                tvAuthor.setText("Người đăng: " + profile.getDisplayName()
                        + " · Uy tín: " + profile.getReputationScore()
                        + (profile.isBanned() ? " · 🚫 Đã bị khóa" : ""));
                updateBanButton();
            }

            @Override
            public void onError(@NonNull Exception e) {
                if (!isAdded() || tvAuthor == null) return;
                tvAuthor.setText("Người đăng: Khách ẩn danh");
            }
        });
    }

    /**
     * Nút "Cấm người đăng": chỉ admin thấy, và chỉ khi người đăng là người khác,
     * không phải admin, chưa bị khóa.
     */
    private void updateBanButton() {
        if (btnBanAuthor == null) return;
        UserProfile me = UserRepository.getInstance().getCurrentProfile().getValue();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String myUid = currentUser != null ? currentUser.getUid() : null;

        boolean canBan = me != null && me.isAdmin()
                && authorProfile != null
                && authorId != null && !authorId.equals(myUid)
                && !authorProfile.isAdmin()
                && !authorProfile.isBanned();
        if (!canBan) {
            btnBanAuthor.setVisibility(View.GONE);
            return;
        }
        btnBanAuthor.setVisibility(View.VISIBLE);
        btnBanAuthor.setEnabled(true);
        btnBanAuthor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showBanReasonDialog();
            }
        });
    }

    /** Hộp thoại nhập lý do; nút "Cấm" chỉ bấm được khi đã nhập lý do. */
    private void showBanReasonDialog() {
        if (!isAdded() || authorId == null) return;
        final String targetUid = authorId;
        final String targetName = authorProfile != null && authorProfile.getDisplayName() != null
                ? authorProfile.getDisplayName() : targetUid;

        float density = getResources().getDisplayMetrics().density;
        int pad = (int) (20 * density);

        final EditText etReason = new EditText(requireContext());
        etReason.setHint("Lý do cấm (bắt buộc)");
        etReason.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        etReason.setMinLines(3);
        etReason.setGravity(Gravity.TOP | Gravity.START);

        LinearLayout container = new LinearLayout(requireContext());
        container.setPadding(pad, (int) (8 * density), pad, 0);
        container.addView(etReason, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Cấm tài khoản?")
                .setMessage("Người đăng: " + targetName
                        + "\nTài khoản bị cấm sẽ không thể đăng báo cáo hay bình chọn.")
                .setView(container)
                .setPositiveButton("Cấm", null)   // gán listener bên dưới để không tự đóng khi thiếu lý do
                .setNegativeButton("Hủy", null)
                .create();
        dialog.show();

        final Button btnConfirm = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        btnConfirm.setEnabled(false);
        etReason.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                btnConfirm.setEnabled(s.toString().trim().length() > 0);
            }
        });
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String reason = etReason.getText().toString().trim();
                if (reason.isEmpty()) {
                    etReason.setError("Vui lòng nhập lý do");
                    return;
                }
                dialog.dismiss();
                banAuthor(targetUid, reason);
            }
        });
    }

    private void banAuthor(@NonNull final String targetUid, @NonNull String reason) {
        FirebaseUser admin = FirebaseAuth.getInstance().getCurrentUser();
        if (admin == null) return;
        if (btnBanAuthor != null) btnBanAuthor.setEnabled(false);

        adminRepository.banUser(targetUid, admin.getUid(), reason, new AdminRepository.AdminCallback() {
            @Override
            public void onSuccess() {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Đã cấm tài khoản", Toast.LENGTH_SHORT).show();
                if (targetUid.equals(authorId)) {
                    if (authorProfile != null) {
                        authorProfile.setStatus(com.phatvv9.wwarning.util.FirestoreNames.UserStatus.BANNED);
                    }
                    loadAuthor();   // tải lại để hiện trạng thái "Đã bị khóa" và ẩn nút
                }
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                if (btnBanAuthor != null) btnBanAuthor.setEnabled(true);
                Toast.makeText(requireContext(), "Không cấm được: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showCounts() {
        if (tvUpvoteCount != null) tvUpvoteCount.setText("Hữu ích: " + upCount);
        if (tvDownvoteCount != null) tvDownvoteCount.setText("Báo sai: " + downCount);
    }

    private boolean isMyReport() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null && user.getUid().equals(authorId);
    }

    private void loadCurrentUserVote() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || reportId == null || isMyReport()) {
            voteStateLoaded = true;
            updateVoteControls();
            return;
        }

        voteStateLoaded = false;
        updateVoteControls();
        voteRepository.getVoteValue(reportId, user.getUid(), new VoteRepository.VoteValueCallback() {
            @Override
            public void onLoaded(int voteValue) {
                if (!isAdded()) return;
                currentUserVote = voteValue;
                voteStateLoaded = true;
                updateVoteControls();
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                voteStateLoaded = true;
                updateVoteControls();
            }
        });
    }

    private void updateVoteControls() {
        if (btnUpvote == null || btnDownvote == null) return;

        if (isMyReport()) {
            btnUpvote.setText("Không thể tự xác nhận");
            btnDownvote.setText("Không thể tự báo sai");
            btnUpvote.setEnabled(false);
            btnDownvote.setEnabled(false);
            if (tvVoteHint != null) tvVoteHint.setText("Bạn không thể bình chọn báo cáo của mình.");
            return;
        }

        btnUpvote.setText(currentUserVote == 1 ? "Đã xác nhận ✓" : "Xác nhận (+1)");
        btnDownvote.setText(currentUserVote == -1 ? "Đã báo sai ✓" : "Báo sai (-1)");
        boolean enabled = voteStateLoaded && !voteInProgress;
        btnUpvote.setEnabled(enabled);
        btnDownvote.setEnabled(enabled);

        if (tvVoteHint != null) {
            if (!voteStateLoaded) {
                tvVoteHint.setText("Đang tải trạng thái bình chọn...");
            } else if (currentUserVote == 1) {
                tvVoteHint.setText("Bạn đã xác nhận. Chạm lại để hủy bình chọn.");
            } else if (currentUserVote == -1) {
                tvVoteHint.setText("Bạn đã báo sai. Chạm lại để hủy bình chọn.");
            } else {
                tvVoteHint.setText("Bình chọn giúp xác thực cảnh báo trong khu vực.");
            }
        }
    }

    // Dùng khi một thao tác khác (ví dụ xóa báo cáo) cần khóa tạm hai nút vote.
    private void setButtonsEnabled(boolean enabled) {
        if (!enabled) {
            if (btnUpvote != null) btnUpvote.setEnabled(false);
            if (btnDownvote != null) btnDownvote.setEnabled(false);
            return;
        }
        updateVoteControls();
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
        if (isMyReport()) {
            Toast.makeText(requireContext(), "Bạn không thể bình chọn báo cáo của mình", Toast.LENGTH_SHORT).show();
            return;
        }

        voteInProgress = true;
        updateVoteControls();
        voteRepository.castVote(reportId, user.getUid(), voteValue, new VoteRepository.VoteCallback() {
            @Override
            public void onSuccess() {
                currentUserVote = currentUserVote == voteValue ? 0 : voteValue;
                voteInProgress = false;
                refreshCounts();
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                voteInProgress = false;
                updateVoteControls();
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
                        updateVoteControls();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if (!isAdded()) return;
                        voteInProgress = false;
                        updateVoteControls();
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

    private void confirmDeleteSpecific(@NonNull final Report report) {
        if (!isAdded() || report.getId() == null) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("Xóa báo cáo")
                .setMessage("Bạn có chắc chắn muốn xóa cảnh báo " + MapFragment.getCategoryIconAndName(report.getCategoryId()) + " này khỏi bản đồ?")
                .setPositiveButton("Xóa", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteSpecific(report.getId());
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void confirmDeleteAllMyInCluster(@NonNull final List<Report> reports) {
        if (!isAdded() || reports.isEmpty()) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("Xóa tất cả báo cáo")
                .setMessage("Bạn có chắc chắn muốn xóa tất cả " + reports.size() + " báo cáo trong khu vực này khỏi bản đồ?")
                .setPositiveButton("Xóa tất cả", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteAllInList(reports);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteSpecific(@NonNull final String idToDelete) {
        setButtonsEnabled(false);
        if (btnDeleteMyReport != null) btnDeleteMyReport.setEnabled(false);

        ReportRepository.getInstance(requireContext()).deleteReport(idToDelete, new ReportRepository.ReportCallback() {
            @Override
            public void onSuccess(@NonNull String id) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Đã xóa báo cáo thành công", Toast.LENGTH_SHORT).show();
                if (currentClusterInstance != null) {
                    currentClusterInstance.removeReport(idToDelete);
                    if (currentClusterInstance.getReportCount() == 0) {
                        dismiss();
                        return;
                    }
                    Report nextPrimary = currentClusterInstance.getPrimaryReport();
                    if (nextPrimary != null) {
                        selectReport(nextPrimary);
                        return;
                    }
                }
                dismiss();
            }

            @Override
            public void onError(@NonNull Exception e) {
                if (!isAdded()) return;
                setButtonsEnabled(true);
                if (btnDeleteMyReport != null) btnDeleteMyReport.setEnabled(true);
                Toast.makeText(requireContext(), "Lỗi khi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteAllInList(@NonNull final List<Report> reports) {
        final ReportRepository repo = ReportRepository.getInstance(requireContext());
        final java.util.concurrent.atomic.AtomicInteger pending =
                new java.util.concurrent.atomic.AtomicInteger(0);
        final java.util.concurrent.atomic.AtomicInteger failed =
                new java.util.concurrent.atomic.AtomicInteger(0);
        for (Report r : reports) {
            if (r.getId() != null) pending.incrementAndGet();
        }
        if (pending.get() == 0) {
            dismiss();
            return;
        }
        for (Report r : reports) {
            if (r.getId() == null) continue;
            repo.deleteReport(r.getId(), new ReportRepository.ReportCallback() {
                @Override
                public void onSuccess(@NonNull String id) {
                    if (currentClusterInstance != null) currentClusterInstance.removeReport(id);
                    finishDeleteAll(pending, failed);
                }

                @Override
                public void onError(@NonNull Exception e) {
                    failed.incrementAndGet();
                    finishDeleteAll(pending, failed);
                }
            });
        }
    }

    private void finishDeleteAll(java.util.concurrent.atomic.AtomicInteger pending,
                                 java.util.concurrent.atomic.AtomicInteger failed) {
        if (pending.decrementAndGet() > 0 || !isAdded()) return;
        if (failed.get() == 0) {
            Toast.makeText(requireContext(), "Đã xóa tất cả báo cáo", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(),
                    "Có " + failed.get() + " báo cáo không xóa được", Toast.LENGTH_LONG).show();
        }
        dismiss();
    }

    private void deleteReport() {
        if (reportId == null) return;
        deleteSpecific(reportId);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        currentClusterInstance = null;
    }
}
