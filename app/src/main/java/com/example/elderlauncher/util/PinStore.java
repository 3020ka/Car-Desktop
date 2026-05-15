package com.example.elderlauncher.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

public class PinStore {
    private static final String PREFS = "elder_launcher_prefs";
    private static final String KEY_PIN = "pin_code";

    private final SharedPreferences prefs;

    public PinStore(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public boolean hasPin() {
        String value = prefs.getString(KEY_PIN, "");
        return !TextUtils.isEmpty(value) && value.length() == 4;
    }

    public boolean isValidPinFormat(String pin) {
        return pin != null && pin.matches("\\d{4}");
    }

    public void savePin(String pin) {
        prefs.edit().putString(KEY_PIN, pin).apply();
    }

    public boolean verifyPin(String pin) {
        if (!isValidPinFormat(pin)) {
            return false;
        }
        String saved = prefs.getString(KEY_PIN, "");
        return pin.equals(saved);
    }
}
