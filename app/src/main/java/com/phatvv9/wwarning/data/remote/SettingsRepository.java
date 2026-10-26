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

    public static final int DEFAULT_EXPIRE_MINUTES = 60;
    public static final int DEFAULT_DOWNVOTE_THRESHOLD = 5;
    public static final int DEFAULT_FADE_MINUTES = 15;
    public static final double DEFAULT_SEARCH_RADIUS_KM = 10.0;

    private static volatile SettingsRepository INSTANCE;

    private final FirebaseFirestore db;
    private final MutableLiveData<List<Severity>> severitiesLiveData;
    private final MutableLiveData<Map<String, AppSetting>> appSettingsLiveData;

    private int cachedExpireMinutes = DEFAULT_EXPIRE_MINUTES;
    private int cachedDownvoteThreshold = DEFAULT_DOWNVOTE_THRESHOLD;
    private int cachedFadeMinutes = DEFAULT_FADE_MINUTES;
    private double cachedSearchRadiusKm = DEFAULT_SEARCH_RADIUS_KM;

    public static SettingsRepository getInstance() {
        if (INSTANCE == null) {
            synchronized (SettingsRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SettingsRepository();
                }
            }
        }
        return INSTANCE;
    }

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

    public int getExpireMinutes() {
        return cachedExpireMinutes;
    }

    public int getDownvoteThreshold() {
        return cachedDownvoteThreshold;
    }

    public int getFadeMinutes() {
        return cachedFadeMinutes;
    }

    public double getSearchRadiusKm() {
        return cachedSearchRadiusKm;
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

                                try {
                                    if ("expire_minutes".equalsIgnoreCase(doc.getId()) && setting.getValue() != null) {
                                        cachedExpireMinutes = Integer.parseInt(setting.getValue());
                                    } else if ("downvote_threshold".equalsIgnoreCase(doc.getId()) && setting.getValue() != null) {
                                        cachedDownvoteThreshold = Integer.parseInt(setting.getValue());
                                    } else if ("fade_minutes".equalsIgnoreCase(doc.getId()) && setting.getValue() != null) {
                                        cachedFadeMinutes = Integer.parseInt(setting.getValue());
                                    } else if ("radius_km".equalsIgnoreCase(doc.getId()) && setting.getValue() != null) {
                                        cachedSearchRadiusKm = Double.parseDouble(setting.getValue());
                                    }
                                } catch (NumberFormatException e) {
                                    Log.e(TAG, "Error parsing app_setting value for key: " + doc.getId(), e);
                                }
                            }
                            appSettingsLiveData.setValue(map);
                        }
                    }
                });
    }
}
