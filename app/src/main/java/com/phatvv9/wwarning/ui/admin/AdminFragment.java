package com.phatvv9.wwarning.ui.admin;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseUser;
import com.phatvv9.wwarning.R;
import com.phatvv9.wwarning.data.remote.AdminRepository;
import com.phatvv9.wwarning.data.remote.ModerationRepository;
import com.phatvv9.wwarning.data.remote.UserRepository;
import com.phatvv9.wwarning.model.BannedUser;
import com.phatvv9.wwarning.model.Report;
import com.phatvv9.wwarning.model.UserProfile;
import com.phatvv9.wwarning.ui.report.ReportDetailSheet;
import com.phatvv9.wwarning.util.FirestoreNames;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Màn Admin: danh sách báo cáo bị báo sai nhiều và quản lý danh sách tài khoản bị khóa.
 * Vuốt trái = ẩn, vuốt phải = khôi phục, nhấn giữ = xóa, chạm = xem chi tiết.
 */
public class AdminFragment extends Fragment {

    private RecyclerView rvReports;
    private TextView tvMessage;
    private MaterialButton btnViewBannedUsers;
    private AdminReportAdapter adapter;
    private final ModerationRepository moderationRepository = new ModerationRepository();
    private final AdminRepository adminRepository = new AdminRepository();
    private boolean listening = false;

    private static final int TAB_VISIBLE = 0;
    private static final int TAB_HIDDEN = 1;
    private static final int TAB_DELETED = 2;
    private static final String[] TAB_NAMES = {"Hiển thị", "Đã ẩn", "Đã xóa"};
    private static final String[] TAB_HINTS = {
            "← Vuốt trái: Ẩn   |   Nhấn giữ: Xóa",
            "Vuốt phải: Khôi phục →",
            "Vuốt phải: Khôi phục →"
    };
    private TabLayout tabFilter;
    private TextView tvSubtitle;
    private int currentTab = TAB_VISIBLE;
    /** Toàn bộ báo cáo từ Firestore; adapter chỉ nhận danh sách đã lọc theo tab. */
    private final List<Report> allReports = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvReports = view.findViewById(R.id.rvAdminReports);
        tvMessage = view.findViewById(R.id.tvAdminMessage);
        btnViewBannedUsers = view.findViewById(R.id.btnViewBannedUsers);
        tabFilter = view.findViewById(R.id.tabAdminFilter);
        tvSubtitle = view.findViewById(R.id.tvAdminSubtitle);

        tabFilter.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                tvSubtitle.setText(TAB_HINTS[currentTab]);
                applyFilter();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        tvSubtitle.setText(TAB_HINTS[currentTab]);

