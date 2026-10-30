package com.workbuddy.jxc;

import android.content.Context;
import android.content.SharedPreferences;

public class AppSettings {
    private static final String PREFS_NAME = "app_settings";
    private static final String KEY_DEEPSEEK = "deepseek_api_key";
    private final SharedPreferences preferences;

    public AppSettings(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public String getDeepSeekApiKey() {
        return preferences.getString(KEY_DEEPSEEK, "").trim();
    }

    public boolean hasDeepSeekApiKey() {
        return !getDeepSeekApiKey().isEmpty();
    }

    public void setDeepSeekApiKey(String key) {
        preferences.edit().putString(KEY_DEEPSEEK, key == null ? "" : key.trim()).apply();
    }
}
