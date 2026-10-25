package com.phatvv9.wwarning.data.sync;

import android.content.Context;
import android.net.ConnectivityManager;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.phatvv9.wwarning.util.NetworkUtils;

public class SyncManager {

    private static volatile SyncManager INSTANCE;

    private final Context context;
    private final MutableLiveData<Boolean> isOnlineLiveData;
    private ConnectivityManager.NetworkCallback networkCallback;

    public static SyncManager getInstance(@NonNull Context context) {
        if (INSTANCE == null) {
            synchronized (SyncManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SyncManager(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    private SyncManager(@NonNull Context context) {
        this.context = context;
        this.isOnlineLiveData = new MutableLiveData<>(NetworkUtils.isNetworkAvailable(context));
        startMonitoring();
    }

    private void startMonitoring() {
        if (networkCallback != null) return;
        networkCallback = NetworkUtils.registerNetworkCallback(context, new NetworkUtils.NetworkCallbackListener() {
            @Override
            public void onNetworkChanged(boolean isConnected) {
                isOnlineLiveData.postValue(isConnected);
            }
        });
    }

    public LiveData<Boolean> getIsOnlineLiveData() {
        boolean current = NetworkUtils.isNetworkAvailable(context);
        if (isOnlineLiveData.getValue() == null || isOnlineLiveData.getValue() != current) {
            isOnlineLiveData.postValue(current);
        }
        return isOnlineLiveData;
    }

    public boolean isOnline() {
        Boolean value = isOnlineLiveData.getValue();
        return value != null ? value : NetworkUtils.isNetworkAvailable(context);
    }
}
