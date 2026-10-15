package com.phatvv9.wwarning.ui.report;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.phatvv9.wwarning.R;
import com.phatvv9.wwarning.data.remote.ReportRepository;

public class QuickReportDialog extends Dialog {

    private final ReportRepository repository;
    private final double lat;
    private final double lng;

    public QuickReportDialog(@NonNull Context context, @NonNull ReportRepository repository, double lat, double lng) {
        super(context);
        this.repository = repository;
        this.lat = lat;
        this.lng = lng;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_quick_report);

        findViewById(R.id.btnSunny).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitReport("cat_nang");
            }
        });
        findViewById(R.id.btnRain).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitReport("cat_mua");
            }
        });
        findViewById(R.id.btnFog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitReport("cat_suong");
            }
        });
        findViewById(R.id.btnStorm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitReport("cat_bao");
            }
        });
        findViewById(R.id.btnFlood).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitReport("cat_ngap");
            }
        });
        findViewById(R.id.btnTree).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitReport("cat_cay");
            }
        });
        findViewById(R.id.btnCancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }

    private void submitReport(@NonNull String categoryId) {
        repository.createReport(categoryId, lat, lng, new ReportRepository.ReportCallback() {
            @Override
            public void onSuccess(@NonNull String reportId) {
                Toast.makeText(getContext(), "Đã gửi cảnh báo thời tiết thành công!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(@NonNull Exception e) {
                Toast.makeText(getContext(), "Gửi cảnh báo thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        dismiss();
    }
}
