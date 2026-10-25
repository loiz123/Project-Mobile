package com.phatvv9.wwarning.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.phatvv9.wwarning.BuildConfig;
import com.phatvv9.wwarning.R;
import com.phatvv9.wwarning.data.remote.AuthRepository;
import com.phatvv9.wwarning.data.remote.ReputationRepository;
import com.phatvv9.wwarning.data.remote.UserRepository;
import com.phatvv9.wwarning.model.ReputationLog;
import com.phatvv9.wwarning.model.UserProfile;
import com.phatvv9.wwarning.ui.login.LoginActivity;
import com.phatvv9.wwarning.util.FirestoreNames;
import com.phatvv9.wwarning.worker.ReminderScheduler;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private final UserRepository userRepository = UserRepository.getInstance();
    private final AuthRepository authRepository = new AuthRepository();
    private final ReputationRepository reputationRepository = new ReputationRepository();

    private TextView tvUserName;
    private TextView tvBannedStatus;
    private TextView tvReputationScore;
    private TextView tvContributionsCount;
    private TextView tvAuthProvider;
    private Button btnGoogleSignIn;
    private Button btnSignOut;
    private Button btnTestNotification;
    private LinearLayout llReputationHistory;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvUserName = view.findViewById(R.id.tvUserName);
        tvBannedStatus = view.findViewById(R.id.tvBannedStatus);
        tvReputationScore = view.findViewById(R.id.tvReputationScore);
        tvContributionsCount = view.findViewById(R.id.tvContributionsCount);
        tvAuthProvider = view.findViewById(R.id.tvAuthProvider);
        btnGoogleSignIn = view.findViewById(R.id.btnGoogleSignIn);
        btnSignOut = view.findViewById(R.id.btnSignOut);
        btnTestNotification = view.findViewById(R.id.btnTestNotification);
        // Nút thử thông báo chỉ dành cho bản debug/demo, ẩn ở bản release
        if (btnTestNotification != null && !BuildConfig.DEBUG) {
            btnTestNotification.setVisibility(View.GONE);
        }
        llReputationHistory = view.findViewById(R.id.llReputationHistory);

        btnGoogleSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGoogleSignIn();
            }
        });

        if (btnSignOut != null) {
            btnSignOut.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    signOut();
                }
            });
        }

        if (btnTestNotification != null) {
            btnTestNotification.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    testNotification();
                }
            });
        }

        ViewCompat.setOnApplyWindowInsetsListener(view, new OnApplyWindowInsetsListener() {
            @NonNull
            @Override
            public WindowInsetsCompat onApplyWindowInsets(@NonNull View v, @NonNull WindowInsetsCompat insets) {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), systemBars.top + (int) (12 * getResources().getDisplayMetrics().density), v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            }
        });

        userRepository.getCurrentProfile().observe(getViewLifecycleOwner(), new Observer<UserProfile>() {
            @Override
            public void onChanged(UserProfile profile) {
                if (profile != null) {
                    bindProfile(profile);
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadReportCount();
        loadHistory();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        tvUserName = null;
        tvBannedStatus = null;
        tvReputationScore = null;
        tvContributionsCount = null;
        tvAuthProvider = null;
        btnGoogleSignIn = null;
        btnSignOut = null;
        btnTestNotification = null;
        llReputationHistory = null;
    }

    private void bindProfile(@NonNull UserProfile profile) {
        if (tvUserName == null) return;
        boolean isAdmin = profile.isAdmin();

        if (isAdmin) {
            tvUserName.setText("Quản trị viên (Admin)");
            tvReputationScore.setText(String.valueOf(profile.getReputationScore()));
            tvBannedStatus.setText("👑 Vai trò: Quản trị viên (Admin)");
            tvBannedStatus.setTextColor(ContextCompat.getColor(
                    requireContext(), android.R.color.holo_purple));
            tvAuthProvider.setText("Tài khoản: Quản trị viên");
            btnGoogleSignIn.setVisibility(View.GONE);
            return;
        }

        tvUserName.setText(profile.getDisplayName());
        tvReputationScore.setText(String.valueOf(profile.getReputationScore()));

        if (profile.isBanned()) {
            tvBannedStatus.setText("Trạng thái: Đã bị khóa");
            tvBannedStatus.setTextColor(ContextCompat.getColor(
                    requireContext(), android.R.color.holo_red_dark));
        } else {
            tvBannedStatus.setText("Trạng thái: Hoạt động");
            tvBannedStatus.setTextColor(ContextCompat.getColor(
                    requireContext(), android.R.color.holo_green_dark));
        }

        if (FirestoreNames.AuthProvider.GOOGLE.equals(profile.getAuthProvider())) {
            tvAuthProvider.setText("Tài khoản: Google");
            btnGoogleSignIn.setVisibility(View.GONE);
        } else if (FirestoreNames.AuthProvider.EMAIL.equals(profile.getAuthProvider())) {
            tvAuthProvider.setText("Tài khoản: Email");
            btnGoogleSignIn.setVisibility(View.GONE);
        } else {
            tvAuthProvider.setText("Tài khoản: Ẩn danh (chưa liên kết Google)");
            btnGoogleSignIn.setVisibility(View.VISIBLE);
        }
    }

    private void signOut() {
        userRepository.clearCurrentProfile();
        authRepository.signOut();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    private void testNotification() {
        ReminderScheduler.requestNotificationPermissionIfNeeded(requireContext());
        Toast.makeText(requireContext(), "Thông báo nhắc nhở 15 phút sẽ xuất hiện sau giây lát...", Toast.LENGTH_SHORT).show();
        ReminderScheduler.scheduleTestReminder(requireContext(), 5);
    }

    private void loadReportCount() {
        String uid = authRepository.currentUid();
        if (uid == null) return;
        userRepository.countMyReports(uid, new UserRepository.CountCallback() {
            @Override
            public void onCount(long count) {
                if (tvContributionsCount != null) {
                    tvContributionsCount.setText(String.valueOf(count));
                }
            }

            @Override
            public void onError(@NonNull Exception e) {
                if (tvContributionsCount != null) {
                    tvContributionsCount.setText("-");
                }
            }
        });
    }

    // ---- Lịch sử biến động điểm (reputation_logs) ----

    private void loadHistory() {
        String uid = authRepository.currentUid();
        if (uid == null || llReputationHistory == null) return;
        reputationRepository.loadRecentLogs(uid, 10, new ReputationRepository.LogsCallback() {
            @Override
            public void onLogs(@NonNull List<ReputationLog> logs) {
                if (!isAdded() || llReputationHistory == null) return;
                showHistory(logs);
            }

            @Override
            public void onError(@NonNull Exception e) {
                if (!isAdded() || llReputationHistory == null) return;
                llReputationHistory.removeAllViews();
                addHistoryRow("Không tải được lịch sử điểm", android.R.color.darker_gray);
            }
        });
    }

    private void showHistory(@NonNull List<ReputationLog> logs) {
        llReputationHistory.removeAllViews();
        if (logs.isEmpty()) {
            addHistoryRow("Chưa có biến động điểm", android.R.color.darker_gray);
            return;
        }
        SimpleDateFormat format = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());
        for (ReputationLog log : logs) {
            Timestamp createdAt = log.getCreatedAt();
            String time = createdAt != null ? format.format(createdAt.toDate()) : "vừa xong";
            String text = String.format(Locale.getDefault(), "%+d   %s   ·   %s",
                    log.getDelta(), log.getReason(), time);
            addHistoryRow(text, log.getDelta() >= 0
                    ? android.R.color.holo_green_dark
                    : android.R.color.holo_red_dark);
        }
    }

    private void addHistoryRow(@NonNull String text, int colorRes) {
        TextView row = new TextView(requireContext());
        row.setText(text);
        row.setTextSize(14);
        row.setTextColor(ContextCompat.getColor(requireContext(), colorRes));
        int pad = (int) (4 * getResources().getDisplayMetrics().density);
        row.setPadding(0, pad, 0, pad);
        llReputationHistory.addView(row);
    }

    private void startGoogleSignIn() {
        btnGoogleSignIn.setEnabled(false);
        authRepository.signInWithGoogle(requireActivity(), new AuthRepository.GoogleCallback() {
            @Override
            public void onSuccess(@NonNull FirebaseUser user, boolean switchedAccount) {
                if (!isAdded()) return;
                onGoogleSuccess(user, switchedAccount);
            }

            @Override
            public void onCancelled() {
                if (btnGoogleSignIn != null) btnGoogleSignIn.setEnabled(true);
            }

            @Override
            public void onError(@NonNull String message) {
                if (btnGoogleSignIn != null) btnGoogleSignIn.setEnabled(true);
                if (isAdded()) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void onGoogleSuccess(@NonNull final FirebaseUser user, final boolean switchedAccount) {
        // Uid có thể đã đổi (đăng nhập vào tài khoản Google có sẵn), nên đảm bảo users/{uid} rồi mới nghe lại
        userRepository.ensureUserProfile(user, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(@NonNull UserProfile profile) {
                userRepository.markLinkedWithGoogle(user, new UserRepository.DoneCallback() {
                    @Override
                    public void onDone() {
                        userRepository.startListening(user.getUid());
                        loadReportCount();
                        if (isAdded()) {
                            Toast.makeText(requireContext(),
                                    switchedAccount
                                            ? "Đã đăng nhập vào tài khoản Google có sẵn (báo cáo ẩn danh cũ không chuyển sang)"
                                            : "Đã liên kết tài khoản Google",
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onError(@NonNull Exception e) {
                        showError("Liên kết xong nhưng chưa cập nhật được hồ sơ");
                    }
                });
            }

            @Override
            public void onError(@NonNull Exception e) {
                showError("Không đọc được hồ sơ sau khi đăng nhập");
            }
        });
    }

    private void showError(@NonNull String message) {
        if (btnGoogleSignIn != null) btnGoogleSignIn.setEnabled(true);
        if (isAdded()) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        }
    }
}
