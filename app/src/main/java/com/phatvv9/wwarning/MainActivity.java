package com.phatvv9.wwarning;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.firebase.auth.FirebaseUser;
import com.phatvv9.wwarning.data.remote.AuthRepository;
import com.phatvv9.wwarning.data.remote.UserRepository;
import com.phatvv9.wwarning.databinding.ActivityMainBinding;
import com.phatvv9.wwarning.model.UserProfile;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private boolean bannedDialogShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(binding.bottomNavigation, navController);
        }

        // Mặc định ẩn tab Admin cho đến khi hồ sơ Firestore xác nhận role = Admin.
        setAdminTabVisible(UserRepository.getInstance().isAdmin());
        observeProfile();

        // Đăng nhập ẩn danh -> đảm bảo có users/{uid} -> ghi thiết bị -> nghe hồ sơ realtime
        new AuthRepository().signInAnonymously(new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(@NonNull FirebaseUser user) {
                onSignedIn(user);
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e("WWarning", "Đăng nhập ẩn danh lỗi", e);
            }
        });
    }

    private void onSignedIn(@NonNull final FirebaseUser user) {
        final UserRepository userRepository = UserRepository.getInstance();
        userRepository.ensureUserProfile(user, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(@NonNull UserProfile profile) {
                userRepository.registerDevice(getApplicationContext(), user.getUid());
                userRepository.startListening(user.getUid());
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e("WWarning", "Không tạo/đọc được users/{uid}", e);
            }
        });
    }

    private void observeProfile() {
        UserRepository.getInstance().getCurrentProfile().observe(this, new Observer<UserProfile>() {
            @Override
            public void onChanged(UserProfile profile) {
                setAdminTabVisible(profile != null && profile.isAdmin());
                if (profile != null && profile.isBanned() && !bannedDialogShown) {
                    bannedDialogShown = true;
                    showBannedDialog();
                }
                if (profile != null && !profile.isBanned()) {
                    bannedDialogShown = false;
                }
            }
        });
    }

    private void showBannedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Tài khoản bị khóa")
                .setMessage("Bạn không thể gửi báo cáo hoặc bình chọn. Bạn vẫn xem được bản đồ.")
                .setPositiveButton("Đã hiểu", null)
                .show();
    }

    private void setAdminTabVisible(boolean visible) {
        Menu menu = binding.bottomNavigation.getMenu();
        MenuItem adminItem = menu.findItem(R.id.adminFragment);
        if (adminItem != null) {
            adminItem.setVisible(visible);
        }
    }
}
