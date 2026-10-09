package com.phatvv9.wwarning.ui.report;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;

import androidx.annotation.NonNull;

import com.phatvv9.wwarning.R;
import com.phatvv9.wwarning.data.remote.ReportRepository;

public class QuickReportDialog extends Dialog {

    private final ReportRepository repository;
    private final double lat;
    private final double lng;

    public QuickReportDialog(@NonNull Context context, ReportRepository repository, double lat, double lng) {
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

        findViewById(R.id.btnSunny).setOnClickListener(v -> submitReport("Sunny"));
        findViewById(R.id.btnRain).setOnClickListener(v -> submitReport("Rain"));
        findViewById(R.id.btnFog).setOnClickListener(v -> submitReport("Fog"));
        findViewById(R.id.btnStorm).setOnClickListener(v -> submitReport("Storm"));
        findViewById(R.id.btnFlood).setOnClickListener(v -> submitReport("Flood"));
        findViewById(R.id.btnTree).setOnClickListener(v -> submitReport("FallenTree"));
    }

    private void submitReport(String categoryId) {
        repository.createReport(categoryId, lat, lng);
        dismiss();
    }
}
