package com.example.elderlauncher.service;

import android.accessibilityservice.AccessibilityService;
import android.os.Build;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;

import java.util.Locale;

public class QuickShieldAccessibilityService extends AccessibilityService {
    private static volatile boolean temporarilyPaused;

    public static void setTemporarilyPaused(boolean paused) {
        temporarilyPaused = paused;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (temporarilyPaused) {
            return;
        }
        if (event == null || event.getPackageName() == null) {
            return;
        }

        String pkg = event.getPackageName().toString();
        if (!"com.android.systemui".equals(pkg)) {
            return;
        }

        String cls = event.getClassName() == null ? "" : event.getClassName().toString();
        if (!isLikelyNotificationShadeClass(cls)) {
            return;
        }

        // Best-effort shield for notification shade: dismiss and jump home.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE);
        }
        performGlobalAction(GLOBAL_ACTION_HOME);
    }

    @Override
    public void onInterrupt() {
        // No-op
    }

    private boolean isLikelyNotificationShadeClass(String className) {
        if (TextUtils.isEmpty(className)) {
            return false;
        }
        String lower = className.toLowerCase(Locale.US);
        return lower.contains("notificationshade")
                || lower.contains("notificationpanel")
                || lower.contains("panelview")
                || lower.contains("statusbarwindow");
    }
}
