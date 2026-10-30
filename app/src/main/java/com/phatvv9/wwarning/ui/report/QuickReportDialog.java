package com.phatvv9.wwarning.ui.report;

import android.app.Dialog;
import androidx.appcompat.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.phatvv9.wwarning.R;
import com.phatvv9.wwarning.data.remote.CategoryRepository;
import com.phatvv9.wwarning.data.remote.ReportRepository;
import com.phatvv9.wwarning.model.Category;
import com.phatvv9.wwarning.worker.ReminderScheduler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuickReportDialog extends Dialog {
    public interface PhotoCaptureListener {
        void onCapturePhotoRequested(@NonNull String reportId);
    }
    private final PhotoCaptureListener photoCaptureListener;

    private final ReportRepository repository;
    private final CategoryRepository categoryRepository;
    private final double lat;
    private final double lng;

    private final Map<String, String> categoryMap = new HashMap<>();

    public QuickReportDialog(@NonNull Context context, @NonNull ReportRepository repository, double lat, double lng) {
        this(context, repository, lat, lng, null);
    }

    public QuickReportDialog(@NonNull Context context, @NonNull ReportRepository repository, double lat, double lng,
                             @Nullable PhotoCaptureListener photoCaptureListener) {
        super(context);
        this.photoCaptureListener = photoCaptureListener;
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
        findViewById(R.id.btnOvercast).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitReportByName("Âm u", "cat_am_u");
            }
        });
        findViewById(R.id.btnCancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }

    /**
     * On a short landscape screen, the six weather choices are taller than the
     * default dialog window. Give the ScrollView a bounded window so users can
     * scroll to every choice instead of the lower rows being clipped.
     */
    @Override
    protected void onStart() {
        super.onStart();

        Window window = getWindow();
        if (window == null) return;

        boolean landscape = getContext().getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
        int screenWidth = getContext().getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getContext().getResources().getDisplayMetrics().heightPixels;
        int width = (int) (screenWidth * (landscape ? 0.78f : 0.92f));
        int height = landscape
                ? (int) (screenHeight * 0.90f)
                : WindowManager.LayoutParams.WRAP_CONTENT;

        window.setLayout(width, height);
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
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
        final Context context = getContext();
        final Context appContext = context.getApplicationContext();

        String resolvedId = fallbackId;
        String key = name.toLowerCase().trim();
        if (categoryMap.containsKey(key)) {
            resolvedId = categoryMap.get(key);
        }

        if (!com.phatvv9.wwarning.data.sync.SyncManager.getInstance(appContext).isOnline()) {
            Toast.makeText(appContext,
                    "Đang offline: báo cáo sẽ được gửi khi có mạng", Toast.LENGTH_LONG).show();
        }

        repository.createReport(resolvedId, lat, lng, new ReportRepository.ReportCallback() {
            @Override
            public void onSuccess(@NonNull String reportId) {
                ReminderScheduler.requestNotificationPermissionIfNeeded(context);
                ReminderScheduler.schedule(appContext, reportId);
                Toast.makeText(appContext, "Đã gửi cảnh báo thời tiết thành công!", Toast.LENGTH_SHORT).show();
                if (photoCaptureListener != null) {
                    new AlertDialog.Builder(context)
                            .setTitle("Thêm ảnh cho báo cáo?")
                            .setMessage("Bạn có muốn chụp ảnh thực tế tại vị trí này không?")
                            .setNegativeButton("Để sau", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Người dùng bỏ qua bước chụp ảnh.
                                }
                            })
                            .setPositiveButton("Chụp ảnh", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    photoCaptureListener.onCapturePhotoRequested(reportId);
                                }
                            })
                            .show();
                }
            }

            @Override
            public void onError(@NonNull Exception e) {
                Toast.makeText(appContext, "Gửi cảnh báo thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        dismiss();
    }
}
