package com.phatvv9.wwarning.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseUser;
import com.phatvv9.wwarning.MainActivity;
import com.phatvv9.wwarning.data.remote.AuthRepository;
import com.phatvv9.wwarning.databinding.ActivityLoginBinding;

/**
 * Màn hình đầu tiên. Đã có phiên đăng nhập (ẩn danh hoặc Google) thì vào thẳng MainActivity.
 * users/{uid} và user_devices do MainActivity tạo sau khi vào.
 */
public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private final AuthRepository authRepository = new AuthRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (authRepository.currentUser() != null) {
            openMain();
            return;
        }

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnGuest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signInAsGuest();
            }
        });
        binding.btnGoogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signInWithGoogle();
            }
        });
    }

    private void signInAsGuest() {
        setLoading(true);
        authRepository.signInAnonymously(new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(@NonNull FirebaseUser user) {
                openMain();
            }

            @Override
            public void onError(@NonNull Exception e) {
                setLoading(false);
                showError("Không đăng nhập được: " + e.getMessage());
            }
        });
    }

    private void signInWithGoogle() {
        setLoading(true);
        authRepository.signInWithGoogle(this, new AuthRepository.GoogleCallback() {
            @Override
            public void onSuccess(@NonNull FirebaseUser user, boolean switchedAccount) {
                openMain();
            }

            @Override
            public void onCancelled() {
                setLoading(false);
            }

            @Override
            public void onError(@NonNull String message) {
                setLoading(false);
                showError(message);
            }
        });
    }

    private void setLoading(boolean loading) {
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnGuest.setEnabled(!loading);
        binding.btnGoogle.setEnabled(!loading);
        if (loading) {
            binding.tvError.setVisibility(View.GONE);
        }
    }

    private void showError(@NonNull String message) {
        binding.tvError.setText(message);
        binding.tvError.setVisibility(View.VISIBLE);
    }

    private void openMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
