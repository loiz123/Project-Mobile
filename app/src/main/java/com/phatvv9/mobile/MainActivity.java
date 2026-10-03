package com.phatvv9.mobile; // đổi cho đúng package của bạn (xem dòng đầu file cũ)

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        NavigationBarView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_report) {
                startActivity(new Intent(MainActivity.this, ReportActivity.class));
            }
            return true;
        });
        // Chỉ để chuyển qua lại giữa 2 màn hình cho tiện xem giao diện

    }
}