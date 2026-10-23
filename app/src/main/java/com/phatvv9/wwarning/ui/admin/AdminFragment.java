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
import com.google.firebase.auth.FirebaseAuth;
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
                        if (profile != null && profile.isAdmin()) {
                            btnViewBannedUsers.setVisibility(View.VISIBLE);
                            startListening();
                        } else {
                            btnViewBannedUsers.setVisibility(View.GONE);
                            stopListening();
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

    private void moderate(@NonNull Report report, @NonNull final String action,
                          @NonNull String targetStatus) {
        if (targetStatus.equals(report.getStatus())) {
            toast("Báo cáo đã ở trạng thái này");
            return;
        }
        FirebaseUser admin = FirebaseAuth.getInstance().getCurrentUser();
        if (admin == null || report.getId() == null) return;

        moderationRepository.moderate(admin.getUid(), report.getId(), action,
                new ModerationRepository.ActionCallback() {
                    @Override
                    public void onSuccess() {
                        toast(successMessage(action));
                    }

                    @Override
                    public void onError(@NonNull Exception e) {
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
                adapter.setReports(reports);
                if (reports.isEmpty()) {
                    showMessage("Chưa có báo cáo nào");
                } else {
                    showList();
                }
            }

            @Override
            public void onError(@NonNull Exception e) {
                if (!isAdded() || getView() == null) return;
                showMessage("Không tải được danh sách: " + e.getMessage());
            }
        });
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