package com.phatvv9.wwarning;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseUser;
import com.phatvv9.wwarning.data.remote.AuthRepository;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main),
                new OnApplyWindowInsetsListener() {
                    @NonNull
                    @Override
                    public WindowInsetsCompat onApplyWindowInsets(@NonNull View v,
                                                                  @NonNull WindowInsetsCompat insets) {
                        Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                        v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                        return insets;
                    }
                });

        new AuthRepository().signInAnonymously(new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(@NonNull FirebaseUser user) {
                Log.d("WWarning", "uid=" + user.getUid());
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e("WWarning", "Đăng nhập ẩn danh lỗi", e);
            }
        });
    }
}