package com.phatvv9.wwarning.data.remote;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import com.phatvv9.wwarning.model.AppSetting;
import com.phatvv9.wwarning.model.Severity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SettingsRepository {

    private static final String TAG = "SettingsRepository";
    private static final String COLLECTION_SEVERITIES = "severities";
    private static final String COLLECTION_APP_SETTINGS = "app_settings";

    private final FirebaseFirestore db;
    private final MutableLiveData<List<Severity>> severitiesLiveData;
    private final MutableLiveData<Map<String, AppSetting>> appSettingsLiveData;

    public SettingsRepository() {
        this(FirebaseFirestore.getInstance());
    }

    public SettingsRepository(@NonNull FirebaseFirestore db) {
        this.db = db;
        this.severitiesLiveData = new MutableLiveData<>();
        this.appSettingsLiveData = new MutableLiveData<>();

        listenToSeverities();
        listenToAppSettings();
    }

    public LiveData<List<Severity>> getSeverities() {
        return severitiesLiveData;
    }

    public LiveData<Map<String, AppSetting>> getAppSettings() {
        return appSettingsLiveData;
    }

    private void listenToSeverities() {
        db.collection(COLLECTION_SEVERITIES)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            Log.e(TAG, "Error fetching severities", error);
                            return;
                        }

                        if (value != null) {
                            List<Severity> list = new ArrayList<>();
                            for (QueryDocumentSnapshot doc : value) {
                                Severity severity = doc.toObject(Severity.class);
                                severity.setId(doc.getId());
                                list.add(severity);
                            }
                            severitiesLiveData.setValue(list);
                        }
                    }
                });
    }

    private void listenToAppSettings() {
        db.collection(COLLECTION_APP_SETTINGS)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            Log.e(TAG, "Error fetching app_settings", error);
                            return;
                        }

                        if (value != null) {
                            Map<String, AppSetting> map = new HashMap<>();
                            for (QueryDocumentSnapshot doc : value) {
                                AppSetting setting = doc.toObject(AppSetting.class);
                                setting.setKey(doc.getId());
                                map.put(doc.getId(), setting);
                            }
                            appSettingsLiveData.setValue(map);
                        }
                    }
                });
    }
}
