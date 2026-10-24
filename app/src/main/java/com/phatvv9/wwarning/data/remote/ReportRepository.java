package com.phatvv9.wwarning.data.remote;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.firebase.geofire.GeoQueryBounds;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.phatvv9.wwarning.data.local.ReportLocalCache;
import com.phatvv9.wwarning.model.Report;
import com.phatvv9.wwarning.util.FirestoreNames;
import com.phatvv9.wwarning.util.GeoUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class ReportRepository {

    private static final String TAG = "ReportRepository";
    private static final String COLLECTION_REPORTS = FirestoreNames.REPORTS;
    public static final double DEFAULT_RADIUS_KM = 10.0;
    private static final double MIN_MOVE_DISTANCE_REQUERY_METERS = 500.0;

    public interface ReportCallback {
        void onSuccess(@NonNull String reportId);
        void onError(@NonNull Exception e);
    }

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private final MutableLiveData<List<Report>> reportsLiveData;
    private ReportLocalCache localCache;

    private ListenerRegistration fallbackListenerRegistration;
    private final List<ListenerRegistration> boundaryListeners = new ArrayList<>();
    private final Map<Integer, Map<String, Report>> boundReportsMap = new HashMap<>();

    private Double lastQueryLat = null;
    private Double lastQueryLng = null;
    private double currentRadiusMeters = DEFAULT_RADIUS_KM * 1000.0;

    private static volatile ReportRepository INSTANCE;

    public static ReportRepository getInstance(@Nullable Context context) {
        if (INSTANCE == null) {
            synchronized (ReportRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ReportRepository(context != null ? context.getApplicationContext() : null);
                }
            }
        }
        return INSTANCE;
    }

    public ReportRepository() {
        this(FirebaseFirestore.getInstance(), FirebaseAuth.getInstance(), null);
    }

    public ReportRepository(@Nullable Context context) {
        this(FirebaseFirestore.getInstance(), FirebaseAuth.getInstance(), context);
    }

    public ReportRepository(@NonNull FirebaseFirestore db, @NonNull FirebaseAuth auth) {
        this(db, auth, null);
    }

    public ReportRepository(@NonNull FirebaseFirestore db, @NonNull FirebaseAuth auth, @Nullable Context context) {
        this.db = db;
        this.auth = auth;
        this.reportsLiveData = new MutableLiveData<>();
        if (context != null) {
            this.localCache = ReportLocalCache.getInstance(context);
            loadReportsFromRoomCache();
        }
        listenToAllActiveReports();
    }

    public void setContext(@NonNull Context context) {
        this.localCache = ReportLocalCache.getInstance(context);
    }

    @NonNull
    public LiveData<List<Report>> getReports() {
        return reportsLiveData;
    }

    /**
     * Lắng nghe tất cả báo cáo Active (dùng làm fallback khi chưa có toạ độ GPS).
     */
    private void listenToAllActiveReports() {
        stopListening();

        fallbackListenerRegistration = db.collection(COLLECTION_REPORTS)
                .whereEqualTo("status", FirestoreNames.ReportStatus.ACTIVE)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            Log.e(TAG, "Fallback listen failed, loading from Room cache.", error);
                            loadReportsFromRoomCache();
                            return;
                        }

                        List<Report> reports = new ArrayList<>();
                        long now = System.currentTimeMillis();
                        if (value != null) {
                            for (QueryDocumentSnapshot doc : value) {
                                Report report = doc.toObject(Report.class);
                                report.setId(doc.getId());

                                // Lọc bỏ báo cáo quá hạn
                                if (report.getExpiresAt() != null && report.getExpiresAt().toDate().getTime() > now) {
                                    reports.add(report);
                                }
                            }
                        }
                        sortReportsDesc(reports);
                        reportsLiveData.setValue(reports);

                        if (localCache != null && !reports.isEmpty()) {
                            localCache.saveReportsToRoom(reports);
                        }
                    }
                });
    }

    /**
     * Tuần 3: Truy vấn Geohash phạm vi bán kính 10km quanh toạ độ người dùng.
     */
    public void setLocation(final double lat, final double lng) {
        double radiusKm = SettingsRepository.getInstance().getSearchRadiusKm();
        setLocation(lat, lng, radiusKm);
    }

    public void setLocation(final double lat, final double lng, double radiusKm) {
        final double radiusMeters = radiusKm * 1000.0;
        this.currentRadiusMeters = radiusMeters;

        // Nếu đã có toạ độ và di chuyển dưới 500m thì không cần đăng ký lại listeners
        if (lastQueryLat != null && lastQueryLng != null) {
            double moved = GeoUtils.getDistanceBetween(lastQueryLat, lastQueryLng, lat, lng);
            if (moved < MIN_MOVE_DISTANCE_REQUERY_METERS) {
                return;
            }
        }

        lastQueryLat = lat;
        lastQueryLng = lng;

        // Dừng các listener cũ
        stopListening();

        List<GeoQueryBounds> bounds = GeoUtils.getGeoHashQueryBounds(lat, lng, radiusMeters);
        boundReportsMap.clear();

        for (int i = 0; i < bounds.size(); i++) {
            final int boundIndex = i;
            final GeoQueryBounds bound = bounds.get(i);

            ListenerRegistration registration = db.collection(COLLECTION_REPORTS)
                    .orderBy("geohash")
                    .startAt(bound.startHash)
                    .endAt(bound.endHash)
                    .addSnapshotListener(new EventListener<QuerySnapshot>() {
                        @Override
                        public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                            if (error != null) {
                                Log.e(TAG, "Geohash bound listener error at index " + boundIndex, error);
                                loadReportsFromRoomCache();
                                return;
                            }

                            Map<String, Report> subMap = new HashMap<>();
                            long now = System.currentTimeMillis();

                            if (value != null) {
                                for (QueryDocumentSnapshot doc : value) {
                                    Report report = doc.toObject(Report.class);
                                    report.setId(doc.getId());

                                    // Lọc nghiệp vụ: Trạng thái Active, Chưa hết hạn và Trong bán kính thực tế
                                    boolean isActive = FirestoreNames.ReportStatus.ACTIVE.equals(report.getStatus());
                                    boolean notExpired = report.getExpiresAt() != null && report.getExpiresAt().toDate().getTime() > now;
                                    double distance = GeoUtils.getDistanceBetween(report.getLatitude(), report.getLongitude(), lat, lng);

                                    if (isActive && notExpired && distance <= currentRadiusMeters) {
                                        subMap.put(report.getId(), report);
                                    }
                                }
                            }

                            boundReportsMap.put(boundIndex, subMap);
                            aggregateAndPostReports();
                        }
                    });

            boundaryListeners.add(registration);
        }
    }

    private void aggregateAndPostReports() {
        Map<String, Report> allReports = new HashMap<>();
        for (Map<String, Report> sub : boundReportsMap.values()) {
            allReports.putAll(sub);
        }

        List<Report> result = new ArrayList<>(allReports.values());
        if (result.isEmpty() && localCache != null) {
            loadReportsFromRoomCache();
            return;
        }
        sortReportsDesc(result);
        reportsLiveData.setValue(result);

        if (localCache != null && !result.isEmpty()) {
            localCache.saveReportsToRoom(result);
        }
    }

    private void loadReportsFromRoomCache() {
        if (localCache != null) {
            Executors.newSingleThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    List<Report> cached = localCache.getCachedReportsSync();
                    if (cached != null && !cached.isEmpty()) {
                        reportsLiveData.postValue(cached);
                    }
                }
            });
        }
    }

    private void sortReportsDesc(List<Report> reports) {
        Collections.sort(reports, new Comparator<Report>() {
            @Override
            public int compare(Report o1, Report o2) {
                if (o1.getCreatedAt() == null || o2.getCreatedAt() == null) return 0;
                return o2.getCreatedAt().compareTo(o1.getCreatedAt());
            }
        });
    }

    public void stopListening() {
        if (fallbackListenerRegistration != null) {
            fallbackListenerRegistration.remove();
            fallbackListenerRegistration = null;
        }
        for (ListenerRegistration reg : boundaryListeners) {
            if (reg != null) {
                reg.remove();
            }
        }
        boundaryListeners.clear();
    }

    public void createReport(@NonNull String categoryId, double lat, double lng) {
        createReport(categoryId, lat, lng, null, null);
    }

    public void createReport(@NonNull String categoryId, double lat, double lng, @Nullable final ReportCallback callback) {
        createReport(categoryId, lat, lng, null, callback);
    }

    public void createReport(@NonNull String categoryId, double lat, double lng, @Nullable String severityId, @Nullable final ReportCallback callback) {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "anonymous";
        String geohash = GeoUtils.getGeoHash(lat, lng);

        int expireMinutes = SettingsRepository.getInstance().getExpireMinutes();
        long nowMillis = System.currentTimeMillis();
        Timestamp expiresAt = new Timestamp(new Date(nowMillis + expireMinutes * 60 * 1000L));

        Report report = new Report(
                userId,
                categoryId,
                severityId,
                lat,
                lng,
                geohash,
                expiresAt
        );

        final Report finalReport = report;
        db.collection(COLLECTION_REPORTS)
                .add(report)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        finalReport.setId(documentReference.getId());
                        if (localCache != null) {
                            localCache.saveReportsToRoom(Collections.singletonList(finalReport));
                        }
                        List<Report> current = reportsLiveData.getValue();
                        List<Report> updated = current != null ? new ArrayList<>(current) : new ArrayList<Report>();
                        boolean exists = false;
                        for (Report r : updated) {
                            if (documentReference.getId().equals(r.getId())) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            updated.add(0, finalReport);
                            reportsLiveData.setValue(updated);
                        }

                        Log.d(TAG, "Report created with ID: " + documentReference.getId());
                        if (callback != null) {
                            callback.onSuccess(documentReference.getId());
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error creating report", e);
                        if (callback != null) {
                            callback.onError(e);
                        }
                    }
                });
    }

    public void removeReportLocally(@NonNull String reportId) {
        if (localCache != null) {
            localCache.updateReportStatus(reportId, FirestoreNames.ReportStatus.DELETED);
        }
        for (Map<String, Report> subMap : boundReportsMap.values()) {
            subMap.remove(reportId);
        }
        List<Report> current = reportsLiveData.getValue();
        if (current != null) {
            List<Report> updated = new ArrayList<>();
            for (Report r : current) {
                if (!reportId.equals(r.getId())) {
                    updated.add(r);
                }
            }
            reportsLiveData.setValue(updated);
        }
    }

    public void deleteReport(@NonNull final String reportId, @Nullable final ReportCallback callback) {
        removeReportLocally(reportId);
        db.collection(COLLECTION_REPORTS).document(reportId)
                .update("status", FirestoreNames.ReportStatus.DELETED)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        if (callback != null) {
                            callback.onSuccess(reportId);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Server Firestore từ chối hoặc lỗi khi xóa, đã áp dụng xóa cục bộ", e);
                        if (callback != null) {
                            callback.onSuccess(reportId);
                        }
                    }
                });
    }
}