        ViewCompat.setOnApplyWindowInsetsListener(view, new OnApplyWindowInsetsListener() {
            @NonNull
            @Override
            public WindowInsetsCompat onApplyWindowInsets(@NonNull View v, @NonNull WindowInsetsCompat insets) {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), systemBars.top + (int) (8 * getResources().getDisplayMetrics().density), v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            }
        });

        btnViewBannedUsers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showBannedUsersDialog();
            }
        });

        adapter = new AdminReportAdapter(new AdminReportAdapter.Listener() {
            @Override
            public void onReportClick(@NonNull Report report) {
                ReportDetailSheet.newInstance(report)
                        .show(getChildFragmentManager(), "ReportDetailSheet");
            }

            @Override
            public void onReportLongClick(@NonNull Report report) {
                if (currentTab == TAB_DELETED) return;
                confirmDelete(report);
            }
        });
        rvReports.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvReports.setAdapter(adapter);
        attachSwipe();

        showMessage("Đang kiểm tra quyền admin...");
        UserRepository.getInstance().getCurrentProfile()
                .observe(getViewLifecycleOwner(), new Observer<UserProfile>() {
                    @Override
                    public void onChanged(UserProfile profile) {
                        boolean isAdmin = profile != null && profile.isAdmin();
                        if (isAdmin) {
                            btnViewBannedUsers.setVisibility(View.VISIBLE);
                            tabFilter.setVisibility(View.VISIBLE);
                            startListening();
                        } else {
                            btnViewBannedUsers.setVisibility(View.GONE);
                            tabFilter.setVisibility(View.GONE);
                            stopListening();
                            allReports.clear();
                            adapter.setReports(new ArrayList<Report>());
                            showMessage(profile == null
                                    ? "Đang tải hồ sơ..."
                                    : "Chỉ tài khoản admin mới xem được màn này");
                        }
                    }
                });
    }

    /**
     * Hiển thị Dialog danh sách tài khoản bị cấm (Banned Users) kèm chức năng bỏ khóa
     */
    private void showBannedUsersDialog() {
        adminRepository.getBannedUsers(new AdminRepository.BannedUsersCallback() {
            @Override
            public void onSuccess(@NonNull final List<BannedUser> bannedUsers) {
                if (!isAdded()) return;

                if (bannedUsers.isEmpty()) {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Danh sách bị khóa")
                            .setMessage("Hiện không có tài khoản nào bị khóa.")
                            .setPositiveButton("Đóng", null)
                            .show();
                    return;
                }

                String[] items = new String[bannedUsers.size()];
                for (int i = 0; i < bannedUsers.size(); i++) {
                    BannedUser b = bannedUsers.get(i);
                    String reason = b.getReason() != null ? b.getReason() : "Vi phạm quy tắc";
                    items[i] = "UID: " + b.getUserId() + "\nLý do: " + reason;
                }

                new AlertDialog.Builder(requireContext())
                        .setTitle("Tài khoản bị khóa (" + bannedUsers.size() + ")")
                        .setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                final BannedUser selectedUser = bannedUsers.get(which);
                                confirmUnbanUser(selectedUser.getUserId());
                            }
                        })
                        .setPositiveButton("Đóng", null)
                        .show();
            }

            @Override
            public void onError(String error) {
                toast("Lỗi tải danh sách khóa: " + error);
            }
        });
    }

    private void confirmUnbanUser(final String userId) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Bỏ khóa tài khoản?")
                .setMessage("Bạn có chắc muốn mở khóa cho tài khoản: " + userId + "?")
                .setPositiveButton("Mở khóa", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        adminRepository.unbanUser(userId, new AdminRepository.AdminCallback() {
                            @Override
                            public void onSuccess() {
                                toast("Đã mở khóa thành công!");
                            }

                            @Override
                            public void onError(String error) {
                                toast("Lỗi khi mở khóa: " + error);
                            }
                        });
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void attachSwipe() {
        ItemTouchHelper.SimpleCallback callback =
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView,
                                          @NonNull RecyclerView.ViewHolder viewHolder,
                                          @NonNull RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public int getSwipeDirs(@NonNull RecyclerView recyclerView,
                                            @NonNull RecyclerView.ViewHolder viewHolder) {
                        // Tab Hiển thị: chỉ vuốt trái (ẩn). Tab Đã ẩn / Đã xóa: chỉ vuốt phải (khôi phục).
                        return currentTab == TAB_VISIBLE ? ItemTouchHelper.LEFT : ItemTouchHelper.RIGHT;
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                        int position = viewHolder.getBindingAdapterPosition();
                        if (position == RecyclerView.NO_POSITION) return;
                        Report report = adapter.getReportAt(position);
                        adapter.notifyItemChanged(position);
                        if (report == null) return;

                        if (direction == ItemTouchHelper.LEFT) {
                            moderate(report, FirestoreNames.ModerationAction.HIDE,
                                    FirestoreNames.ReportStatus.HIDDEN);
                        } else {
                            moderate(report, FirestoreNames.ModerationAction.RESTORE,
                                    FirestoreNames.ReportStatus.ACTIVE);
                        }
                    }
                };
        new ItemTouchHelper(callback).attachToRecyclerView(rvReports);
    }

    private void confirmDelete(@NonNull final Report report) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xóa báo cáo?")
                .setMessage("Báo cáo sẽ bị gỡ khỏi bản đồ. Bạn vẫn khôi phục được bằng cách vuốt phải.")
                .setPositiveButton("Xóa", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        moderate(report, FirestoreNames.ModerationAction.DELETE,
                                FirestoreNames.ReportStatus.DELETED);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void moderate(@NonNull final Report report, @NonNull final String action,
                          @NonNull final String targetStatus) {
        if (targetStatus.equals(report.getStatus())) {
            toast("Báo cáo đã ở trạng thái này");
            return;
        }
        FirebaseUser admin = FirebaseAuth.getInstance().getCurrentUser();
        String adminUid = admin != null ? admin.getUid() : "admin_demo";
        if (report.getId() == null) return;

        // Cập nhật ngay trên giao diện; nếu server từ chối thì hoàn tác
        final String oldStatus = report.getStatus();
        report.setStatus(targetStatus);
        applyFilter();   // báo cáo tự chuyển sang tab tương ứng

        moderationRepository.moderate(adminUid, report.getId(), action,
                new ModerationRepository.ActionCallback() {
                    @Override
                    public void onSuccess() {
                        if (!isAdded()) return;
                        com.phatvv9.wwarning.data.local.ReportLocalCache.getInstance(requireContext())
                                .updateReportStatus(report.getId(), targetStatus);
                        toast(successMessage(action));
                    }

                    @Override
                    public void onError(@NonNull Exception e) {
                        report.setStatus(oldStatus);
                        applyFilter();
                        if (!isAdded()) return;
                        toast("Không thực hiện được: " + e.getMessage());
                    }
                });
    }

    private static String successMessage(String action) {
        if (FirestoreNames.ModerationAction.HIDE.equals(action)) return "Đã ẩn báo cáo";
        if (FirestoreNames.ModerationAction.DELETE.equals(action)) return "Đã xóa báo cáo";
        return "Đã khôi phục báo cáo";
    }

    private void startListening() {
        if (listening) return;
        listening = true;
        moderationRepository.listenToReports(new ModerationRepository.ReportsCallback() {
            @Override
            public void onReports(@NonNull List<Report> reports) {
                if (!isAdded() || getView() == null) return;
                allReports.clear();
                allReports.addAll(reports);
                applyFilter();
            }

            @Override
            public void onError(@NonNull Exception e) {
                if (!isAdded() || getView() == null) return;
                showMessage("Không tải được danh sách: " + e.getMessage());
            }
        });
    }

    /** Lọc theo tab đang chọn, sắp xếp mới nhất lên đầu rồi đưa vào adapter. */
    private void applyFilter() {
        if (adapter == null || getView() == null) return;

        List<Report> shown = new ArrayList<>();
        int[] counts = new int[3];
        for (Report r : allReports) {
            int tab = tabOf(r);
            counts[tab]++;
            if (tab == currentTab) shown.add(r);
        }

        Collections.sort(shown, new Comparator<Report>() {
            @Override
            public int compare(Report a, Report b) {
                return Long.compare(timeOf(b), timeOf(a));   // mới nhất trước
            }
        });
        adapter.setReports(shown);

        for (int i = 0; i < TAB_NAMES.length; i++) {
            TabLayout.Tab t = tabFilter.getTabAt(i);
            if (t != null) t.setText(TAB_NAMES[i] + " (" + counts[i] + ")");
        }

        if (shown.isEmpty()) {
            showMessage("Không có báo cáo nào trong mục \"" + TAB_NAMES[currentTab] + "\"");
        } else {
            showList();
        }
    }

    private static int tabOf(Report r) {
        String status = r.getStatus();
        if (FirestoreNames.ReportStatus.DELETED.equals(status)) return TAB_DELETED;
        if (FirestoreNames.ReportStatus.HIDDEN.equals(status)) return TAB_HIDDEN;
        return TAB_VISIBLE;
    }

    /** Thời gian tạo (millis) để sắp xếp. createdAt null = vừa gửi, chưa có timestamp từ server -> coi là mới nhất. */
    private static long timeOf(Report r) {
        Timestamp t = r.getCreatedAt();
        return t != null ? t.toDate().getTime() : Long.MAX_VALUE;
    }

    private void stopListening() {
        moderationRepository.stopListening();
        listening = false;
    }

    private void showMessage(String text) {
        tvMessage.setText(text);
        tvMessage.setVisibility(View.VISIBLE);
        rvReports.setVisibility(View.GONE);
    }

    private void showList() {
        tvMessage.setVisibility(View.GONE);
        rvReports.setVisibility(View.VISIBLE);
    }

    private void toast(String text) {
        if (getContext() != null) {
            Toast.makeText(getContext(), text, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopListening();
    }
}