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
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.phatvv9.wwarning.R;
import com.phatvv9.wwarning.data.remote.ReportRepository;
import com.phatvv9.wwarning.model.Report;
import com.phatvv9.wwarning.ui.report.QuickReportDialog;

import java.util.List;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private Location currentLocation;
    private LocationCallback locationCallback;
    private ReportRepository reportRepository;
    private boolean isFirstLocationUpdate = true;
    private List<Report> cachedReports;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    enableMyLocation();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        reportRepository = new ReportRepository();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        view.findViewById(R.id.fabQuickReport).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentLocation != null) {
                    new QuickReportDialog(requireContext(), reportRepository,
                            currentLocation.getLatitude(), currentLocation.getLongitude()).show();
                } else {
                    Toast.makeText(requireContext(), "Đang xác định vị trí, vui lòng đợi giây lát...", Toast.LENGTH_SHORT).show();
                }
            }
        });

        reportRepository.getReports().observe(getViewLifecycleOwner(), new Observer<List<Report>>() {
            @Override
            public void onChanged(List<Report> reports) {
                cachedReports = reports;
                if (mMap != null && reports != null) {
                    renderReportMarkers(reports);
                }
            }
        });

        return view;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        checkLocationPermission();
        if (cachedReports != null) {
            renderReportMarkers(cachedReports);
        }
    }

    private void renderReportMarkers(@NonNull List<Report> reports) {
        if (mMap == null) return;
        mMap.clear();
        for (Report report : reports) {
            LatLng pos = new LatLng(report.getLatitude(), report.getLongitude());
            String title = getCategoryDisplayName(report.getCategoryId());
            String snippet = getRelativeTime(report.getCreatedAt());

            MarkerOptions options = new MarkerOptions()
                    .position(pos)
                    .title(title)
                    .snippet(snippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(getCategoryHue(report.getCategoryId())));

            mMap.addMarker(options);
        }
    }

    private String getCategoryDisplayName(@Nullable String categoryId) {
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
                    if (location != null && isFirstLocationUpdate) {
                        isFirstLocationUpdate = false;
                        currentLocation = location;
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                new LatLng(location.getLatitude(), location.getLongitude()), 15f));
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
