package com.phatvv9.wwarning.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;

import com.google.firebase.auth.FirebaseUser;
import com.phatvv9.wwarning.R;
import com.phatvv9.wwarning.data.remote.AuthRepository;
import com.phatvv9.wwarning.data.remote.UserRepository;
import com.phatvv9.wwarning.model.UserProfile;
import com.phatvv9.wwarning.util.FirestoreNames;

public class ProfileFragment extends Fragment {

    private final UserRepository userRepository = UserRepository.getInstance();
    private final AuthRepository authRepository = new AuthRepository();

    private TextView tvUserName;
    private TextView tvBannedStatus;
    private TextView tvReputationScore;
    private TextView tvContributionsCount;
    private TextView tvAuthProvider;
    private Button btnGoogleSignIn;

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

        btnGoogleSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGoogleSignIn();
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
    }

    private void bindProfile(@NonNull UserProfile profile) {
        if (tvUserName == null) return;
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

        boolean isGoogle = FirestoreNames.AuthProvider.GOOGLE.equals(profile.getAuthProvider());
        tvAuthProvider.setText(isGoogle
                ? "Tài khoản: Google"
                : "Tài khoản: Ẩn danh (chưa liên kết Google)");
        btnGoogleSignIn.setVisibility(isGoogle ? View.GONE : View.VISIBLE);
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
