package com.phatvv9.wwarning.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

public final class AdminSessionManager {
    private static final String PREF_NAME = "wwarning_admin_prefs";
    private static final String KEY_IS_ADMIN = "is_admin_logged_in";

    private AdminSessionManager() {}

    public static boolean isAdmin(@Nullable Context context) {
        if (context == null) return false;
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_IS_ADMIN, false);
    }

    public static void setAdmin(@Nullable Context context, boolean isAdmin) {
        if (context == null) return;
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_IS_ADMIN, isAdmin).apply();
    }
}
