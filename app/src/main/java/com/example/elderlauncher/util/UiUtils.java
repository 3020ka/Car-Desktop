package com.example.elderlauncher.util;

import android.content.Context;
import android.util.TypedValue;

public final class UiUtils {
    private UiUtils() {
    }

    public static int dp(Context context, int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                context.getResources().getDisplayMetrics());
    }
}
