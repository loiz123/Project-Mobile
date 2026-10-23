package com.phatvv9.wwarning.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseUser;
import com.phatvv9.wwarning.MainActivity;
import com.phatvv9.wwarning.R;
import com.phatvv9.wwarning.data.remote.AuthRepository;
import com.phatvv9.wwarning.databinding.ActivityLoginBinding;

/**
 * Màn hình đầu tiên. Đã có phiên đăng nhập (ẩn danh, Google hoặc Email Admin) thì vào thẳng MainActivity.
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
        binding.btnAdminLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAdminLoginDialog();
            }
        });
    }

    private void showAdminLoginDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_admin_login, null);
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        final TextInputEditText etEmail = dialogView.findViewById(R.id.etAdminEmail);
        final TextInputEditText etPassword = dialogView.findViewById(R.id.etAdminPassword);
        final TextView tvError = dialogView.findViewById(R.id.tvAdminLoginError);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelAdmin);
        final Button btnSubmit = dialogView.findViewById(R.id.btnSubmitAdmin);

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
                String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

                if (email.isEmpty() || password.isEmpty()) {
                    tvError.setText("Vui lòng nhập đầy đủ email và mật khẩu");
                    tvError.setVisibility(View.VISIBLE);
                    return;
                }

                btnSubmit.setEnabled(false);
                tvError.setVisibility(View.GONE);

                authRepository.signInWithEmailAndPassword(email, password, new AuthRepository.AuthCallback() {
                    @Override
                    public void onSuccess(@NonNull FirebaseUser user) {
                        com.phatvv9.wwarning.util.AdminSessionManager.setAdmin(LoginActivity.this, true);
                        dialog.dismiss();
                        openMain();
                    }

                    @Override
                    public void onError(@NonNull Exception e) {
                        btnSubmit.setEnabled(true);
                        tvError.setText("Đăng nhập thất bại: " + e.getMessage());
                        tvError.setVisibility(View.VISIBLE);
                    }
                });
            }
        });

        dialog.show();
    }

    private void signInAsGuest() {
        setLoading(true);
        com.phatvv9.wwarning.util.AdminSessionManager.setAdmin(this, false);
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
        com.phatvv9.wwarning.util.AdminSessionManager.setAdmin(this, false);
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
        binding.btnAdminLogin.setEnabled(!loading);
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
