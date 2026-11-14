package com.rena.w4b;

import java.util.Locale;

public final class DownloadFormat {
    private DownloadFormat() {
    }

    public static String size(long value) {
        if (value < 0) {
            return "0 B";
        }
        if (value < 1024L) {
            return value + " B";
        }
        double kb = value / 1024.0;
        if (kb < 1024.0) {
            return String.format(Locale.US, "%.0f KB", kb);
        }
        double mb = kb / 1024.0;
        if (mb < 1024.0) {
            return String.format(Locale.US, "%.1f MB", mb);
        }
        return String.format(Locale.US, "%.2f GB", mb / 1024.0);
    }

    public static String duration(long seconds) {
        if (seconds < 0) {
            return "";
        }
        if (seconds < 60) {
            return seconds + "s";
        }
        if (seconds < 3600) {
            return (seconds / 60) + "m " + (seconds % 60) + "s";
        }
        return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
    }
}
