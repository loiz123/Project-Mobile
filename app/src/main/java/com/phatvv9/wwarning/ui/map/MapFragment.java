package com.phatvv9.wwarning.ui.map;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;
import java.util.concurrent.Executors;

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
import com.phatvv9.wwarning.data.remote.ReportImageRepository;
import com.phatvv9.wwarning.data.remote.ReportRepository;
import com.phatvv9.wwarning.data.remote.ReportImageRepository;
import com.phatvv9.wwarning.data.remote.SettingsRepository;
import com.phatvv9.wwarning.data.remote.UserRepository;
import com.phatvv9.wwarning.data.sync.SyncManager;
import com.phatvv9.wwarning.model.Report;
import com.phatvv9.wwarning.model.ReportCluster;
import com.phatvv9.wwarning.ui.report.QuickReportDialog;
import com.phatvv9.wwarning.ui.report.ClusterRankingSheet;
import com.phatvv9.wwarning.ui.report.ReportDetailSheet;
import com.phatvv9.wwarning.util.GeoUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

    private EditText etSearchLocation;
    private ImageView btnClearSearch;
    private Marker searchLocationMarker;
    private String pendingPhotoReportId;
    private final ReportImageRepository reportImageRepository = new ReportImageRepository();
    private final ActivityResultLauncher<Void> cameraPreviewLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(),
                    new ActivityResultCallback<Bitmap>() {
                        @Override public void onActivityResult(Bitmap bitmap) {
                            if (bitmap == null || pendingPhotoReportId == null || !isAdded()) return;
                            final String reportId = pendingPhotoReportId;
                            pendingPhotoReportId = null;
                            try {
                                Uri imageUri = com.phatvv9.wwarning.util.CameraImageUtils.savePreview(requireContext(), bitmap);
                                reportImageRepository.uploadImage(requireContext(), reportId, imageUri,
                                        new ReportImageRepository.Callback<Void>() {
                                            @Override public void onResult(@Nullable Void value) {
                                                if (isAdded()) Toast.makeText(requireContext(), "Đã thêm ảnh cho báo cáo", Toast.LENGTH_SHORT).show();
                                            }
                                            @Override public void onError(@NonNull String message) {
                                                if (isAdded()) Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                                            }
                                        });
                            } catch (Exception e) {
                                Toast.makeText(requireContext(), "Không lưu được ảnh: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    });

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
        etSearchLocation = view.findViewById(R.id.etSearchLocation);
        btnClearSearch = view.findViewById(R.id.btnClearSearch);
        ImageView ivSearchIcon = view.findViewById(R.id.ivSearchIcon);

        if (etSearchLocation != null) {
            etSearchLocation.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_SEARCH
                            || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                        performSearch(v.getText().toString().trim());
                        hideKeyboard(v);
                        return true;
                    }
                    return false;
                }
            });

            etSearchLocation.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (btnClearSearch != null) {
                        btnClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                    }
                }
                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        if (btnClearSearch != null) {
            btnClearSearch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (etSearchLocation != null) {
                        etSearchLocation.setText("");
                    }
                }
            });
        }

        if (ivSearchIcon != null) {
            ivSearchIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (etSearchLocation != null) {
                        performSearch(etSearchLocation.getText().toString().trim());
                        hideKeyboard(etSearchLocation);
                    }
                }
            });
        }

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

        reportRepository = ReportRepository.getInstance(requireContext());
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
                            currentLocation.getLatitude(), currentLocation.getLongitude(),
                            new QuickReportDialog.PhotoCaptureListener() {
                                @Override public void onCapturePhotoRequested(@NonNull String reportId) {
                                    pendingPhotoReportId = reportId;
                                    cameraPreviewLauncher.launch(null);
                                }
                            }).show();
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
                if (tag instanceof ReportCluster) {
                    ReportCluster cluster = (ReportCluster) tag;
                    if (cluster.getReportCount() > 1) {
                        // Cụm nhiều báo cáo: mở overlay bảng xếp hạng, không hiện info window
                        ClusterRankingSheet.newInstance(cluster)
                                .show(getChildFragmentManager(), "ClusterRankingSheet");
                    } else {
                        marker.showInfoWindow();
                        ReportDetailSheet.newInstance(cluster)
                                .show(getChildFragmentManager(), "ReportDetailSheet");
                    }
                    return true;
                } else if (tag instanceof Report) {
                    marker.showInfoWindow();
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

    private void updateHeaderInsets(@Nullable final View v) {
        if (v == null || !isAdded()) return;
        final View topContainer = v.findViewById(R.id.topBarContainer);
        if (topContainer == null) return;

        topContainer.post(new Runnable() {
            @Override
            public void run() {
                if (!isAdded()) return;
                int topBarHeight = statusBarHeight;
                if (topBarHeight <= 0) {
                    WindowInsetsCompat rootInsets = ViewCompat.getRootWindowInsets(v);
                    if (rootInsets != null) {
                        topBarHeight = rootInsets.getInsets(
                                WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.displayCutout()).top;
                    }
                }
                if (topBarHeight <= 0) {
                    int resId = getResources().getIdentifier("status_bar_height", "dimen", "android");
                    if (resId > 0) {
                        topBarHeight = getResources().getDimensionPixelSize(resId);
                    }
                }

                int[] loc = new int[2];
                topContainer.getLocationOnScreen(loc);
                int screenY = loc[1]; // Vị trí Y thực tế trên màn hình
                int extraSpace = (int) (12 * getResources().getDisplayMetrics().density);
                int neededPadding = Math.max(0, topBarHeight - screenY) + extraSpace;

                topContainer.setPadding(0, neededPadding, 0, 0);
                applyMapPadding();
            }
        });
    }

    private void applyMapPadding() {
        if (mMap == null || !isAdded()) return;
        View view = getView();
        View topContainer = view != null ? view.findViewById(R.id.topBarContainer) : null;
        int topPadding = (topContainer != null && topContainer.getHeight() > 0) ? topContainer.getHeight() : 0;
        int extra = (int) (8 * getResources().getDisplayMetrics().density);
        mMap.setPadding(0, topPadding + extra, 0, 0);
    }

    private void performSearch(@NonNull final String query) {
        if (query.isEmpty()) {
            Toast.makeText(requireContext(), "Vui lòng nhập địa điểm cần tìm", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(requireContext(), "Đang tìm kiếm: " + query + "...", Toast.LENGTH_SHORT).show();

        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
                    List<Address> addresses = geocoder.getFromLocationName(query, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        final Address address = addresses.get(0);
                        final LatLng target = new LatLng(address.getLatitude(), address.getLongitude());
                        final String displayName = address.getAddressLine(0) != null ? address.getAddressLine(0) : query;

                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                if (mMap != null && isAdded()) {
                                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(target, 15f));
                                    reportRepository.setLocation(target.latitude, target.longitude);

                                    if (searchLocationMarker != null) {
                                        searchLocationMarker.remove();
                                    }
                                    searchLocationMarker = mMap.addMarker(new MarkerOptions()
                                            .position(target)
                                            .title("📍 " + query)
                                            .snippet(displayName)
                                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                                    if (searchLocationMarker != null) {
                                        searchLocationMarker.showInfoWindow();
                                    }

                                    Toast.makeText(requireContext(), "Đã chuyển đến: " + displayName, Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    } else {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                if (isAdded()) {
                                    Toast.makeText(requireContext(), "Không tìm thấy địa điểm: " + query, Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                } catch (Exception e) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            if (isAdded()) {
                                Toast.makeText(requireContext(), "Lỗi tìm kiếm: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });
    }

    private void hideKeyboard(@Nullable View v) {
        if (v == null || !isAdded()) return;
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    public static final double CLUSTER_RADIUS_METERS = 500.0;

    @NonNull
    public static List<ReportCluster> clusterReports(@NonNull List<Report> reports, double radiusMeters) {
        List<ReportCluster> clusters = new ArrayList<>();
        if (reports.isEmpty()) return clusters;

        List<Report> sorted = new ArrayList<>(reports);
        Collections.sort(sorted, new Comparator<Report>() {
            @Override
            public int compare(Report o1, Report o2) {
                if (o2.getUpCount() != o1.getUpCount()) {
                    return Integer.compare(o2.getUpCount(), o1.getUpCount());
                }
                long t1 = o1.getCreatedAt() != null ? o1.getCreatedAt().toDate().getTime() : 0L;
                long t2 = o2.getCreatedAt() != null ? o2.getCreatedAt().toDate().getTime() : 0L;
                return Long.compare(t2, t1);
            }
        });

        for (Report report : sorted) {
            ReportCluster matched = null;
            for (ReportCluster cluster : clusters) {
                double dist = GeoUtils.getDistanceBetween(
                        report.getLatitude(), report.getLongitude(),
                        cluster.getCenterLat(), cluster.getCenterLng()
                );
                if (dist <= radiusMeters) {
                    matched = cluster;
                    break;
                }
            }
            if (matched != null) {
                matched.addReport(report);
            } else {
                clusters.add(new ReportCluster(report));
            }
        }
        return clusters;
    }

    private void renderReportMarkers(@NonNull List<Report> reports) {
        if (mMap == null) return;
        mMap.clear();

        long now = System.currentTimeMillis();
        int fadeMinutes = SettingsRepository.getInstance().getFadeMinutes();
        List<Report> validReports = new ArrayList<>();

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
            validReports.add(report);
        }

        // Gom nhóm trong phạm vi 500m
        List<ReportCluster> clusters = clusterReports(validReports, CLUSTER_RADIUS_METERS);
        int renderedCount = 0;

        for (ReportCluster cluster : clusters) {
            LatLng pos = new LatLng(cluster.getDisplayLat(), cluster.getDisplayLng());
            List<ReportCluster.CategoryStat> stats = cluster.getCategoryStats();
            if (stats.isEmpty()) continue;

            ReportCluster.CategoryStat top1 = stats.get(0);
            String title;
            String snippet;
            float alpha = 1.0f;

            if (cluster.getReportCount() == 1) {
                Report r = cluster.getReports().get(0);
                title = getCategoryIconAndName(r.getCategoryId());
                snippet = getRelativeTime(r.getCreatedAt());

                boolean isHighReputation = r.getUpCount() >= 3;
                if (isHighReputation) {
                    title = "⭐ " + title + " (" + r.getUpCount() + " xác nhận)";
                    snippet = "Cộng đồng xác thực cao • " + snippet;
                }

                if (r.getExpiresAt() != null) {
                    long remainingMs = r.getExpiresAt().toDate().getTime() - now;
                    if (remainingMs > 0 && remainingMs <= fadeMinutes * 60 * 1000L) {
                        alpha = 0.5f;
                        snippet += " (Sắp hết hạn)";
                    }
                }
            } else {
                // Có từ 2 báo cáo trở lên trong vùng 500m:
                // Hiện 2 loại hiện tượng được báo cáo nhiều nhất hoặc vote đúng nhiều nhất
                String name1 = getCategoryIconAndName(top1.getCategoryId());
                int count1 = top1.getTotalUpCount() > 0 ? top1.getTotalUpCount() : top1.getReportCount();

                if (stats.size() >= 2) {
                    ReportCluster.CategoryStat top2 = stats.get(1);
                    String name2 = getCategoryIconAndName(top2.getCategoryId());
                    int count2 = top2.getTotalUpCount() > 0 ? top2.getTotalUpCount() : top2.getReportCount();
                    title = name1 + " (" + count1 + ") · " + name2 + " (" + count2 + ")";
                } else {
                    title = name1 + " (" + cluster.getReportCount() + " báo cáo)";
                }

                if (cluster.isHighReputation()) {
                    title = "⭐ " + title;
                }
                snippet = "Vùng 500m: " + cluster.getReportCount() + " báo cáo • Chạm để xem chi tiết";
            }

            boolean isHighZ = cluster.isHighReputation() || cluster.getReportCount() > 1;

            // Icon tùy chỉnh: cụm hiện emoji của 2 hiện tượng đứng đầu bảng xếp hạng
            String emoji1 = getCategoryEmoji(top1.getCategoryId());
            String emoji2 = (cluster.getReportCount() > 1 && stats.size() >= 2)
                    ? getCategoryEmoji(stats.get(1).getCategoryId()) : null;
            int iconColor = cluster.hasRiskCategory()
                    ? 0xFFD32F2F : MarkerIconFactory.getCategoryColor(top1.getCategoryId());
            float density = getResources().getDisplayMetrics().density;

            MarkerOptions options = new MarkerOptions()
                    .position(pos)
                    .title(title)
                    .snippet(snippet)
                    .alpha(alpha)
                    .zIndex(isHighZ ? 2.0f : 1.0f)
                    .anchor(0.5f, 1.0f)
                    .icon(MarkerIconFactory.create(density, emoji1, emoji2,
                            cluster.getReportCount() > 1 ? cluster.getReportCount() : 0, iconColor));

            Marker marker = mMap.addMarker(options);
            if (marker != null) marker.setTag(cluster);
            renderedCount++;
        }

        if (isRiskFilterActive && renderedCount == 0 && isAdded() && getContext() != null) {
            Toast.makeText(getContext(), "Khu vực quanh bạn an toàn, không có rủi ro nào", Toast.LENGTH_SHORT).show();
        }
    }

    @NonNull
    public static String getCategoryEmoji(@Nullable String categoryId) {
        if (categoryId == null) return "🌦️";
        switch (categoryId) {
            case "cat_nang":
            case "Sunny":
                return "☀️";
            case "cat_mua":
            case "Rain":
                return "🌧️";
            case "cat_suong":
            case "Fog":
                return "🌫️";
            case "cat_bao":
            case "Storm":
                return "⛈️";
            case "cat_ngap":
            case "Flood":
                return "🌊";
            case "cat_cay":
            case "FallenTree":
                return "🪵";
            default:
                return "🌦️";
        }
    }

    @NonNull
    public static String getCategoryIconAndName(@Nullable String categoryId) {
        return getCategoryEmoji(categoryId) + " " + getCategoryDisplayName(categoryId);
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

                        if (isFirstLocationUpdate && mMap != null) {
                            isFirstLocationUpdate = false;
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(location.getLatitude(), location.getLongitude()), 15f));
                        }
                    }
                }
            });

            // 2. Lắng nghe cập nhật vị trí thời gian thực (tự động zoom ngay khi có tín hiệu GPS)
            if (locationCallback != null) {
                fusedLocationClient.removeLocationUpdates(locationCallback);
            }
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

                        if (isFirstLocationUpdate && mMap != null) {
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
        mMap = null;
        isFirstLocationUpdate = (restoredCameraPosition == null);
        etSearchLocation = null;
        btnClearSearch = null;
        if (searchLocationMarker != null) {
            searchLocationMarker.remove();
            searchLocationMarker = null;
        }
        if (reportRepository != null) {
            reportRepository.stopListening();
        }
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}
