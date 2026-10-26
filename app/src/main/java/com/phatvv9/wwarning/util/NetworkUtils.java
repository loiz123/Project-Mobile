package com.phatvv9.wwarning.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;

import androidx.annotation.NonNull;

public class NetworkUtils {

    public interface NetworkCallbackListener {
        void onNetworkChanged(boolean isConnected);
    }

    public static boolean isNetworkAvailable(Context context) {
        if (context == null) return false;
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        Network activeNetwork = cm.getActiveNetwork();
        if (activeNetwork == null) return false;

        NetworkCapabilities capabilities = cm.getNetworkCapabilities(activeNetwork);
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    public static ConnectivityManager.NetworkCallback registerNetworkCallback(@NonNull final Context context, @NonNull final NetworkCallbackListener listener) {
        final ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return null;

        ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                listener.onNetworkChanged(true);
            }

            @Override
            public void onLost(@NonNull Network network) {
                listener.onNetworkChanged(isNetworkAvailable(context));
            }

            @Override
            public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities capabilities) {
                boolean hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                listener.onNetworkChanged(hasInternet);
            }
        };

        try {
            cm.registerDefaultNetworkCallback(callback);
        } catch (Exception e) {
            NetworkRequest request = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();
            cm.registerNetworkCallback(request, callback);
        }
        return callback;
    }

    public static void unregisterNetworkCallback(@NonNull Context context, ConnectivityManager.NetworkCallback callback) {
        if (callback == null) return;
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            try {
                cm.unregisterNetworkCallback(callback);
            } catch (Exception e) {
                // Ignore
            }
        }
    }
}
