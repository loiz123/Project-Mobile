package com.phatvv9.wwarning.ui.map;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.phatvv9.wwarning.R;
import com.phatvv9.wwarning.data.remote.ReportRepository;
import com.phatvv9.wwarning.ui.report.QuickReportDialog;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private Location currentLocation;
    private ReportRepository repository;

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

        repository = new ReportRepository();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        view.findViewById(R.id.fabQuickReport).setOnClickListener(v -> {
            if (currentLocation != null) {
                new QuickReportDialog(requireContext(), repository, currentLocation.getLatitude(), currentLocation.getLongitude()).show();
            }
        });

        repository.getReports().observe(getViewLifecycleOwner(), reports -> {
            if (mMap != null && reports != null) {
                mMap.clear();
                for (com.phatvv9.wwarning.model.Report report : reports) {
                    LatLng pos = new LatLng(report.getLatitude(), report.getLongitude());
                    mMap.addMarker(new MarkerOptions()
                            .position(pos)
                            .title(report.getCategoryId())
                            .snippet("Vừa xong")); // placeholder snippet
                }
            }
        });

        return view;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        checkLocationPermission();
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
            fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
                if (location != null) {
                    currentLocation = location;
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 15f));
                }
            });
        }
    }
}
