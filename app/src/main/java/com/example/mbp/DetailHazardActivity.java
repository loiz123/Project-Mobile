package com.example.mbp;

import android.graphics.Color;
import android.os.Bundle;

import android.view.Window;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

public class DetailHazardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Dark Status Bar matching background
        Window window = getWindow();
        window.setStatusBarColor(Color.parseColor("#0B132B"));

        setContentView(R.layout.activity_detail_hazard);

        // Nút quay lại
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }
}