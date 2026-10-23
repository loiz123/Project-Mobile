package com.phatvv9.wwarning.ui.map;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.Timestamp;
import com.phatvv9.wwarning.R;
import com.phatvv9.wwarning.data.remote.ReportRepository;
import com.phatvv9.wwarning.data.remote.SettingsRepository;
import com.phatvv9.wwarning.data.remote.UserRepository;
import com.phatvv9.wwarning.data.sync.SyncManager;
import com.phatvv9.wwarning.model.Report;
import com.phatvv9.wwarning.ui.report.QuickReportDialog;
import com.phatvv9.wwarning.ui.report.ReportDetailSheet;

import java.util.List;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private static final String KEY_CAMERA_LAT = "camera_target_lat";
    private static final String KEY_CAMERA_LNG = "camera_target_lng";
    private static final String KEY_CAMERA_ZOOM = "camera_zoom";

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private Location currentLocation;
    private LocationCallback locationCallback;
    private ReportRepository reportRepository;
    private boolean isFirstLocationUpdate = true;
    private List<Report> cachedReports;
    private CameraPosition restoredCameraPosition;
    private int statusBarHeight = 0;

    private TextView tvOfflineBanner;
    private ChipGroup chipGroupRiskFilter;
    private boolean isRiskFilterActive = false;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean isGranted) {
                    if (Boolean.TRUE.equals(isGranted)) {
                        enableMyLocation();
                    }
                }
            });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_CAMERA_LAT)) {
            double lat = savedInstanceState.getDouble(KEY_CAMERA_LAT);
            double lng = savedInstanceState.getDouble(KEY_CAMERA_LNG);
            float zoom = savedInstanceState.getFloat(KEY_CAMERA_ZOOM, 15f);
            restoredCameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(lat, lng))
                    .zoom(zoom)
                    .build();
            isFirstLocationUpdate = false; // Đã có góc nhìn camera phục hồi, không tự zoom đè
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        tvOfflineBanner = view.findViewById(R.id.tvOfflineBanner);
        chipGroupRiskFilter = view.findViewById(R.id.chipGroupRiskFilter);

        ViewCompat.setOnApplyWindowInsetsListener(view, new OnApplyWindowInsetsListener() {
            @NonNull
            @Override
            public WindowInsetsCompat onApplyWindowInsets(@NonNull View v, @NonNull WindowInsetsCompat insets) {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                statusBarHeight = systemBars.top;
                updateHeaderInsets(v);
                applyMapPadding();
                return insets;
            }
        });

        reportRepository = new ReportRepository(requireContext());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        view.findViewById(R.id.fabQuickReport).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (UserRepository.getInstance().isBanned()) {
                    Toast.makeText(requireContext(), "Tài khoản của bạn đã bị khóa, không thể gửi báo cáo", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (currentLocation != null) {
                    new QuickReportDialog(requireContext(), reportRepository,
                            currentLocation.getLatitude(), currentLocation.getLongitude()).show();
                } else {
                    Toast.makeText(requireContext(), "Đang xác định vị trí, vui lòng đợi giây lát...", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Theo dõi trạng thái mạng online/offline
        SyncManager.getInstance(requireContext()).getIsOnlineLiveData().observe(getViewLifecycleOwner(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isOnline) {
                if (tvOfflineBanner != null) {
                    tvOfflineBanner.setVisibility(Boolean.TRUE.equals(isOnline) ? View.GONE : View.VISIBLE);
                    updateHeaderInsets(getView());
                    applyMapPadding();
                }
            }
        });

        // Theo dõi bộ lọc rủi ro (ChipGroup)
        if (chipGroupRiskFilter != null) {
            chipGroupRiskFilter.setOnCheckedStateChangeListener(new ChipGroup.OnCheckedStateChangeListener() {
                @Override
                public void onCheckedChanged(@NonNull ChipGroup group, @NonNull List<Integer> checkedIds) {
                    isRiskFilterActive = checkedIds.contains(R.id.chipFilterRisk);
                    if (cachedReports != null) {
                        renderReportMarkers(cachedReports);
                    }
                }
            });
        }

        reportRepository.getReports().observe(getViewLifecycleOwner(), new Observer<List<Report>>() {
            @Override
            public void onChanged(List<Report> reports) {
                cachedReports = reports;
                if (mMap != null && reports != null) {
                    renderReportMarkers(reports);
                }
            }
        });

        view.post(new Runnable() {
            @Override
            public void run() {
                updateHeaderInsets(view);
            }
        });

        return view;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mMap != null) {
            CameraPosition position = mMap.getCameraPosition();
            outState.putDouble(KEY_CAMERA_LAT, position.target.latitude);
            outState.putDouble(KEY_CAMERA_LNG, position.target.longitude);
            outState.putFloat(KEY_CAMERA_ZOOM, position.zoom);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        applyMapPadding();

        if (restoredCameraPosition != null) {
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(restoredCameraPosition));
        }

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(@NonNull Marker marker) {
                Object tag = marker.getTag();
                if (tag instanceof Report) {
                    ReportDetailSheet.newInstance((Report) tag)
                            .show(getChildFragmentManager(), "ReportDetailSheet");
                    return true;
                }
                return false;
            }
        });

        checkLocationPermission();
        if (cachedReports != null) {
            renderReportMarkers(cachedReports);
        }
    }

    private void updateHeaderInsets(@Nullable View v) {
        if (v == null || !isAdded()) return;
        View topContainer = v.findViewById(R.id.topBarContainer);
        if (topContainer == null) return;

        int top = statusBarHeight;
        if (top <= 0) {
            WindowInsetsCompat rootInsets = ViewCompat.getRootWindowInsets(v);
            if (rootInsets != null) {
                top = rootInsets.getInsets(
                        WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.displayCutout()).top;
            }
        }
        if (top <= 0) {
            int resId = getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resId > 0) {
                top = getResources().getDimensionPixelSize(resId);
            }
        }
        if (top <= 0) {
            top = (int) (32 * getResources().getDisplayMetrics().density);
        }

        topContainer.setPadding(0, top, 0, 0);
    }

    private void applyMapPadding() {
        if (mMap == null || !isAdded()) return;
        // Bản đồ đã nằm dưới topBarContainer nên không bị che khuất
        mMap.setPadding(0, 0, 0, 0);
    }

    private void renderReportMarkers(@NonNull List<Report> reports) {
        if (mMap == null) return;
        mMap.clear();

        long now = System.currentTimeMillis();
        int fadeMinutes = SettingsRepository.getInstance().getFadeMinutes();
        int renderedCount = 0;

        for (Report report : reports) {
            // Lọc bỏ báo cáo đã quá hạn hoặc không còn Active
            if (!"Active".equals(report.getStatus())) {
                continue;
            }
            if (report.getExpiresAt() != null && report.getExpiresAt().toDate().getTime() <= now) {
                continue;
            }

            // Lọc theo bộ lọc rủi ro (khi chip "Chỉ rủi ro" được chọn)
            if (isRiskFilterActive && !isRiskCategory(report.getCategoryId())) {
                continue;
            }

            LatLng pos = new LatLng(report.getLatitude(), report.getLongitude());
            String title = getCategoryDisplayName(report.getCategoryId());
            String snippet = getRelativeTime(report.getCreatedAt());

            // Tuần 3: Marker nhiều xác nhận (upCount >= 3) hiển thị nổi bật
            boolean isHighReputation = report.getUpCount() >= 3;
            if (isHighReputation) {
                title = "⭐ " + title + " (" + report.getUpCount() + " xác nhận)";
                snippet = "Cộng đồng xác thực cao • " + snippet;
            }

            // Tuần 3: Marker sắp hết hạn (còn dưới fadeMinutes từ app_settings, mặc định 15 phút) đặt alpha 0.5f làm mờ
            float alpha = 1.0f;
            if (report.getExpiresAt() != null) {
                long remainingMs = report.getExpiresAt().toDate().getTime() - now;
                if (remainingMs > 0 && remainingMs <= fadeMinutes * 60 * 1000L) {
                    alpha = 0.5f;
                    snippet += " (Sắp hết hạn)";
                }
            }

            MarkerOptions options = new MarkerOptions()
                    .position(pos)
                    .title(title)
                    .snippet(snippet)
                    .alpha(alpha)
                    .zIndex(isHighReputation ? 2.0f : 1.0f)
                    .icon(BitmapDescriptorFactory.defaultMarker(getCategoryHue(report.getCategoryId())));

            Marker marker = mMap.addMarker(options);
            if (marker != null) marker.setTag(report);
            renderedCount++;
        }

        if (isRiskFilterActive && renderedCount == 0 && isAdded() && getContext() != null) {
            Toast.makeText(getContext(), "Khu vực quanh bạn an toàn, không có rủi ro nào", Toast.LENGTH_SHORT).show();
        }
    }

    public static boolean isRiskCategory(@Nullable String categoryId) {
        if (categoryId == null) return false;
        switch (categoryId) {
            case "cat_ngap":
            case "cat_bao":
            case "cat_cay":
            case "Flood":
            case "Storm":
            case "FallenTree":
                return true;
            default:
                return false;
        }
    }

    @NonNull
    public static String getCategoryDisplayName(@Nullable String categoryId) {
        if (categoryId == null) return "Cảnh báo thời tiết";
        switch (categoryId) {
            case "cat_nang":
            case "Sunny":
                return "Nắng";
            case "cat_mua":
            case "Rain":
                return "Mưa";
            case "cat_suong":
            case "Fog":
                return "Sương mù";
            case "cat_bao":
            case "Storm":
                return "Bão";
            case "cat_ngap":
            case "Flood":
                return "Ngập lụt";
            case "cat_cay":
            case "FallenTree":
                return "Cây đổ";
            default:
                return categoryId;
        }
    }

    private float getCategoryHue(@Nullable String categoryId) {
        if (categoryId == null) return BitmapDescriptorFactory.HUE_RED;
        switch (categoryId) {
            case "cat_nang":
            case "Sunny":
                return BitmapDescriptorFactory.HUE_ORANGE;
            case "cat_mua":
            case "Rain":
                return BitmapDescriptorFactory.HUE_AZURE;
            case "cat_suong":
            case "Fog":
                return BitmapDescriptorFactory.HUE_CYAN;
            case "cat_bao":
            case "Storm":
            case "cat_ngap":
            case "Flood":
                return BitmapDescriptorFactory.HUE_RED;
            case "cat_cay":
            case "FallenTree":
                return BitmapDescriptorFactory.HUE_VIOLET;
            default:
                return BitmapDescriptorFactory.HUE_RED;
        }
    }

    @NonNull
    public static String getRelativeTime(@Nullable Timestamp timestamp) {
        if (timestamp == null) return "Vừa xong";
        long diffMillis = System.currentTimeMillis() - timestamp.toDate().getTime();
        long diffMinutes = diffMillis / (60 * 1000);
        if (diffMinutes < 1) {
            return "Vừa xong";
        } else if (diffMinutes < 60) {
            return diffMinutes + " phút trước";
        } else {
            return (diffMinutes / 60) + " giờ trước";
        }
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    @SuppressLint("MissingPermission")
    private void enableMyLocation() {
        if (mMap != null) {
            mMap.setMyLocationEnabled(true);

            // 1. Zoom ngay nếu đã có vị trí được cache
            fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        currentLocation = location;
                        // Cập nhật toạ độ GPS cho ReportRepository kích hoạt truy vấn bán kính từ app_settings
                        reportRepository.setLocation(location.getLatitude(), location.getLongitude());

                        if (isFirstLocationUpdate) {
                            isFirstLocationUpdate = false;
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(location.getLatitude(), location.getLongitude()), 15f));
                        }
                    }
                }
            });

            // 2. Lắng nghe cập nhật vị trí thời gian thực (tự động zoom ngay khi có tín hiệu GPS)
            LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                    .setMinUpdateIntervalMillis(2000)
                    .build();

            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    Location location = locationResult.getLastLocation();
                    if (location != null) {
                        currentLocation = location;
                        // Cập nhật toạ độ GPS cho ReportRepository
                        reportRepository.setLocation(location.getLatitude(), location.getLongitude());

                        if (isFirstLocationUpdate) {
                            isFirstLocationUpdate = false;
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(location.getLatitude(), location.getLongitude()), 15f));
                        }
                    }
                }
            };

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (reportRepository != null) {
            reportRepository.stopListening();
        }
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}
