package com.phatvv9.wwarning.ui.report;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.phatvv9.wwarning.R;
import com.phatvv9.wwarning.data.remote.CategoryRepository;
import com.phatvv9.wwarning.data.remote.ReportRepository;
import com.phatvv9.wwarning.model.Category;
import com.phatvv9.wwarning.worker.ReminderScheduler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuickReportDialog extends Dialog {

    private final ReportRepository repository;
    private final CategoryRepository categoryRepository;
    private final double lat;
    private final double lng;

    private final Map<String, String> categoryMap = new HashMap<>();

    public QuickReportDialog(@NonNull Context context, @NonNull ReportRepository repository, double lat, double lng) {
        super(context);
        this.repository = repository;
        this.categoryRepository = new CategoryRepository(context);
        this.lat = lat;
        this.lng = lng;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_quick_report);

        if (getWindow() != null) {
            getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        loadCategoriesFromRepository();

        findViewById(R.id.btnSunny).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitReportByName("Nắng", "cat_nang");
            }
        });
        findViewById(R.id.btnRain).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitReportByName("Mưa", "cat_mua");
            }
        });
        findViewById(R.id.btnFog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitReportByName("Sương mù", "cat_suong");
            }
        });
        findViewById(R.id.btnStorm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitReportByName("Bão", "cat_bao");
            }
        });
        findViewById(R.id.btnFlood).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitReportByName("Ngập lụt", "cat_ngap");
            }
        });
        findViewById(R.id.btnTree).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitReportByName("Cây đổ", "cat_cay");
            }
        });
        findViewById(R.id.btnCancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }

    private void loadCategoriesFromRepository() {
        categoryRepository.getCategoriesAsync(new CategoryRepository.CategoryCallback() {
            @Override
            public void onCategoriesLoaded(List<Category> categories) {
                if (categories != null) {
                    for (Category cat : categories) {
                        if (cat.getId() != null && cat.getName() != null) {
                            categoryMap.put(cat.getName().toLowerCase().trim(), cat.getId());
                            categoryMap.put(cat.getId().toLowerCase().trim(), cat.getId());
                        }
                    }
                }
            }
        });
    }

    private void submitReportByName(@NonNull String name, @NonNull String fallbackId) {
        String resolvedId = fallbackId;
        String key = name.toLowerCase().trim();
        if (categoryMap.containsKey(key)) {
            resolvedId = categoryMap.get(key);
        }

        repository.createReport(resolvedId, lat, lng, new ReportRepository.ReportCallback() {
            @Override
            public void onSuccess(@NonNull String reportId) {
                ReminderScheduler.requestNotificationPermissionIfNeeded(getContext());
                ReminderScheduler.schedule(getContext(), reportId);
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
