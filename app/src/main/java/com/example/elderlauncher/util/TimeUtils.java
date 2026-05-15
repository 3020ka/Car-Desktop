package com.example.elderlauncher.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class TimeUtils {
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm", Locale.CHINA);
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy年MM月dd日 EEEE", Locale.CHINA);

    private TimeUtils() {
    }

    public static String formatTime(long millis) {
        return TIME_FORMAT.format(new Date(millis));
    }

    public static String formatDate(long millis) {
        return DATE_FORMAT.format(new Date(millis));
    }
}
