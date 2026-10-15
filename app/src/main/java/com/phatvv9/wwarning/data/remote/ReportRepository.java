package com.phatvv9.wwarning.data.remote;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

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
import com.phatvv9.wwarning.model.Report;
import com.phatvv9.wwarning.util.GeoUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ReportRepository {

    private static final String TAG = "ReportRepository";
    private static final String COLLECTION_REPORTS = "reports";
    private static final int DEFAULT_EXPIRE_MINUTES = 60;

    public interface ReportCallback {
        void onSuccess(@NonNull String reportId);
        void onError(@NonNull Exception e);
    }

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private final MutableLiveData<List<Report>> reportsLiveData;
    private ListenerRegistration reportsListenerRegistration;

    public ReportRepository() {
        this(FirebaseFirestore.getInstance(), FirebaseAuth.getInstance());
    }

    public ReportRepository(@NonNull FirebaseFirestore db, @NonNull FirebaseAuth auth) {
        this.db = db;
        this.auth = auth;
        this.reportsLiveData = new MutableLiveData<>();
        listenToReports();
    }

    @NonNull
    public LiveData<List<Report>> getReports() {
        return reportsLiveData;
    }

    public void listenToReports() {
        if (reportsListenerRegistration != null) {
            reportsListenerRegistration.remove();
        }

        reportsListenerRegistration = db.collection(COLLECTION_REPORTS)
                .whereEqualTo("status", "Active")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            Log.e(TAG, "Listen failed.", error);
                            return;
                        }

                        List<Report> reports = new ArrayList<>();
                        if (value != null) {
                            for (QueryDocumentSnapshot doc : value) {
                                Report report = doc.toObject(Report.class);
                                report.setId(doc.getId());
                                reports.add(report);
                            }
                        }
                        reportsLiveData.setValue(reports);
                    }
                });
    }

    public void stopListening() {
        if (reportsListenerRegistration != null) {
            reportsListenerRegistration.remove();
            reportsListenerRegistration = null;
        }
    }

    public void createReport(@NonNull String categoryId, double lat, double lng) {
        createReport(categoryId, lat, lng, null);
    }

    public void createReport(@NonNull String categoryId, double lat, double lng, @Nullable final ReportCallback callback) {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "anonymous";
        String geohash = GeoUtils.getGeoHash(lat, lng);

        long nowMillis = System.currentTimeMillis();
        Timestamp expiresAt = new Timestamp(new Date(nowMillis + DEFAULT_EXPIRE_MINUTES * 60 * 1000L));

        Report report = new Report(
                userId,
                categoryId,
                null,
                lat,
                lng,
                geohash,
                expiresAt
        );

        db.collection(COLLECTION_REPORTS)
                .add(report)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
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
}
